/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Removes the configured request header before the request is routed to the southbound service,
 * <b>unless</b> the request is a cross-site browser request (i.e. {@code Sec-Fetch-Site: cross-site}).
 * <p>
 * The Gateway strips CORS request headers (notably {@code Origin}) so that it remains the sole CORS
 * terminator and southbound services do not perform their own CORS processing. Only cross-site requests
 * carry a CSRF risk that a southbound service may want to inspect, so for those the header is preserved,
 * allowing the service to examine {@code Origin} together with {@code Sec-Fetch-Site} and make its own
 * decision. Every other request (same-origin, same-site, none, or non-browser without the header) keeps
 * the previous behavior of having the header removed.
 */
@Component
public class RemoveRequestHeaderIfNotCrossSiteGatewayFilterFactory
    extends AbstractGatewayFilterFactory<RemoveRequestHeaderIfNotCrossSiteGatewayFilterFactory.Config> {

    static final String SEC_FETCH_SITE_HEADER = "Sec-Fetch-Site";
    static final String CROSS_SITE = "cross-site";

    @Value("${apiml.security.csrf.preserveOriginForCrossSite:true}")
    private boolean preserveOrigin;

    public RemoveRequestHeaderIfNotCrossSiteGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return List.of("name");
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            if (preserveOrigin && CROSS_SITE.equals(request.getHeaders().getFirst(SEC_FETCH_SITE_HEADER))) {
                // Cross-site browser request: keep the header so the southbound service can inspect it.
                return chain.filter(exchange);
            }

            ServerHttpRequest mutatedRequest = request.mutate()
                .headers(headers -> headers.remove(config.getName()))
                .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    @Getter
    @Setter
    public static class Config {
        private String name;
    }

}
