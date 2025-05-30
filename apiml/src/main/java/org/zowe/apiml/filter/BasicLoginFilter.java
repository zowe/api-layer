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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.zowe.apiml.handler.FailedAuthenticationWebHandler;
import org.zowe.apiml.security.common.login.LoginFilter;
import org.zowe.apiml.security.common.login.LoginRequest;
import org.zowe.apiml.zaas.security.config.CompoundAuthProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    private final ObjectMapper mapper;
    private final ReactiveAuthenticationManagerAdapter authenticationManager;
    private final FailedAuthenticationWebHandler failedAuthenticationWebHandler;

    public BasicLoginFilter(CompoundAuthProvider compoundAuthProvider, ObjectMapper mapper, FailedAuthenticationWebHandler failedAuthenticationWebHandler) {
        var authManager = new ProviderManager(compoundAuthProvider);
        this.authenticationManager = new ReactiveAuthenticationManagerAdapter(authManager);
        this.mapper = mapper;
        this.failedAuthenticationWebHandler = failedAuthenticationWebHandler;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return extractBasicAuth(exchange)
            .map(this::getToken)
            .switchIfEmpty(getTokenFromBody(exchange))
            .switchIfEmpty(chain.filter(exchange).then(Mono.empty()))
            .flatMap(token ->
                authenticationManager.authenticate(token)
                    .flatMap(authentication -> {
                        SecurityContextImpl securityContext = new SecurityContextImpl();
                        securityContext.setAuthentication(authentication);
                        return chain.filter(exchange)
                            .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));
                    })
                    .onErrorResume(AuthenticationException.class, ex -> failedAuthenticationWebHandler.onAuthenticationFailure(new WebFilterExchange(exchange, chain), ex))
            ).onErrorResume(AuthenticationException.class, ex -> failedAuthenticationWebHandler.onAuthenticationFailure(new WebFilterExchange(exchange, chain), ex)
        );
    }

    AbstractAuthenticationToken getToken(LoginRequest credentials) {
        return new UsernamePasswordAuthenticationToken(credentials.getUsername(), credentials.getPassword());
    }

    private Mono<? extends AbstractAuthenticationToken> getTokenFromBody(ServerWebExchange exchange) {
        return Mono.defer(() ->
                getCredentialsFromBody(exchange).map(this::getToken)
        );
    }

    private Mono<LoginRequest> extractBasicAuth(ServerWebExchange exchange) {
        return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
            .map(header ->
                LoginFilter.getCredentialFromAuthorizationHeader(Optional.of(header)).get())
            .onErrorResume(e -> {
                log.debug("Failed to decode Basic Auth header: {}", e.getMessage());
                return Mono.empty(); // Return empty if decoding fails
            });
    }

    private Mono<LoginRequest> getCredentialsFromBody(ServerWebExchange exchange) {
        // method available could return 0 even there are some data, depends on the implementation
        return exchange.getRequest().getBody().flatMap(buffer -> {
                try {
                    byte[] bytes = new byte[buffer.readableByteCount()];
                    buffer.read(bytes);
                    DataBufferUtils.release(buffer);
                    String bodyString = new String(bytes, StandardCharsets.UTF_8);
                    var loginRequest = mapper.readValue(bodyString, LoginRequest.class);
                    if (loginRequest.getUsername() != null && loginRequest.getPassword() != null) {
                        return Mono.just(loginRequest);
                    }
                    return  Flux.error(new AuthenticationCredentialsNotFoundException("Login object has wrong format."));
                } catch (IOException e) {
                    log.debug("Authentication problem: login object has wrong format");
                    return Flux.error(new AuthenticationCredentialsNotFoundException("Login object has wrong format."));
                }
            }
        ).next();

    }

}
