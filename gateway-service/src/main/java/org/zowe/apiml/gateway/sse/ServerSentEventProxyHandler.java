/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.sse;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.product.routing.RoutedService;
import org.zowe.apiml.product.routing.RoutedServices;
import org.zowe.apiml.product.routing.RoutedServicesUser;
import org.zowe.apiml.security.SecurityUtils;
import org.zowe.apiml.util.UrlUtils;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.SslProvider;

import javax.annotation.PostConstruct;
import javax.net.ssl.TrustManagerFactory;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.zowe.apiml.security.SecurityUtils.loadKeyStore;

@Slf4j
@Controller
@RequiredArgsConstructor
@Component("ServerSentEventProxyHandler")
public class ServerSentEventProxyHandler implements RoutedServicesUser {

    @Value("${server.ssl.trustStore:#{null}}")
    private String trustStore;

    @Value("${server.ssl.trustStorePassword:#{null}}")
    private char[] trustStorePassword;

    @Value("${server.ssl.trustStoreType:PKCS12}")
    private String trustStoreType;

    private final DiscoveryClient discovery;
    private final MessageService messageService;
    private final Map<String, RoutedServices> routedServicesMap = new ConcurrentHashMap<>();
    private WebClient webClient;

    @PostConstruct
    void initWebClient() throws CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException {
        updateStorePaths();
        webClient = WebClient.builder().clientConnector(new ReactorClientHttpConnector(
            HttpClient.create().secure(SslProvider.builder().sslContext(getSslContext()).build())
        )).build();
    }

    void updateStorePaths() {
        if (SecurityUtils.isKeyring(trustStore)) {
            trustStore = SecurityUtils.formatKeyringUrl(trustStore);
            if (trustStorePassword == null || trustStorePassword.length == 0) {
                trustStorePassword = "password".toCharArray();
            }
        }
    }

    private SslContext getSslContext() throws CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException {
        SslContextBuilder sslContextBuilder = SslContextBuilder.forClient().clientAuth(ClientAuth.NONE);
        if (trustStore != null) {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(loadKeyStore(trustStoreType, trustStore, trustStorePassword));
            sslContextBuilder.trustManager(tmf);
        }
        return sslContextBuilder.build();
    }

    @GetMapping({"/sse/**","/*/sse/**"})
    public SseEmitter getEmitter(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SseEmitter emitter = new SseEmitter(-1L) {
            @Override
            public void send(Object object, MediaType mediaType) throws IOException {
                super.send(new SseEventBuilderFixedImpl().data(object, mediaType));
            }
        };

        String uri = request.getRequestURI();
        List<String> uriParts = getUriParts(uri);
        if (uriParts.size() < 4) {
            writeError(response, SseErrorMessages.INVALID_ROUTE, uri);
            return null;
        }

        String serviceId = getServiceId(uriParts);
        String majorVersion = getMajorVersion(uriParts);
        String path = uriParts.size() < 5 ? "" : uriParts.get(4);

        ServiceInstance serviceInstance = findServiceInstance(serviceId);
        if (serviceInstance == null) {
            writeError(response, SseErrorMessages.INSTANCE_NOT_FOUND, serviceId);
            return null;
        }

        RoutedServices routedServices = routedServicesMap.get(serviceId);
        if (routedServices == null) {
            writeError(response, SseErrorMessages.INSTANCE_NOT_FOUND, serviceId);
            return null;
        }

        String sseRoute = "sse/" + majorVersion;
        RoutedService routedService = routedServices.findServiceByGatewayUrl(sseRoute);
        if (routedService == null) {
            writeError(response, SseErrorMessages.ENDPOINT_NOT_FOUND, sseRoute);
            return null;
        }

        String targetUrl = getTargetUrl(serviceInstance, routedService.getServiceUrl(), path, request.getQueryString());
        getSseStream(targetUrl).subscribe(consumer(emitter), emitter::completeWithError, emitter::complete);

        return emitter;
    }

    boolean hasEnter(String in) {
        return StringUtils.containsAny(in, '\n', '\r');
    }

    boolean hasEnter(ServerSentEvent<String> event) {
        return
            hasEnter(event.data()) ||
            hasEnter(event.event()) ||
            hasEnter(event.comment()) ||
            hasEnter(event.id());
    }

    ServerSentEvent<String> sanitize(ServerSentEvent<String> event) {
        if (!hasEnter(event)) {
            return event;
        }

        Assert.isTrue(!hasEnter(event.event()), "Illegal character in event content");
        Assert.isTrue(!hasEnter(event.id()), "Illegal character in event content");

        String data = event.data();
        if (hasEnter(data)) {
            data = data.replaceAll("\r\n", "\n");
            data = data.replaceAll("\n", "\ndata:");
        }

        String comment = event.comment();
        if (hasEnter(comment)) {
            comment = comment.replaceAll("\n", "\n:");
        }

        return ServerSentEvent.<String>builder()
            .comment(comment)
            .event(event.event())
            .id(event.id())
            .data(data)
            .retry(event.retry())
            .build();
    }

    // package protected for unit testing
    Consumer<ServerSentEvent<String>> consumer(SseEmitter emitter) {
        return content -> {
            try {
                emitter.send(sanitize(content).data());
            } catch (IOException error) {
                emitter.completeWithError(error);
            }
        };
    }

