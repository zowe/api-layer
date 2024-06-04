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

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.util.Collections;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
@RequiredArgsConstructor
public class AuthEndpointConfig {

    private Mono<ServerResponse> resend(WebClient webClient, ServerRequest request, String path, String body) {
        return webClient
            .method(request.method())
            .uri("lb://zaas/zaas" + path)
            .headers(headers -> headers.addAll(request.headers().asHttpHeaders()))
            .exchangeToMono(clientResponse -> {
                var builder = ServerResponse.status(clientResponse.statusCode());
                clientResponse.bodyToMono(String.class).flatMap(b -> {
                    builder.bodyValue(b);
                    return Mono.empty();
                });
                return builder.build();
            });
    }

    private HandlerFunction<ServerResponse> resendTo(WebClient webClient, String path) {
        return request -> request.bodyToMono(String.class)
            .flatMap(body -> resend(webClient, request, path, body));
    }

    @Bean
    public RouterFunction<ServerResponse> routes(HttpClient httpClient, ReactiveLoadBalancer.Factory<ServiceInstance> serviceInstanceFactory) {
        WebClient webClient = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .filter(new ReactorLoadBalancerExchangeFilterFunction(serviceInstanceFactory, Collections.emptyList()))
            .build();

        return route(GET("/gateway/api/v1/auth/query"), resendTo(webClient, "/api/v1/auth/query"))
            .andRoute(POST("/gateway/api/v1/auth/login"), resendTo(webClient, "/api/v1/auth/login"));
    }

}
