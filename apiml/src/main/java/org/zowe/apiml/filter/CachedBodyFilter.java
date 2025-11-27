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
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * This filter caches the body if present in exchange request attribute and it makes the body readable for downstream controllers
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class CachedBodyFilter implements WebFilter {

    /**
     * Cached request body for WebFilters
     */
    public static final String CACHED_BODY_ATTR = "CACHED_REQUEST_BODY";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return DataBufferUtils.join(exchange.getRequest().getBody())
            .flatMap(dataBuffer -> {
                var bytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(bytes);
                DataBufferUtils.release(dataBuffer);
                exchange.getAttributes().put(CACHED_BODY_ATTR, new String(bytes, StandardCharsets.UTF_8));
                var decoratedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
                    @Override
                    public Flux<DataBuffer> getBody() {
                        return Flux.just(exchange.getResponse().bufferFactory().wrap(bytes));
                    }
                };
                return chain.filter(exchange.mutate().request(decoratedRequest).build());
            })
            .switchIfEmpty(chain.filter(exchange.mutate().request(new ServerHttpRequestDecorator(exchange.getRequest()) {
                @Override
                    public Flux<DataBuffer> getBody() {
                        return Flux.just(exchange.getResponse().bufferFactory().wrap(new byte[]{}));
                    }
            }).build()));
    }

}
