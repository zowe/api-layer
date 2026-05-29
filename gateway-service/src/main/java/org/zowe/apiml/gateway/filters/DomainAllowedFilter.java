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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Filter to enforce domain allowlist
 */
@Component
@Slf4j
public class DomainAllowedFilter implements GlobalFilter, Ordered, InitializingBean {

    @Value("${apiml.security.allowedDomains:${apiml.service.hostname}}")
    private String allowedDomains;

    @Override
    public int getOrder() {
        return 1001; // TODO check the correct order
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var uri = exchange.getRequest().getURI(); // is this the routed URL?
        var host = uri.getHost();
        return chain.filter(exchange);
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }

}
