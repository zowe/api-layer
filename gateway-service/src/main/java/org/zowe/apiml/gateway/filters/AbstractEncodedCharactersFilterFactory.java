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

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractEncodedCharactersFilterFactory extends AbstractGatewayFilterFactory<Object> {

    protected abstract boolean shouldFilter(String uri);

    /**
     * Filters requests by checking for encoded characters in the URI.
     * If encoded characters are not allowed and found, returns a BAD_REQUEST response.
     * Otherwise, proceeds with the filter chain.
     *
     * @return GatewayFilter
     */
    @Override
    public GatewayFilter apply(Object routeId) {
        return ((exchange, chain) -> {
            String uri = exchange.getRequest().getURI().toString();

            if (!shouldFilter(uri)) {
                return chain.filter(exchange);
            }

            throw getException(uri);

        });
    }

    abstract RuntimeException getException(String uri);
}
