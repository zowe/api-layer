/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.zowe.apiml.gateway.service.TokenProvider;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class TokenAuthFilter implements WebFilter {
    public static final String HEADER_PREFIX = "Bearer ";
    private final TokenProvider tokenProvider;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String token = resolveToken(exchange.getRequest());
        if (StringUtils.hasText(token)) {
            return this.tokenProvider.validateToken(token).flatMap(resp -> {
                if (StringUtils.hasText(resp.getUserId())) {
                    Authentication authentication = this.tokenProvider.getAuthentication(resp.getUserId(), token);
                    return chain.filter(exchange).contextWrite((context) -> ReactiveSecurityContextHolder.withAuthentication(authentication));
                }
                return chain.filter(exchange);
            });
        }
        return chain.filter(exchange);
    }

    private String resolveToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(bearerToken)) {
            bearerToken = request.getHeaders().getFirst(HttpHeaders.COOKIE);
            if (StringUtils.hasText(bearerToken)) {
                return bearerToken.substring(bearerToken.indexOf("=") + 1);
            }
        }
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(HEADER_PREFIX)) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
