/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.zowe.apiml.gateway.x509.X509Util;
import reactor.core.publisher.Mono;

import java.security.cert.CertificateEncodingException;
import java.util.Collections;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.zowe.apiml.gateway.x509.ClientCertFilterFactory.CLIENT_CERT_HEADER;


@Slf4j
@Configuration
public class AuthEndpointConfig {

    private final WebClient webClient;
    private final WebClient webClientClientCert;
    private final ReactiveLoadBalancer.Factory<ServiceInstance> serviceInstanceFactory;

    public AuthEndpointConfig(
        WebClient webClient,
        @Qualifier("webClientClientCert") WebClient webClientClientCert,
        ReactiveLoadBalancer.Factory<ServiceInstance> serviceInstanceFactory
    ) {
        this.webClient = webClient;
        this.webClientClientCert = webClientClientCert;
        this.serviceInstanceFactory = serviceInstanceFactory;
    }

    private WebClient.RequestBodySpec getWebclient(ServerRequest serverRequest, String path) {
        var sslInfo = serverRequest.exchange().getRequest().getSslInfo();
        var webClient = sslInfo == null ? this.webClient : this.webClientClientCert;
        webClient = webClient.mutate()
            .filter(new ReactorLoadBalancerExchangeFilterFunction(serviceInstanceFactory, Collections.emptyList()))
            .build();

        var request = webClient
            .method(serverRequest.method())
            .uri("lb://zaas/zaas" + path)
            .headers(headers -> headers.addAll(serverRequest.headers().asHttpHeaders()))
            .headers(headers -> headers.remove(CLIENT_CERT_HEADER));

        if (sslInfo != null) {
            return request.headers(headers -> {
                try {
                    headers.add(CLIENT_CERT_HEADER, X509Util.getEncodedClientCertificate(sslInfo));
                } catch (CertificateEncodingException e) {
                    throw new IllegalStateException("Cannot forward client certificate", e);
                }
            });
        }

        return request;
    }

    private Mono<ServerResponse> resend(ServerRequest request, String path, String body) {
        var bodyInserter = StringUtils.isNotEmpty(body) ? BodyInserters.fromValue(body) : BodyInserters.empty();
        return getWebclient(request, path)
            .body(bodyInserter)
            .exchangeToMono(clientResponse -> {
                var response = ServerResponse.status(clientResponse.statusCode());
                response.headers(httpHeaders -> httpHeaders.addAll(clientResponse.headers().asHttpHeaders()));
                return clientResponse.bodyToMono(String.class).flatMap(responseBody -> {
                    if (!responseBody.isEmpty()) {
                        return response.bodyValue(responseBody);
                    }
                    return response.build();
                }).switchIfEmpty(response.build());
            });
    }

    private HandlerFunction<ServerResponse> resendTo(String path) {
        return request -> request.bodyToMono(String.class)
            .switchIfEmpty(Mono.just(""))
            .flatMap(body -> resend(request, path, body))
            .doOnError(e -> log.debug("Cannot resend authentication call to the ZAAS", e));
    }

    @Bean
    public RouterFunction<ServerResponse> routes() {
        return route(POST("/gateway/api/v1/auth/login"), resendTo("/api/v1/auth/login"))
            .andRoute(POST("/gateway/api/v1/auth/logout"), resendTo("/api/v1/auth/logout"))
            .andRoute(GET("/gateway/api/v1/auth/query"), resendTo("/api/v1/auth/query"))
            .andRoute(POST("/gateway/api/v1/auth/ticket"), resendTo("/api/v1/auth/ticket"))
            .andRoute(DELETE("/gateway/api/v1/auth/access-token/revoke"), resendTo("/api/v1/auth/access-token/revoke"))
            .andRoute(POST("/gateway/api/v1/auth/access-token/validate"), resendTo("/api/v1/auth/access-token/validate"))
            .andRoute(POST("/gateway/api/v1/auth/access-token/generate"), resendTo("/api/v1/auth/access-token/generate"))
            .andRoute(DELETE("/gateway/auth/access-token/revoke/tokens/user"), resendTo("/auth/access-token/revoke/tokens/user"))
            .andRoute(DELETE("/gateway/auth/access-token/revoke/tokens"), resendTo("/auth/access-token/revoke/tokens"))
            .andRoute(DELETE("/gateway/auth/access-token/revoke/tokens/scope"), resendTo("/auth/access-token/revoke/tokens/scope"))
            .andRoute(DELETE("/gateway/auth/access-token/evict"), resendTo("/auth/access-token/evict"))
            .andRoute(GET("/gateway/api/v1//keys/public/all"), resendTo("/api/v1//keys/public/all"))
            .andRoute(GET("/gateway/api/v1/keys/public/current"), resendTo("/api/v1/keys/public/current"))
            .andRoute(POST("/gateway/api/v1/oidc-token/validate"), resendTo("/api/v1/oidc-token/validate"))
            .andRoute(GET("/gateway/api/v1/oidc/webfinger"), resendTo("/api/v1/oidc/webfinger"));
    }

}
