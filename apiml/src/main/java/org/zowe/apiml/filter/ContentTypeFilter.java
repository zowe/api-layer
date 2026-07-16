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

import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Rejects requests that carry a body with HTTP 415 unless they declare a {@code Content-Type}
 * compatible with {@code application/json}. Bodyless requests (e.g. cert or Basic-Auth login,
 * logout) pass through unchecked, since they have nothing to be misinterpreted as JSON.
 * <p>
 * Must run after {@link CachedBodyFilter}: whether a body was actually sent is determined from
 * {@link CachedBodyFilter#CACHED_BODY_ATTR} rather than the {@code Content-Length} header, which
 * is absent for chunked/proxied requests.
 */
public class ContentTypeFilter implements WebFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        boolean hasBody = exchange.getAttribute(CachedBodyFilter.CACHED_BODY_ATTR) != null;
        if (hasBody) {
            MediaType contentType = exchange.getRequest().getHeaders().getContentType();
            if (contentType == null || !contentType.isCompatibleWith(MediaType.APPLICATION_JSON)) {
                exchange.getResponse().setStatusCode(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
                return exchange.getResponse().setComplete();
            }
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

}
