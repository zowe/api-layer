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
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ReactiveAuthenticationManager; // Assuming usage of reactive authentication manager
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.server.WebFilterExchange; // For passing to Spring Security reactive handlers
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.zowe.apiml.security.common.error.AuthMethodNotSupportedException;
import org.zowe.apiml.security.common.error.InvalidCertificateException;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.security.common.token.TokenNotProvidedException;
import org.zowe.apiml.security.common.token.TokenNotValidException;
import org.zowe.apiml.zaas.security.service.AuthenticationService;
import reactor.core.publisher.Mono;

import java.security.cert.X509Certificate;
import java.util.Objects;

/**
 * Reactive Filter for /query endpoint requests with JWT token.
 */
public class QueryWebFilter implements WebFilter {

    private final ServerAuthenticationSuccessHandler successHandler;
    private final ServerAuthenticationFailureHandler failureHandler;
    private final HttpMethod httpMethod;
    private final boolean protectedByCertificate;
    private final ReactiveAuthenticationManager authenticationManager;

    public QueryWebFilter(
        ServerAuthenticationSuccessHandler successHandler,
        ServerAuthenticationFailureHandler failureHandler,

        HttpMethod httpMethod,
        boolean protectedByCertificate,
        ReactiveAuthenticationManager authenticationService) {
        this.successHandler = Objects.requireNonNull(successHandler, "successHandler cannot be null");
        this.failureHandler = Objects.requireNonNull(failureHandler, "failureHandler cannot be null");
        this.httpMethod = Objects.requireNonNull(httpMethod, "httpMethod cannot be null");
        this.protectedByCertificate = protectedByCertificate;
        this.authenticationManager = authenticationService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Check if the request path matches the configured endpoint path


        // Check HTTP Method
        if (!exchange.getRequest().getMethod().equals(this.httpMethod)) {
            AuthMethodNotSupportedException ex = new AuthMethodNotSupportedException(
                exchange.getRequest().getMethod().name());
            // It's generally better to delegate to the failureHandler if it can handle this,
            // or set a specific response. For simplicity here, we'll use the failure handler.
            return this.failureHandler.onAuthenticationFailure(
                new WebFilterExchange(exchange, chain), ex);
        }

        return attemptAuthentication(exchange, chain)
            .flatMap(authResult -> {
                // Authentication successful
                // The successHandler will typically commit the response or proceed with the chain
                // after setting the authentication in the context.
                return this.successHandler.onAuthenticationSuccess(
                        new WebFilterExchange(exchange, chain), authResult)
                    // After success handler, ensure authentication is in the reactive context for downstream
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authResult));
            })
            .onErrorResume(AuthenticationException.class, failed -> {
                // Authentication failed
                // Clear context is handled by ReactiveSecurityContextHolder if needed, or by the failure handler.
                // For reactive, context isn't typically "cleared" in the same way as thread-local,
                // but rather, a new empty context or context without authentication is used.
                return this.failureHandler.onAuthenticationFailure(
                    new WebFilterExchange(exchange, chain), failed);
            })
            // Handle specific non-AuthenticationException errors if they are thrown before authentication attempt
            .onErrorResume(AuthMethodNotSupportedException.class, ex ->
                this.failureHandler.onAuthenticationFailure(new WebFilterExchange(exchange, chain), new AuthenticationException(ex.getMessage(), ex) {})
            )
            .onErrorResume(InvalidCertificateException.class, ex ->
                this.failureHandler.onAuthenticationFailure(new WebFilterExchange(exchange, chain), new AuthenticationException(ex.getMessage(), ex) {})
            )
            .onErrorResume(TokenNotProvidedException.class, ex ->
                this.failureHandler.onAuthenticationFailure(new WebFilterExchange(exchange, chain), new AuthenticationException(ex.getMessage(), ex) {})
            );
    }

    private Mono<Authentication> attemptAuthentication(ServerWebExchange exchange, WebFilterChain chain) {
        Mono<Void> certificateCheckMono = Mono.empty();

        if (protectedByCertificate) {
            certificateCheckMono = ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth != null && auth.isAuthenticated() && auth.getCredentials() instanceof X509Certificate)
                .switchIfEmpty(Mono.error(new InvalidCertificateException("Invalid or missing certificate authentication.")))
                .then(); // We only care that it passed, not the auth object itself for this step.
        }

        return certificateCheckMono
            .then(LogoutHandler.getTokenFromRequest(exchange)) // Reactive method
                .switchIfEmpty(Mono.error(new TokenNotProvidedException("Authorization token not provided.")))
                .flatMap(tokenValue -> {
                    Authentication tokenAuthRequest = new TokenAuthentication(tokenValue, TokenAuthentication.Type.JWT);
                    return this.authenticationManager.authenticate(tokenAuthRequest)
                        .filter(Authentication::isAuthenticated)
                        .switchIfEmpty(Mono.error(new TokenNotValidException("JWT Token is not authenticated")));
                });
    }
}
