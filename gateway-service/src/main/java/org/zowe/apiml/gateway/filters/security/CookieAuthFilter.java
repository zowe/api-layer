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
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import reactor.core.publisher.Mono;

import java.util.Optional;

@RequiredArgsConstructor
public class CookieAuthFilter implements WebFilter {

    private final AuthConfigurationProperties authConfigurationProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return extractToken(exchange.getRequest())
            .map(token -> {
                AbstractAuthenticationToken authentication = new TokenAuthentication(token, TokenAuthentication.Type.JWT);
                authentication.setAuthenticated(true);
                return authentication;
            })
            .map(auth -> chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth)))
            .orElseGet(() -> chain.filter(exchange));
    }

    private Optional<String> extractToken(ServerHttpRequest request) {
        String cookieName = authConfigurationProperties.getCookieProperties().getCookieName();
        return request.getCookies().getFirst(cookieName) != null
            ? Optional.ofNullable(request.getCookies().getFirst(cookieName).getValue())
            : Optional.empty();
    }
}
