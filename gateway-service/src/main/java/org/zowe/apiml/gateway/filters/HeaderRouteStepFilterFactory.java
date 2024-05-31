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

import lombok.Data;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Service;

/**
 * This filter is responsible to update request header during the routing. The header contain a locator to which server
 * route. It could contain also multiple steps separated by /.
 *
 * In the case header contain multiple steps the filter remove just the first part, otherwise it remove header at all.
 *
 * Examples:
 *      "step1/step2/step3" > "step2/step3"
 *      "node"              > null (removed)
 */
@Service
public class HeaderRouteStepFilterFactory extends AbstractGatewayFilterFactory<HeaderRouteStepFilterFactory.Config>  {

    public HeaderRouteStepFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        String header = config.getHeader();
        return (exchange, chain) -> {
            if (exchange.getRequest().getHeaders().containsKey(header)) {
                exchange.mutate().request(request -> exchange.getRequest().mutate().headers(headers -> {
                    String headerValue = headers.getFirst(header);
                    int index = headerValue.indexOf("/");
                    if ((index >= 0) && (index + 1 < headerValue.length())) {
                        headers.set(header, headerValue.substring(index + 1));
                    } else {
                        headers.remove(header);
                    }
                }));
            }

            return chain.filter(exchange);
        };
    }

    @Data
    public static class Config {

        private String header;

    }

}
