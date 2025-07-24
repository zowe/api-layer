/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.filter;

import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.zowe.apiml.security.common.error.AuthMethodNotSupportedException;
import org.zowe.apiml.security.common.error.InvalidCertificateException;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.security.common.token.TokenNotProvidedException;
import org.zowe.apiml.security.common.token.TokenNotValidException;
import org.zowe.apiml.security.common.token.X509AuthenticationToken;
import org.zowe.apiml.util.HttpUtils;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Reactive Filter for endpoints have optional X.509 authentication + token authentication
 * such as /ticket, /query (check {@link WebSecurityConfig} to view which specific endpoints use it)
 */
public class QueryWebFilter implements WebFilter {

    private final ServerAuthenticationFailureHandler failureHandler;
    private final HttpMethod httpMethod;
    private final boolean protectedByCertificate;
    private final ReactiveAuthenticationManager authenticationService;
    private final HttpUtils httpUtils;

    public QueryWebFilter(
        ServerAuthenticationFailureHandler failureHandler,
        HttpMethod httpMethod,
        boolean protectedByCertificate,
        ReactiveAuthenticationManager authenticationService,
        HttpUtils httpUtils) {
        this.failureHandler = Objects.requireNonNull(failureHandler, "failureHandler cannot be null");
        this.httpMethod = Objects.requireNonNull(httpMethod, "httpMethod cannot be null");
        this.protectedByCertificate = protectedByCertificate;
        this.authenticationService = authenticationService;
        this.httpUtils = httpUtils;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!exchange.getRequest().getMethod().equals(this.httpMethod)) {
            AuthMethodNotSupportedException ex = new AuthMethodNotSupportedException(
                exchange.getRequest().getMethod().name());
            return this.failureHandler.onAuthenticationFailure(
                new WebFilterExchange(exchange, chain), ex);
        }

        return attemptAuthentication(exchange)
            .flatMap(authResult -> chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authResult))
            )
            .onErrorResume(AuthenticationException.class, failed ->
                this.failureHandler.onAuthenticationFailure(new WebFilterExchange(exchange, chain), failed));
    }

    private Mono<Authentication> attemptAuthentication(ServerWebExchange exchange) {
        Mono<Void> certificateCheckMono = Mono.empty();

        if (protectedByCertificate) {
            certificateCheckMono = ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .filter(X509AuthenticationToken.class::isInstance)
                .switchIfEmpty(Mono.error(new InvalidCertificateException("Invalid or missing certificate authentication.")))
                .then();
        }

        return certificateCheckMono
            .then(httpUtils.getTokenFromRequest(exchange))
                .switchIfEmpty(Mono.error(new TokenNotProvidedException("Authorization token not provided.")))
                .flatMap(tokenValue -> {
                    var tokenAuthRequest = new TokenAuthentication(tokenValue, TokenAuthentication.Type.JWT);
                    return this.authenticationService.authenticate(tokenAuthRequest)
                        .filter(Authentication::isAuthenticated)
                        .switchIfEmpty(Mono.error(new TokenNotValidException("JWT Token is not authenticated")));
                });

    }

}
