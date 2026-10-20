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
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Applies the existing HSTS policy across security chains, including error responses.
 * In AT-TLS mode, AttlsHttpHandler verifies transport security before invoking WebFlux.
 */
@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class HstsResponseFilter implements WebFilter, Ordered {

    private final CustomHstsServerHttpHeadersWriter writer = new CustomHstsServerHttpHeadersWriter();
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
        if (exchange.getRequest().getSslInfo() != null || attlsEnabled) {
            exchange.getResponse().beforeCommit(() -> writer.writeHttpHeaders(exchange));
        }
        return chain.filter(exchange);
    }
}
