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
