/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters;

import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.gateway.service.InstanceInfoService;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.zaas.ZaasTokenResponse;
import reactor.core.publisher.Mono;

import java.util.Optional;


public abstract class TokenFilterFactory extends AbstractAuthSchemeFactory<TokenFilterFactory.Config, ZaasTokenResponse, Object> {

    protected TokenFilterFactory(WebClient webClient, InstanceInfoService instanceInfoService, MessageService messageService) {
        super(Config.class, webClient, instanceInfoService, messageService);
    }

    public abstract String getEndpointUrl(ServiceInstance instance);

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
    protected Class<ZaasTokenResponse> getResponseClass() {
        return ZaasTokenResponse.class;
    }

    @Override
    protected ZaasTokenResponse getResponseFor401() {
        return new ZaasTokenResponse();
    }

    @Override
    protected WebClient.RequestHeadersSpec<?> createRequest(ServiceInstance instance, Object data) {
        String tokensUrl = getEndpointUrl(instance);
        return webClient.post()
            .uri(tokensUrl);
    }

    @Override
    @SuppressWarnings("squid:S2092")    // the internal API cannot define generic more specifically
    protected Mono<Void> processResponse(ServerWebExchange exchange, GatewayFilterChain chain, ZaasTokenResponse response) {
        ServerHttpRequest request = null;
        if (response.getToken() != null) {
            if (!StringUtils.isEmpty(response.getCookieName())) {
                request = exchange.getRequest().mutate().headers(headers -> {
                    // FIXME add util
                    String cookieValue = response.getCookieName() + "=" + response.getToken();
                    String currentCookies = Optional.ofNullable(headers.get(HttpHeaders.COOKIE))
                        .filter(cookie -> !cookie.isEmpty())
                        .map(cookie -> cookie.get(0))
                        .map(currentCookieValue -> currentCookieValue + "; " + cookieValue)
                        .orElse(cookieValue);
                    headers.set(HttpHeaders.COOKIE, currentCookies);
                }).build();
            }
            if (!StringUtils.isEmpty(response.getHeaderName())) {
                request = exchange.getRequest().mutate().headers(headers ->
                    headers.add(response.getHeaderName(), response.getToken())
                ).build();
            }
        }
        if (request == null) {
            request = updateHeadersForError(exchange, "Invalid or missing authentication");
        }

        exchange = exchange.mutate().request(request).build();
        return chain.filter(exchange);
    }

    @EqualsAndHashCode(callSuper = true)
    public static class Config extends AbstractAuthSchemeFactory.AbstractConfig {

    }

}
