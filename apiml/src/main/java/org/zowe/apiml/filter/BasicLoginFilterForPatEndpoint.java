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
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.zowe.apiml.security.common.login.LoginFilter;
import org.zowe.apiml.security.common.login.LoginRequest;
import org.zowe.apiml.zaas.security.config.CompoundAuthProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Reactive WebFilter responsible for extracting and authenticating login credentials on the
 * <code>/gateway/api/v1/auth/access-token/generate</code> endpoint, as part of the Personal Access Token (PAT) generation flow.
 *
 * <p>This filter supports two authentication input methods:</p>
 * <ul>
 *   <li>HTTP Basic Authentication via the <code>Authorization</code> header.</li>
 *   <li>JSON payload in the request body containing {@link LoginRequest} fields (username and password).</li>
 * </ul>
 *
 * <p>Execution order and integration:</p>
 * <ul>
 *   <li>Runs after {@link StoreAccessTokenInfoWebFilter}, which parses the request body and stores the access token info.</li>
 *   <li>Runs before {@link X509AuthFilter}, allowing dual-mode credential-based and certificate-based authentication.</li>
 *   <li>Integrated into a security filter chain that matches <code>/gateway/api/v1/auth/access-token/generate</code>.</li>
 * </ul>
 *
 * <p>Authentication is delegated to a {@link ProviderManager} configured with a {@link CompoundAuthProvider}.
 * Upon successful authentication, a {@link ServerAuthenticationSuccessHandler} is invoked. In case of failure,
 * a {@link ServerAuthenticationFailureHandler} is used to return the appropriate response.</p>
 *
 * @see StoreAccessTokenInfoWebFilter
 * @see X509AuthFilter
 * @see LoginRequest
 * @see CompoundAuthProvider
 * @see ServerAuthenticationSuccessHandler
 * @see ServerAuthenticationFailureHandler
 */
@Slf4j
public class BasicLoginFilterForPatEndpoint implements WebFilter {

    private final ObjectMapper mapper;
    private final ReactiveAuthenticationManagerAdapter authenticationManager;
    private final ServerAuthenticationSuccessHandler successHandler;
    private final ServerAuthenticationFailureHandler failureHandler;

    public BasicLoginFilterForPatEndpoint(CompoundAuthProvider compoundAuthProvider,
                                          ObjectMapper mapper,
                                          ServerAuthenticationSuccessHandler successHandler,
                                          ServerAuthenticationFailureHandler failureHandler) {
        var authManager = new ProviderManager(compoundAuthProvider);
        this.authenticationManager = new ReactiveAuthenticationManagerAdapter(authManager);
        this.mapper = mapper;
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return extractBasicAuth(exchange)
            .map(this::getToken)
            .switchIfEmpty(getTokenFromBody(exchange))
            .flatMap(token ->
                authenticationManager.authenticate(token)
                    .flatMap(authentication ->
                        successHandler.onAuthenticationSuccess(
                            new WebFilterExchange(exchange, chain),
                            authentication
                        ).contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
                    )
                    .onErrorResume(AuthenticationException.class, ex -> {
                        log.debug("Authentication failed for PAT endpoint: {}", ex.getMessage());
                        return failureHandler.onAuthenticationFailure(
                            new WebFilterExchange(exchange, chain),
                            ex
                        );
                    })
            )
            .switchIfEmpty(chain.filter(exchange));
    }

    private Mono<? extends AbstractAuthenticationToken> getTokenFromBody(ServerWebExchange exchange) {
        return Mono.defer(() ->
                getCredentialsFromBody(exchange).map(this::getToken)
        );
    }

    AbstractAuthenticationToken getToken(LoginRequest credentials) {
        return new UsernamePasswordAuthenticationToken(credentials.getUsername(), credentials.getPassword());
    }

    private Mono<LoginRequest> extractBasicAuth(ServerWebExchange exchange) {
        return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
            .map(header ->
                LoginFilter.getCredentialFromAuthorizationHeader(Optional.of(header)).get())
            .onErrorResume(e -> {
                log.debug("Failed to decode Basic Auth header (PAT endpoint): {}", e.getMessage());
                return Mono.empty();
            });
    }

    private Mono<LoginRequest> getCredentialsFromBody(ServerWebExchange exchange) {
        return exchange.getRequest().getBody().flatMap(buffer -> {
            try {
                byte[] bytes = new byte[buffer.readableByteCount()];
                buffer.read(bytes);
                DataBufferUtils.release(buffer);
                String bodyString = new String(bytes, StandardCharsets.UTF_8);
                return Mono.just(mapper.readValue(bodyString, LoginRequest.class));
            } catch (IOException e) {
                log.debug("Invalid login body (PAT endpoint)");
                return Flux.error(new AuthenticationCredentialsNotFoundException("Login object has wrong format."));
            }
        }).next();
    }
}
