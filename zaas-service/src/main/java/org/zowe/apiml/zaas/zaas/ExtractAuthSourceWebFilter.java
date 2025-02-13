/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.zaas;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSource;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSourceService;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

@Component
@RequiredArgsConstructor
public class ExtractAuthSourceWebFilter implements WebFilter {

    public static final String AUTH_SOURCE_ATTR = "zaas.auth.source";
    public static final String AUTH_SOURCE_PARSED_ATTR = "zaas.auth.source.parsed";

    private final AuthSourceService authSourceService;

    @Override
    @Nonnull
    public Mono<Void> filter(ServerWebExchange exchange, @Nonnull WebFilterChain chain) {

        var authSource = authSourceService.getAuthSourceFromRequest(exchange.getRequest());
        if (authSource.isPresent()) {
            AuthSource.Parsed parsed = authSourceService.parse(authSource.get());

            exchange.getAttributes().put(AUTH_SOURCE_ATTR, authSource.get());
            exchange.getAttributes().put(AUTH_SOURCE_PARSED_ATTR, parsed);

            return chain.filter(exchange);

        } else {
            // TODO find better solution
            throw new InsufficientAuthenticationException("No authentication source found in the request.");
        }
    }

}
