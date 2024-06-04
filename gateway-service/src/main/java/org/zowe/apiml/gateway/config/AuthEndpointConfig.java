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

import com.netflix.discovery.DiscoveryClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.integration.webflux.dsl.WebFlux;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RouterFunction;
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

    private final DiscoveryClient discoveryClient;

    @Bean
    public RouterFunction<ServerResponse> routes(
            //@Qualifier("webClientLoadBalanced")
            WebClient webClient/*PostHandler postController*/,
            ReactiveLoadBalancer.Factory<ServiceInstance> serviceInstanceFactory,
            HttpClient httpClient
    ) {

        return route(GET("/gateway/api/v1/auth/query"), request -> {
                return Mono.empty();
            })
            .andRoute(POST("/gateway/api/v1/auth/login"), request -> {

                System.out.println(discoveryClient.getApplication("zaas").size());

                //WebFlux.outboundGateway("lb://zaas/zaas/api/v1/auth/login").

                return request.bodyToMono(String.class)
                        .flatMap(body -> {
                            return  WebClient.builder()
                                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                                    .filter(new ReactorLoadBalancerExchangeFilterFunction(serviceInstanceFactory, Collections.emptyList()))
                                    .build()
                                    .post()
                                    .uri("lb://zaas/zaas/api/v1/auth/login")
                                    //.uri("https://zaas/zaas/api/v1/auth/login")
                                    //.uri("https://localhost:10023/zaas/api/v1/auth/login")
                                    .headers(headers -> {
                                        headers.add(HttpHeaders.AUTHORIZATION, request.headers().firstHeader(HttpHeaders.AUTHORIZATION));
                                        headers.add(HttpHeaders.COOKIE, request.headers().firstHeader(HttpHeaders.COOKIE));
                                    })
                                    .contentType(MediaType.APPLICATION_JSON)
                                    //.body(BodyInserters.fromValue(body))
                                    //.uri("https://localhost:10023/zaas/api/v1/auth/login")
                                    //.body(BodyInserters.fromValue(request.))
                                    //.body(request.bo)
                                    .exchangeToMono(clientResponse -> {
                                        clientResponse.bodyToMono(String.class).flatMap(b -> {
                                            System.out.println(b);
                                            return Mono.empty();
                                        });
                                        var builder = ServerResponse.status(clientResponse.statusCode());
                                        //builder.bodyValue(BodyInserters.f)
                                        return builder.build();
                                    });
                        });

            });
    }

}
