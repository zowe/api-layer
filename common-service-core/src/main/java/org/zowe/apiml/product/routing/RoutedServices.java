/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.product.routing;

import org.zowe.apiml.util.UrlUtils;

import java.util.HashMap;
import java.util.Map;

public class RoutedServices {
    private final Map<String, RoutedService> routedService = new HashMap<>();

    /**
     * Add route to the service
     *
     * @param route the route
     */
    public void addRoutedService(RoutedService route) {
        routedService.put(route.getGatewayUrl(), route);
    }

    /**
     * Find RoutedService by Gateway Url
     *
     * @param gatewayUrl the url of gateway
     * @return the route
     */
    public RoutedService findServiceByGatewayUrl(String gatewayUrl) {
        return routedService.get(gatewayUrl);
    }

    /**
     * Get best matching service url
     *
     * @param serviceUrl service url
     * @param type       service type
     * @return the route
     */
    public RoutedService getBestMatchingServiceUrl(String serviceUrl, ServiceType type) {
        RoutedService result = null;
        int maxSize = 0;

        for (Map.Entry<String, RoutedService> serviceEntry : routedService.entrySet()) {
            if (isServiceTypeMatch(serviceEntry, type)) {
                RoutedService value = serviceEntry.getValue();
                int size = value.getServiceUrl().length();
                //Remove last slash for service url
                String routeServiceUrl = UrlUtils.removeLastSlash(value.getServiceUrl().toLowerCase());
                if (size > maxSize && serviceUrl.toLowerCase().startsWith(routeServiceUrl)) {
                    result = value;
                    maxSize = size;
                }
            }
        }

        return result;
    }

    /**
     * Get best matching api url
     *
     * @param serviceUrl service url
     * @return the route
     */
    public RoutedService getBestMatchingApiUrl(String serviceUrl) {
        RoutedService result = null;
        int maxSize = 0;

        for (Map.Entry<String, RoutedService> serviceEntry : routedService.entrySet()) {
            if (isServiceTypeMatch(serviceEntry, ServiceType.API)) {
                RoutedService value = serviceEntry.getValue();
                int size = value.getServiceUrl().length();
                //Remove last slash for service url
                String routeServiceUrl = UrlUtils.removeLastSlash(value.getServiceUrl().toLowerCase());
                if (size > maxSize && isMatchingApiRoute(serviceUrl, routeServiceUrl)) {
                    result = value;
                    maxSize = size;
                }
            }
        }

        return result;
    }

    private boolean isServiceTypeMatch(Map.Entry<String, RoutedService> serviceEntry, ServiceType type) {
        String serviceEntryKey = serviceEntry.getKey().toLowerCase();
        String typeName = type.name().toLowerCase();
        return type.equals(ServiceType.ALL) || serviceEntryKey.startsWith(typeName);
    }

    private boolean isMatchingApiRoute(String serviceUrl, String routeServiceUrl) {
        serviceUrl = serviceUrl.toLowerCase();
        return serviceUrl.startsWith(routeServiceUrl)
            || routeServiceUrl.startsWith(serviceUrl); // Allow serviceUrl of /serviceId to map to /serviceId/{type}/{version} NOSONAR
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (Map.Entry<String, RoutedService> route : routedService.entrySet()) {
            builder.append(route.getKey());
            builder.append(" -> ");
            builder.append(route.toString());
            builder.append(", ");
        }
        if (routedService.size() > 0) {
            builder.setLength(builder.length() - 2); // Remove extra ", "
        }
        builder.append("]");
        return builder.toString();
    }
}
