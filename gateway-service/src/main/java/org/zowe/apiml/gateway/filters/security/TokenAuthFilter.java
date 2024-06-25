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
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.zowe.apiml.gateway.service.TokenProvider;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.util.CookieUtil;
import reactor.core.publisher.Mono;

import java.net.HttpCookie;
import java.util.Optional;

@RequiredArgsConstructor
public class TokenAuthFilter implements WebFilter {

    public static final String HEADER_PREFIX = "Bearer ";
    private final TokenProvider tokenProvider;
    private final AuthConfigurationProperties authConfigurationProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        Mono<Void> response = chain.filter(exchange);
        return resolveToken(exchange.getRequest())
            .filter(token -> StringUtils.isNotBlank(token))
            .map(token -> this.tokenProvider.validateToken(token)
                .filter(resp -> StringUtils.isNotBlank(resp.getUserId()))
                .flatMap(resp -> {
                    Authentication authentication = this.tokenProvider.getAuthentication(resp.getUserId(), token);
                    return response.contextWrite((context) -> ReactiveSecurityContextHolder.withAuthentication(authentication));
                })
            ).orElse(response);
    }

    private Optional<String> resolveToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.startsWith(bearerToken, HEADER_PREFIX)) {
            return Optional.of(bearerToken.substring(HEADER_PREFIX.length()));
        }

        String cookieName = authConfigurationProperties.getCookieProperties().getCookieName();
        return CookieUtil.readCookies(request.getHeaders())
            .filter(httpCookie -> StringUtils.equals(cookieName, httpCookie.getName()))
            .findFirst()
            .map(HttpCookie::getValue);
    }

}
