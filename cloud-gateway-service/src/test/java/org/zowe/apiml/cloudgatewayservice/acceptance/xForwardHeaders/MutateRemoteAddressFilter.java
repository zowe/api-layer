/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.acceptance.xForwardHeaders;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;

import java.net.InetSocketAddress;

@Configuration
public class MutateRemoteAddressFilter {

    @Value("${test.proxyAddress}")
    public String proxyAddress;

    // A helper filter for tests only that will replace the remote address host in the request so it will appear to come from a remote proxy.
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    WebFilter mutateWebExchangeAddress() {
        return (exchange, chain) -> {
            ServerWebExchange exchangeFromProxy = exchange.mutate().request(
                exchange.getRequest().mutate()
                    .remoteAddress(new InetSocketAddress(proxyAddress, 0))
                    .build()
            ).build();
            return chain.filter(exchangeFromProxy);
        };
    }
}