    // package protected for unit testing
    Flux<ServerSentEvent<String>> getSseStream(String sseStreamUrl) {
        ParameterizedTypeReference<ServerSentEvent<String>> type
            = new ParameterizedTypeReference<ServerSentEvent<String>>() {
        };
        return webClient
            .get()
            .uri(sseStreamUrl)
            .retrieve()
            .bodyToFlux(type);
    }

    private List<String> getUriParts(String uri) {
        if (uri == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(Arrays.asList(uri.split("/", 5)));
    }

    private String getServiceId(List<String> uriParts) {
        return uriParts.get(1);
    }

    private String getMajorVersion(List<String> uriParts) {
        return uriParts.get(3);
    }

    private ServiceInstance findServiceInstance(String serviceId) {
        List<ServiceInstance> serviceInstances = this.discovery.getInstances(serviceId);
        return serviceInstances.isEmpty() ? null : serviceInstances.get(0);
    }

    private String getTargetUrl(ServiceInstance serviceInstance, String serviceUrl, String path, String queryParameterString) {
        String parameters = queryParameterString == null ? "" : "?" + queryParameterString;
        String protocol = serviceInstance.isSecure() ? "https" : "http";
        return String.format("%s://%s:%d/%s/%s%s",
            protocol,
            serviceInstance.getHost(),
            serviceInstance.getPort(),
            UrlUtils.removeFirstAndLastSlash(serviceUrl),
            path,
            parameters
        );
    }

    private void writeError(HttpServletResponse response, SseErrorMessages errorMessage, String messageParameter) throws IOException {
        Message message = messageService.createMessage(errorMessage.getKey(), messageParameter);

        response.getWriter().print(message.mapToReadableText());
        response.setStatus(errorMessage.getStatus().value());
    }

    @Override
    public void addRoutedServices(String serviceId, RoutedServices routedServices) {
        routedServicesMap.put(serviceId, routedServices);
    }

    static class SseEventBuilderFixedImpl implements SseEmitter.SseEventBuilder {

        private final Set<ResponseBodyEmitter.DataWithMediaType> dataToSend = new LinkedHashSet<>(4);

        private final StringBuilder sb = new StringBuilder();


        private boolean hasName;

        @Override
        public SseEventBuilderFixedImpl id(String id) {
            checkEvent(id);
            append("id:").append(id).append('\n');
            return this;
        }

        @Override
        public SseEventBuilderFixedImpl name(String name) {
            checkEvent(name);
            this.hasName = true;
            append("event:").append(name).append('\n');
            return this;
        }

        @Override
        public SseEventBuilderFixedImpl reconnectTime(long reconnectTimeMillis) {
            append("retry:").append(String.valueOf(reconnectTimeMillis)).append('\n');
            return this;
        }

        @Override
        public SseEventBuilderFixedImpl comment(String comment) {
            append(':').append(StringUtils.replace(comment, "\n", "\n:")).append('\n');
            return this;
        }

        @Override
        public SseEventBuilderFixedImpl data(Object object) {
            return data(object, null);
        }

        @Override
        public SseEventBuilderFixedImpl data(Object object, @Nullable MediaType mediaType) {
            if (object instanceof ModelAndView && !this.hasName && ((ModelAndView) object).getViewName() != null) {
                name(((ModelAndView) object).getViewName());
            }
            append("data:");
            saveAppendedText(TEXT_PLAIN);
            if (object instanceof String) {
                writeStringData((String) object, mediaType);
            }
            else {
                this.dataToSend.add(new ResponseBodyEmitter.DataWithMediaType(object, mediaType));
            }

            append('\n');
            return this;
        }

        private static void checkEvent(String content) {
            Assert.isTrue(content.indexOf('\n') == -1 && content.indexOf('\r') == -1,
                "illegal character '\\n' or '\\r' in event content");
        }

        private void writeStringData(String input, @Nullable MediaType mediaType) {
            if (input.indexOf('\n') == -1 && input.indexOf('\r') == -1) {
                this.dataToSend.add(new ResponseBodyEmitter.DataWithMediaType(input, mediaType));
            }
            else {
                int length = input.length();
                for (int i = 0; i < length; i++) {
                    char c = input.charAt(i);
                    if (c == '\r') {
                        if (i + 1 < length && input.charAt(i + 1) == '\n') {
                            i++;
                        }
                        this.sb.append("\ndata:");
                    }
                    else if (c == '\n') {
                        this.sb.append("\ndata:");
                    }
                    else {
                        this.sb.append(c);
                    }
                }
                saveAppendedText(mediaType);
            }
        }

        SseEventBuilderFixedImpl append(String text) {
            this.sb.append(text);
            return this;
        }

        SseEventBuilderFixedImpl append(char ch) {
            this.sb.append(ch);
            return this;
        }

        @Override
        public Set<ResponseBodyEmitter.DataWithMediaType> build() {
            if (!org.springframework.util.StringUtils.hasLength(this.sb) && this.dataToSend.isEmpty()) {
                return Collections.emptySet();
            }
            append('\n');
            saveAppendedText(TEXT_PLAIN);
            return this.dataToSend;
        }

        private void saveAppendedText(@Nullable MediaType mediaType) {
            if (org.springframework.util.StringUtils.hasLength(this.sb)) {
                this.dataToSend.add(new ResponseBodyEmitter.DataWithMediaType(this.sb.toString(), mediaType));
                this.sb.setLength(0);
            }
        }
    }

}
