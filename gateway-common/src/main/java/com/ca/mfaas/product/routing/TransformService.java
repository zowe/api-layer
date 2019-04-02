/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package com.ca.mfaas.product.routing;

import com.ca.mfaas.product.gateway.GatewayConfigProperties;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

@Slf4j
public class TransformService {

    private final GatewayConfigProperties gatewayConfigProperties;

    public TransformService(GatewayConfigProperties gatewayConfigProperties) {
        this.gatewayConfigProperties = gatewayConfigProperties;
    }

    /**
     * Construct the URL using gateway hostname and royte
     *
     * @param type       the type of the route
     * @param serviceId  the service id
     * @param serviceUrl the service URL
     * @param routes     the routes
     * @return the new URL
     * @throws URLTransformationException if the path of the service URL is not valid
     */
    public String transformURL(ServiceType type,
                               String serviceId,
                               String serviceUrl,
                               RoutedServices routes) throws URLTransformationException {
        URI serviceUri = URI.create(serviceUrl);

        RoutedService route = routes.getBestMatchingServiceUrl(serviceUri.getPath(), type);
        if (route == null) {
            String message = String.format("Not able to select route for url %s of the service %s. Original url used.", serviceUri, serviceId);
            log.warn(message);
            throw new URLTransformationException(message);
        }

        String path = serviceUri.getPath().replace(route.getServiceUrl(), "");
        if (!path.isEmpty() && !path.startsWith("/")) {
            throw new URLTransformationException("The path " + serviceUri.getPath() + " of the service URL " + serviceUri + " is not valid.");
        }

        return String.format("%s://%s/%s/%s%s",
            gatewayConfigProperties.getScheme(),
            gatewayConfigProperties.getHostname(),
            route.getGatewayUrl(),
            serviceId,
            path);
    }
}
