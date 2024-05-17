/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.filters;

import org.apache.catalina.connector.RequestFacade;
import org.apache.commons.lang3.stream.Streams;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.AbstractServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class RequestAttributesProvider implements WebFilter, GlobalFilter, Ordered {

    private void copyAttributes(ServerWebExchange exchange) {
        AbstractServerHttpRequest request = (AbstractServerHttpRequest) exchange.getRequest();
        RequestFacade requestFacade = request.getNativeRequest();

        Streams.of(requestFacade.getAttributeNames())
                .filter(name -> !exchange.getAttributes().containsKey(name))
                .forEach(name -> exchange.getAttributes().put(name, requestFacade.getAttribute(name)));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        copyAttributes(exchange);
        return chain.filter(exchange);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        copyAttributes(exchange);
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

}
