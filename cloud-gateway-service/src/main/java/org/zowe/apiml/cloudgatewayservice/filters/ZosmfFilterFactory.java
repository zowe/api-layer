/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.filters;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.cloudgatewayservice.service.InstanceInfoService;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.zaas.zosmf.ZosmfResponse;
import reactor.core.publisher.Mono;

@Service
public class ZosmfFilterFactory extends AbstractGatewayFilterFactory<ZosmfFilterFactory.Config> {
    private final WebClient webClient;
    private final InstanceInfoService instanceInfoService;
    private final MessageService messageService;
    private final String zosmfTokensUrl = "%s://%s:%s/%s/zaas/zosmf";

    public ZosmfFilterFactory(WebClient webClient, InstanceInfoService instanceInfoService, MessageService messageService) {
        super(Config.class);
        this.webClient = webClient;
        this.instanceInfoService = instanceInfoService;
        this.messageService = messageService;
    }

    @Override
    public GatewayFilter apply(Config config) {
        try {
            return (ServerWebExchange exchange, GatewayFilterChain chain) ->
                instanceInfoService.getServiceInstance("gateway").flatMap(instances -> {
                    for (ServiceInstance instance : instances) {
                        return webClient.post()
                            .uri(String.format(zosmfTokensUrl, instance.getScheme(), instance.getHost(), instance.getPort(), instance.getServiceId().toLowerCase()))
                            .headers(headers -> headers.addAll(exchange.getRequest().getHeaders()))
                            .retrieve().onStatus(HttpStatus::is4xxClientError, (response) -> Mono.empty())
                            .bodyToMono(ZosmfResponse.class)
                            .flatMap(response -> {
                                if (response.getToken() == null) {
                                    ServerHttpRequest request = updateHeadersForError(exchange, "Invalid or missing authentication.");
                                    return chain.filter(exchange.mutate().request(request).build());
                                } else {
                                    final String headerValue = response.getCookieName() + "=" + response.getToken();
                                    ServerHttpRequest request = addRequestHeader(exchange, HttpHeaders.COOKIE, headerValue);
                                    return chain.filter(exchange.mutate().request(request).build());
                                }
                            });
                    }
                    ServerHttpRequest request = updateHeadersForError(exchange, "All gateway service instances failed to respond.");
                    return chain.filter(exchange.mutate().request(request).build());
                });
        } catch (Exception e) {
            return ((exchange, chain) -> {
                ServerHttpRequest request = updateHeadersForError(exchange, e.getMessage());
                return chain.filter(exchange.mutate().request(request).build());
            });
        }
    }

    private ServerHttpRequest updateHeadersForError(ServerWebExchange exchange, String errorMessage) {
        ServerHttpRequest request = addRequestHeader(exchange, ApimlConstants.AUTH_FAIL_HEADER, messageService.createMessage("org.zowe.apiml.security.ticket.generateFailed", errorMessage).mapToLogMessage());
        exchange.getResponse().getHeaders().add(ApimlConstants.AUTH_FAIL_HEADER, messageService.createMessage("org.zowe.apiml.security.ticket.generateFailed", errorMessage).mapToLogMessage());
        return request;
    }

    private ServerHttpRequest addRequestHeader(ServerWebExchange exchange, String key, String value) {
        return exchange.getRequest().mutate()
            .headers(headers -> {
                    headers.remove(HttpHeaders.COOKIE);
                    headers.add(key, value);
                }
            ).build();
    }


    public static class Config {

    }
}
