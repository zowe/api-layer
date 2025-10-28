/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.util;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import reactor.core.publisher.Mono;

import static org.zowe.apiml.constants.ApimlConstants.BEARER_AUTHENTICATION_PREFIX;
import static org.zowe.apiml.security.SecurityUtils.COOKIE_AUTH_NAME;

@Component
@RequiredArgsConstructor
public class HttpUtils {

    private final AuthConfigurationProperties authConfigurationProperties;

    private AuthConfigurationProperties.CookieProperties cp;
    private int cookieMaxAge = -1;

    @PostConstruct
    protected void readConfig() {
        cp = authConfigurationProperties.getCookieProperties();
        if (cp.getCookieMaxAge() != null) {
            cookieMaxAge = cp.getCookieMaxAge();
        }
    }

    public ResponseCookie createResponseCookie(String jwt) {
        return ResponseCookie.from(cp.getCookieName(), jwt)
            .path(cp.getCookiePath())
            .sameSite(cp.getCookieSameSite().getValue())
            .maxAge(cookieMaxAge)
            .httpOnly(true)
            .secure(cp.isCookieSecure())
            .build();
    }

    public ResponseCookie createResponseCookieRemoval() {
        return ResponseCookie.from(cp.getCookieName())
            .path(cp.getCookiePath())
            .sameSite(cp.getCookieSameSite().getValue())
            .maxAge(0L)
            .httpOnly(true)
            .secure(cp.isCookieSecure())
            .build();
    }

    public Mono<String> getTokenFromRequest(ServerWebExchange exchange) {
        return getCookieValue(exchange, COOKIE_AUTH_NAME)
            .switchIfEmpty(getBearerTokenFromHeaderReactive(exchange));
    }

    public Mono<String> getBearerTokenFromHeaderReactive(ServerWebExchange exchange) {
        return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
            .filter(authHeader -> authHeader.toLowerCase().startsWith((BEARER_AUTHENTICATION_PREFIX + " ").toLowerCase()))
            .map(authHeader -> authHeader.substring((BEARER_AUTHENTICATION_PREFIX + " ").length()).trim())
            .filter(token -> !token.isBlank());
    }

    public Mono<String> getCookieValue(ServerWebExchange exchange, String cookieName) {
        return Mono.justOrEmpty(exchange)
            .mapNotNull(ex -> ex.getRequest().getCookies().getFirst(cookieName))
            .map(HttpCookie::getValue);
    }
}
