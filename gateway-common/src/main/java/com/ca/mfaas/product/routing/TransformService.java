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

import java.net.MalformedURLException;
import java.net.URI;

@Slf4j
public class TransformService {

    private final GatewayConfigProperties gatewayConfigProperties;

    public TransformService(GatewayConfigProperties gatewayConfigProperties) {
        this.gatewayConfigProperties = gatewayConfigProperties;
    }

    public String transformURL(ServiceType type,
                               String serviceId,
                               String serviceUrl,
                               RoutedServices routes) throws MalformedURLException {
        URI serviceUri = URI.create(serviceUrl);

        RoutedService route = routes.getBestMatchingServiceUrl(serviceUri.getPath(), type);
        if (route == null) {
            log.warn("Not able to select route for url {} of the service {}. Original url used.",
                serviceUrl, serviceId);
            return serviceUrl;
        }

        String path = serviceUri.getPath().replace(route.getServiceUrl(), "");
        if (!path.isEmpty()) {
            if (path.charAt(0) != '/') {
                throw new MalformedURLException("The path " + serviceUri.getPath() + " of the service URL " + serviceUri + " is not valid.");
            }
        }
        return String.format("%s://%s/%s/%s%s",
            gatewayConfigProperties.getScheme(),
            gatewayConfigProperties.getHostname(),
            route.getGatewayUrl(),
            serviceId,
            path);
    }
}
