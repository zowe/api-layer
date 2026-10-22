/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;

/** Applies the existing HSTS policy to secure responses across security chains, including errors. */
@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class HstsResponseFilter implements WebFilter, Ordered {

    private static final String HEADER_VALUE = "max-age=" + Duration.ofDays(365).getSeconds() + "; includeSubDomains";

    private final boolean attlsEnabled;

    public HstsResponseFilter(@Value("${server.attlsServer.enabled:false}") boolean attlsEnabled) {
        this.attlsEnabled = attlsEnabled;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (isSecureRequest(exchange.getRequest())) {
            exchange.getResponse().beforeCommit(() -> Mono.fromRunnable(() ->
                exchange.getResponse().getHeaders().set("Strict-Transport-Security", HEADER_VALUE)));
        }
        return chain.filter(exchange);
    }

    private boolean isSecureRequest(ServerHttpRequest request) {
        return attlsEnabled || request.getSslInfo() != null || "https".equalsIgnoreCase(request.getURI().getScheme());
    }
}
