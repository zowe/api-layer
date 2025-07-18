/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.web.server.WebSession;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import reactor.core.publisher.Mono;

/**
 * Handles logout success by removing cookie and clearing security context
 */
@RequiredArgsConstructor
public class ApiCatalogLogoutSuccessHandler implements ServerLogoutSuccessHandler {

    private final AuthConfigurationProperties authConfigurationProperties;

    /**
     * Clears cookie, session, context and sets response code
     *
     * @param exchange            Request exchange
     * @param authentication      Valid authentication
     */
    @Override
    public Mono<Void> onLogoutSuccess(WebFilterExchange exchange, Authentication authentication) {
        return exchange.getExchange().getSession()
            .flatMap(WebSession::invalidate)
            .then(Mono.defer(() -> {
                var response = exchange.getExchange().getResponse();
                response.addCookie(ResponseCookie.from(authConfigurationProperties.getCookieProperties().getCookieName())
                    .path(authConfigurationProperties.getCookieProperties().getCookiePath())
                    .secure(true)
                    .httpOnly(true)
                    .maxAge(0L)
                    .build()
                );
                response.setStatusCode(HttpStatusCode.valueOf(HttpServletResponse.SC_OK));
                return Mono.empty();
            }));
    }

}
