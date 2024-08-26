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

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import org.zowe.apiml.gateway.controllers.GatewayExceptionHandler;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.apache.hc.core5.http.HttpStatus.SC_BAD_REQUEST;

/**
 * This filter checks if encoded slashes in the URI are allowed based on configuration.
 * If not allowed and encoded slashes are present, it returns a BAD_REQUEST response.
 */
@Component
@RequiredArgsConstructor
public class ForbidEncodedSlashesFilterFactory extends AbstractGatewayFilterFactory<Object> {

    private final GatewayExceptionHandler gatewayExceptionHandler;

    /**
     * Filters requests to check for encoded slashes in the URI.
     * If encoded slashes are not allowed and found, returns a BAD_REQUEST response.
     * Otherwise, proceeds with the filter chain.
     *
     * @return Allowed Encoded slashes filter.
     */
    @Override
    public GatewayFilter apply(Object routeId) {
        return ((exchange, chain) -> {
            String uri = exchange.getRequest().getURI().toString();
            String decodedUri = URLDecoder.decode(uri, StandardCharsets.UTF_8);

            if (uri.equals(decodedUri)) {
                return chain.filter(exchange);
            }

            // TODO: replace with throwing an exception
            return gatewayExceptionHandler.setBodyResponse(exchange, SC_BAD_REQUEST, "org.zowe.apiml.gateway.requestContainEncodedSlash", uri);
        });
    }

}
