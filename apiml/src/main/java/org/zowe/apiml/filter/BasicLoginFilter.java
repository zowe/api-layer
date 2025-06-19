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

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.ReactiveAuthenticationManagerAdapter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.zowe.apiml.handler.FailedAuthenticationWebHandler;
import org.zowe.apiml.security.common.login.LoginFilter;
import org.zowe.apiml.security.common.login.LoginRequest;
import org.zowe.apiml.zaas.security.config.CompoundAuthProvider;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * Reactive WebFilter that handles basic login authentication using either:
 * <ul>
 *   <li>HTTP Basic Authorization header, or</li>
 *   <li>JSON body containing a {@link LoginRequest} with username and password.</li>
 * </ul>
 *
 * <p>Authentication flow:</p>
 * <ol>
 *   <li>Attempts to extract credentials from the Authorization header.</li>
 *   <li>If not found, attempts to extract credentials from the request body.</li>
 *   <li>If credentials are found, tries to authenticate using the reactive authentication manager.</li>
 *   <li>On success, stores the {@link org.springframework.security.core.Authentication} in the reactive security context.</li>
 *   <li>On failure, delegates to the provided {@link FailedAuthenticationWebHandler}.</li>
 *   <li>If no credentials are present, simply delegates to the next filter in the chain.</li>
 * </ol>
 *
 * <p>This filter is intended to be used on /login endpoints.</p>
 *
 * @see LoginRequest
 * @see CompoundAuthProvider
 * @see FailedAuthenticationWebHandler
 */
@Slf4j
public class BasicLoginFilter implements WebFilter {

    private final ReactiveAuthenticationManagerAdapter authenticationManager;
    private final FailedAuthenticationWebHandler failedAuthenticationWebHandler;

    public BasicLoginFilter(CompoundAuthProvider compoundAuthProvider, FailedAuthenticationWebHandler failedAuthenticationWebHandler) {
        var authManager = new ProviderManager(compoundAuthProvider);
        this.authenticationManager = new ReactiveAuthenticationManagerAdapter(authManager);
        this.failedAuthenticationWebHandler = failedAuthenticationWebHandler;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return extractBasicAuth(exchange)
            .map(this::useCredentials)
            .switchIfEmpty(Mono.<AbstractAuthenticationToken>defer(() -> chain.filter(exchange).then(Mono.empty())))
            .flatMap(credentials ->
                authenticationManager.authenticate(credentials)
                    .flatMap(authentication -> chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))))
            .onErrorResume(AuthenticationException.class, ex -> failedAuthenticationWebHandler.onAuthenticationFailure(new WebFilterExchange(exchange, chain), ex));
    }

    private AbstractAuthenticationToken useCredentials(LoginRequest credentials) {
        return new UsernamePasswordAuthenticationToken(credentials.getUsername(), credentials.getPassword());
    }

    private Mono<LoginRequest> extractBasicAuth(ServerWebExchange exchange) {
        var authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null) return Mono.empty();
        if (!authHeader.toLowerCase().startsWith("basic ")) return Mono.empty();

        return Mono.fromCallable(() ->
            LoginFilter.getCredentialFromAuthorizationHeader(Optional.of(authHeader))
                .filter(this::credentialsAvailable)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Username or password not provided."))
        ).onErrorResume(e -> {
            log.debug("Failed to decode Basic Auth header: {}", e.getMessage());
            return Mono.error(new AuthenticationCredentialsNotFoundException("Invalid basic authentication header", e));
        });
    }

    private boolean credentialsAvailable(LoginRequest credentials) {
        return credentials.getUsername() != null && !credentials.getUsername().isBlank()
            && credentials.getPassword() != null && credentials.getPassword().length > 0;
    }

}
