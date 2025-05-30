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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.zowe.apiml.handler.SuccessfulPersonalAccessTokenHandler;
import org.zowe.apiml.security.common.error.AccessTokenBodyNotValidException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

import static org.zowe.apiml.security.common.filter.StoreAccessTokenInfoFilter.TOKEN_REQUEST;

/**
 * A reactive WebFilter that parses and stores access token request details from the request body.
 * <p>
 * This filter reads the request body expecting a {@link SuccessfulPersonalAccessTokenHandler.AccessTokenRequest}
 * JSON payload. It extracts and normalizes the scopes, storing the full object in the exchange attributes
 * under the key {@code TOKEN_REQUEST} for downstream filters to use.
 * <p>
 * If the payload is invalid or missing required fields (like scopes), the provided
 * {@link ServerAuthenticationFailureHandler} is triggered to handle the error response.
 * <p>
 * The original body is reconstructed and passed downstream using {@link ServerHttpRequestDecorator}.
 */
@Slf4j
@RequiredArgsConstructor
public class StoreAccessTokenInfoWebFilter implements WebFilter {

    private final ServerAuthenticationFailureHandler failureHandler;
    private final ObjectMapper mapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return DataBufferUtils.join(exchange.getRequest().getBody())
            .flatMap(dataBuffer -> {
                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(bytes);
                DataBufferUtils.release(dataBuffer);
                String body = new String(bytes, StandardCharsets.UTF_8);

                try {
                    SuccessfulPersonalAccessTokenHandler.AccessTokenRequest accessTokenRequest =
                        mapper.readValue(body, SuccessfulPersonalAccessTokenHandler.AccessTokenRequest.class);

                    Set<String> scopes = accessTokenRequest.getScopes();
                    if (scopes == null || scopes.isEmpty()) {
                        return failureHandler.onAuthenticationFailure(
                            new WebFilterExchange(exchange, chain),
                            new AccessTokenBodyNotValidException("org.zowe.apiml.security.token.accessTokenBodyMissingScopes")
                        );
                    }

                    accessTokenRequest.setScopes(scopes.stream().map(String::toLowerCase).collect(Collectors.toSet()));
                    exchange.getAttributes().put(TOKEN_REQUEST, accessTokenRequest);

                    ServerHttpRequestDecorator decorated = new ServerHttpRequestDecorator(exchange.getRequest()) {
                        @Override
                        public Flux<DataBuffer> getBody() {
                            return Flux.just(exchange.getResponse().bufferFactory().wrap(bytes));
                        }
                    };

                    return chain.filter(exchange.mutate().request(decorated).build());

                } catch (Exception e) {
                    log.error("Failed to parse access token request body", e);
                    return failureHandler.onAuthenticationFailure(
                        new WebFilterExchange(exchange, chain),
                        new AccessTokenBodyNotValidException("org.zowe.apiml.security.query.invalidAccessTokenBody")
                    );
                }
            });
    }
}
