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

import static org.zowe.apiml.security.common.token.TokenAuthentication.createAuthenticated;

@RequiredArgsConstructor
public class TokenAuthFilter implements WebFilter {

    public static final String HEADER_PREFIX = "Bearer ";
    private final TokenProvider tokenProvider;
    private final AuthConfigurationProperties authConfigurationProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        var token = resolveToken(exchange.getRequest()).filter(StringUtils::isNotBlank);
        return token.map(jwt -> tokenProvider
            .validateToken(jwt)
            .flatMap(resp -> {
                if (StringUtils.isNotBlank(resp.getUserId())) {
                    Authentication authentication = createAuthenticated(resp.getUserId(), jwt);
                    return chain.filter(exchange)
                        .contextWrite((context) -> ReactiveSecurityContextHolder.withAuthentication(authentication));
                }
                return chain.filter(exchange);
            })
        ).orElseGet(() -> chain.filter(exchange));
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
