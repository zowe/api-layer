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

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/** Applies the existing HSTS policy to secure responses across security chains, including errors. */
@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class HstsResponseFilter implements WebFilter, Ordered {

    private final CustomHstsServerHttpHeadersWriter writer = new CustomHstsServerHttpHeadersWriter();

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (isSecureTransport(exchange.getRequest())) {
            exchange.getResponse().beforeCommit(() -> writer.writeHttpHeaders(exchange));
        }
        return chain.filter(exchange);
    }

    private boolean isSecureTransport(ServerHttpRequest request) {
        if (request.getSslInfo() != null) {
            return true;
        }
        Object nativeRequest;
        try {
            nativeRequest = ServerHttpRequestDecorator.getNativeRequest(request);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return false;
        }
        // Tomcat's WebFlux adapter may omit SslInfo when TLS has no client certificate.
        if (nativeRequest instanceof HttpServletRequest servletRequest) {
            Object sessionId = servletRequest.getAttribute("jakarta.servlet.request.ssl_session_id");
            return sessionId instanceof String id && !id.isEmpty();
        }
        return false;
    }
}
