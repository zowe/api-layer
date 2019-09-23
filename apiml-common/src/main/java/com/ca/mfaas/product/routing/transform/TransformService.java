/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package com.ca.mfaas.product.routing.transform;

import com.ca.mfaas.eurekaservice.client.util.StringUtils;
import com.ca.mfaas.product.gateway.GatewayClient;
import com.ca.mfaas.product.gateway.GatewayConfigProperties;
import com.ca.mfaas.product.routing.RoutedService;
import com.ca.mfaas.product.routing.RoutedServices;
import com.ca.mfaas.product.routing.ServiceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

/**
 * Class for producing service URL using Gateway hostname and service route
 */

@Slf4j
@RequiredArgsConstructor
public class TransformService {

    private static final String SEPARATOR = "/";

    private final GatewayClient gatewayClient;

    /**
     * Construct the URL using gateway hostname and route
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

        if (!gatewayClient.isInitialized()) {
            String message = "Gateway not found yet, transform service cannot perform the request";
            log.error(message);
            throw new URLTransformationException(message);
        }

        URI serviceUri = URI.create(serviceUrl);
        String serviceUriPath = serviceUri.getPath();
        if (serviceUriPath == null) {
            String message = String.format("The URI %s is not valid.", serviceUri);
            throw new URLTransformationException(message);
        }

        RoutedService route = routes.getBestMatchingServiceUrl(serviceUriPath, type);
        if (route == null) {
            String message = String.format("Not able to select route for url %s of the service %s. Original url used.", serviceUri, serviceId);
            throw new URLTransformationException(message);
        }


        if (serviceUri.getQuery() != null) {
            serviceUriPath += "?" + serviceUri.getQuery();
        }

        String endPoint = getShortEndPoint(route.getServiceUrl(), serviceUriPath);
        if (!endPoint.isEmpty() && !endPoint.startsWith("/")) {
            throw new URLTransformationException("The path " + serviceUri.getPath() + " of the service URL " + serviceUri + " is not valid.");
        }

        GatewayConfigProperties gatewayConfigProperties = gatewayClient.getGatewayConfigProperties();

        return String.format("%s://%s/%s/%s%s",
            gatewayConfigProperties.getScheme(),
            gatewayConfigProperties.getHostname(),
            route.getGatewayUrl(),
            serviceId,
            endPoint);
    }


    /**
     * Get short endpoint
     *
     * @param routeServiceUrl service url of route
     * @param endPoint        the endpoint of method
     * @return short endpoint
     */
    private String getShortEndPoint(String routeServiceUrl, String endPoint) {
        String shortEndPoint = endPoint;
        if (!routeServiceUrl.equals(SEPARATOR)) {
            shortEndPoint = shortEndPoint.replaceFirst(StringUtils.removeLastSlash(routeServiceUrl), "");
        }
        return shortEndPoint;
    }
}
