/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.attls;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.apache.catalina.connector.RequestFacade;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.AbstractServerHttpRequest;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.SslInfo;
import org.springframework.stereotype.Component;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.i18n.LocaleContextResolver;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.commons.attls.AttlsContext;
import org.zowe.commons.attls.ContextIsNotInitializedException;
import org.zowe.commons.attls.InboundAttls;
import org.zowe.commons.attls.IoctlCallException;
import org.zowe.commons.attls.StatConn;
import org.zowe.commons.attls.UnknownEnumValueException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "server.attls.enabled", havingValue = "true")
public class AttlsHttpHandler implements BeanPostProcessor {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final MessageService messageService;
    private final LocaleContextResolver localeContextResolver;

    private final WebSessionManager sessionManager = new DefaultWebSessionManager();
    private final ServerCodecConfigurer serverCodecConfigurer = ServerCodecConfigurer.create();

    private Mono<Void> writeError(ServerHttpRequest request, ServerHttpResponse response, String message) {
        DefaultServerWebExchange serverWebExchange = new DefaultServerWebExchange(request, response, sessionManager, serverCodecConfigurer, localeContextResolver);
        response.setRawStatusCode(500);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE);
        DataBuffer buffer = serverWebExchange.getResponse().bufferFactory().wrap(message.getBytes(StandardCharsets.UTF_8));
        return serverWebExchange.getResponse().writeWith(Flux.just(buffer));
    }

    private String getMessage(String key) {
        Message message = messageService.createMessage(key);
        try {
            return objectMapper.writeValueAsString(message.mapToView());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    Mono<Void> internalError(ServerHttpRequest request, ServerHttpResponse response) {
        return writeError(request, response, getMessage("org.zowe.apiml.gateway.internalServerError"));
    }

    Mono<Void> unsecureError(ServerHttpRequest request, ServerHttpResponse response) {
        return writeError(request, response, getMessage("org.zowe.apiml.gateway.security.attls.notSecure"));
    }

    ServerHttpRequest updateCertificate(ServerHttpRequest request, HttpServletRequest nativeRequest, byte[] rawCertificate) throws CertificateException {
        if (ArrayUtils.isEmpty(rawCertificate)) {
            return request;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("-----BEGIN CERTIFICATE-----\n");
        sb.append(new String(Base64.getEncoder().encode(rawCertificate), StandardCharsets.UTF_8));
        sb.append("\n-----END CERTIFICATE-----");
        X509Certificate certificate = (X509Certificate) CertificateFactory
            .getInstance("X509")
            .generateCertificate(new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8)));
        X509Certificate[] certificates = new X509Certificate[1];
        certificates[0] = certificate;
        nativeRequest.setAttribute("javax.servlet.request.X509Certificate", certificates);

        AttlsSslInfo sslInfo = AttlsSslInfo.builder().peerCertificates(certificates).build();
        return request.mutate().sslInfo(sslInfo).build();
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof HttpHandler) {
            HttpHandler httpHandler = (HttpHandler) bean;
            return (HttpHandler) (request, response) -> {
                try {
                    AttlsContext attlsContext = InboundAttls.get();
                    if (attlsContext.getStatConn() != StatConn.SECURE) {
                        return unsecureError(request, response);
                    }

                    RequestFacade requestFacade = ((AbstractServerHttpRequest) request).getNativeRequest();
                    requestFacade.setAttribute("attls", attlsContext);
                    request = updateCertificate(request, requestFacade, attlsContext.getCertificate());
                } catch (IoctlCallException | UnknownEnumValueException | ContextIsNotInitializedException |
                         CertificateException e) {
                    return internalError(request, response);
                }

                return httpHandler.handle(request, response);
            };
        }
        return bean;
    }

    @Builder
    @Value
    static class AttlsSslInfo implements SslInfo {

        private String sessionId;
        private X509Certificate[] peerCertificates;

    }

}
