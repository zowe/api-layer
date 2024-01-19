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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.product.routing.RoutedService;
import org.zowe.apiml.product.routing.RoutedServices;
import org.zowe.apiml.product.routing.RoutedServicesUser;
import org.zowe.apiml.util.UrlUtils;
import reactor.core.publisher.Flux;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
@Controller
@Component("ServerSentEventProxyHandler")
public class ServerSentEventProxyHandler implements RoutedServicesUser {
    private final DiscoveryClient discovery;
    private final MessageService messageService;
    private final Map<String, RoutedServices> routedServicesMap = new ConcurrentHashMap<>();

    @Autowired
    public ServerSentEventProxyHandler(DiscoveryClient discovery, MessageService messageService) {
        this.discovery = discovery;
        this.messageService = messageService;
    }

    @GetMapping({"/sse/**","/*/sse/**"})
    public SseEmitter getEmitter(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SseEmitter emitter = new SseEmitter(-1L);

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

    // package protected for unit testing
    Consumer<ServerSentEvent<String>> consumer(SseEmitter emitter) {
        return content -> {
            try {
                emitter.send(content.data());
            } catch (IOException error) {
                emitter.completeWithError(error);
            }
        };
    }

    // package protected for unit testing
    Flux<ServerSentEvent<String>> getSseStream(String sseStreamUrl) {
        WebClient client = WebClient.create(sseStreamUrl);
        ParameterizedTypeReference<ServerSentEvent<String>> type
            = new ParameterizedTypeReference<ServerSentEvent<String>>() {
        };
        return client.get()
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
}
