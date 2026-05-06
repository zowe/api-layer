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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import org.zowe.apiml.gateway.service.TokenProvider;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import reactor.core.publisher.Mono;

import java.net.ConnectException;

import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.zowe.apiml.security.common.token.TokenAuthentication.createAuthenticated;

@RequiredArgsConstructor
public class TokenAuthFilter extends AbstractTokenAuthFilter {

    private final TokenProvider tokenProvider;
    private final AuthConfigurationProperties authConfigurationProperties;
    private final AuthExceptionHandlerReactive authExceptionHandlerReactive;

    @Override
    protected AuthConfigurationProperties getAuthConfigurationProperties() {
        return authConfigurationProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .map(Authentication::isAuthenticated)
            .defaultIfEmpty(false)
            .flatMap(alreadyAuthenticated -> {
                if (alreadyAuthenticated) {
                    return chain.filter(exchange);
                }

                var token = resolveToken(exchange.getRequest()).filter(StringUtils::isNotBlank);
                return token
                    .map(jwt -> tokenProvider
                        .validateToken(jwt)
                        .flatMap(resp -> {
                            if (StringUtils.isNotBlank(resp.getUserId())) {
                                Authentication authentication = createAuthenticated(resp.getUserId(), jwt, TokenAuthentication.Type.JWT);
                                return chain.filter(exchange)
                                    .contextWrite(context -> ReactiveSecurityContextHolder.withAuthentication(authentication));
                            }
                            return authExceptionHandlerReactive.handleTokenNotValid(exchange);

                        }).onErrorResume(ex -> {
                            if (isServiceUnavailable(ex)) {
                                return authExceptionHandlerReactive.handleServiceUnavailable(exchange);
                            } else {
                                return authExceptionHandlerReactive.handleTokenNotValid(exchange);
                            }
                        })
                    ).orElseGet(() -> chain.filter(exchange));
            });

    }


    private boolean isServiceUnavailable(Throwable ex) {
        return ex instanceof ConnectException
            || ex instanceof WebClientRequestException
            || (ex instanceof WebClientResponseException webEx && webEx.getStatusCode().value() == SC_SERVICE_UNAVAILABLE);
    }

}
