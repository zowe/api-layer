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
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.zowe.apiml.gateway.service.BasicAuthProvider;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.Optional;

import static org.zowe.apiml.security.common.token.TokenAuthentication.createAuthenticatedFromHeader;

@RequiredArgsConstructor
public class BasicAuthFilter implements WebFilter {

    private final BasicAuthProvider basicAuthProvider;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        var authHeaderOpt = resolveAuth(exchange.getRequest());

        if (authHeaderOpt.isEmpty()) {
            return chain.filter(exchange); // no basic auth header, skip processing
        }

        var header = authHeaderOpt.get();
        String encodedCredentials = header.substring(6); // Remove "Basic "

        String decodedCredentials;
        try {
            decodedCredentials = new String(Base64.getDecoder().decode(encodedCredentials));
        } catch (IllegalArgumentException e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete(); // Explicit 401 if decoding fails
        }

        if (!decodedCredentials.contains(":")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete(); // Malformed header without colon
        }

        return basicAuthProvider.getToken(header)
            .flatMap(token -> {
                if (StringUtils.isEmpty(token)) {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete(); // Invalid credentials
                }
                var auth = createAuthenticatedFromHeader(token, header);
                return chain.filter(exchange)
                    .contextWrite(context -> ReactiveSecurityContextHolder.withAuthentication(auth));
            });
    }

    private Optional<String> resolveAuth(ServerHttpRequest request) {
        return Optional.ofNullable(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
            .filter(header -> StringUtils.startsWith(header, "Basic "));
    }

}

