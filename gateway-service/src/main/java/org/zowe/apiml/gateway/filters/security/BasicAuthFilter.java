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
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.zowe.apiml.gateway.service.BasicAuthProvider;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static org.zowe.apiml.security.common.token.TokenAuthentication.createAuthenticatedFromHeader;

@RequiredArgsConstructor
public class BasicAuthFilter implements WebFilter {

    private final BasicAuthProvider basicAuthProvider;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        var authHeader = resolveAuth(exchange.getRequest());
        return authHeader
            .map(header -> basicAuthProvider.getToken(header)
                .flatMap(token -> {
                        if (StringUtils.isEmpty(token)) {
                            return chain.filter(exchange);
                        }
                        var auth = createAuthenticatedFromHeader(token, header);
                        return chain.filter(exchange)
                            .contextWrite(context -> ReactiveSecurityContextHolder.withAuthentication(auth));
                    }
                )
            )
            .orElseGet(() -> chain.filter(exchange));
    }

    private Optional<String> resolveAuth(ServerHttpRequest request) {
        return Optional.of(request.getHeaders())
            .map(head -> head.getFirst(HttpHeaders.AUTHORIZATION))
            .filter(header -> StringUtils.startsWith(header, "Basic "));
    }

}
