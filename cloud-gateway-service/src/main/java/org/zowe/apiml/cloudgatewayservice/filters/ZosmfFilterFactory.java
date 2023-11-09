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

import lombok.EqualsAndHashCode;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.cloudgatewayservice.service.InstanceInfoService;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.zaas.zosmf.ZosmfResponse;
import reactor.core.publisher.Mono;


@Service
public class ZosmfFilterFactory extends AbstractAuthSchemeFactory<ZosmfFilterFactory.Config, ZosmfResponse, Object> {

    public ZosmfFilterFactory(WebClient webClient, InstanceInfoService instanceInfoService, MessageService messageService) {
        super(Config.class, webClient, instanceInfoService, messageService);
    }

    @Override
    public GatewayFilter apply(Config config) {
        try {
            return createGatewayFilter(config, null);
        } catch (Exception e) {
            return ((exchange, chain) -> {
                ServerHttpRequest request = updateHeadersForError(exchange, e.getMessage());
                return chain.filter(exchange.mutate().request(request).build());
            });
        }
    }

    @Override
    protected Class<ZosmfResponse> getResponseClass() {
        return ZosmfResponse.class;
    }

    @Override
    protected WebClient.RequestHeadersSpec<?> createRequest(ServerWebExchange exchange, ServiceInstance instance, Object data) {
        String zosmfTokensUrl = String.format("%s://%s:%d/%s/zaas/zosmf", instance.getScheme(), instance.getHost(), instance.getPort(), instance.getServiceId().toLowerCase());
        return webClient.post()
            .uri(zosmfTokensUrl);
    }

    @Override
    protected Mono<Void> processResponse(ServerWebExchange exchange, GatewayFilterChain chain, ZosmfResponse response) {
        if (response.getToken() == null) {
            throw new IllegalArgumentException("The ZAAS is not configured properly");
        }
        ServerHttpRequest request = setCookie(exchange, response.getCookieName(), response.getToken());
        return chain.filter(exchange.mutate().request(request).build());
    }

    @EqualsAndHashCode(callSuper = true)
    public static class Config extends AbstractAuthSchemeFactory.AbstractConfig {

    }

}
