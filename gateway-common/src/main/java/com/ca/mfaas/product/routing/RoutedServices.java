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

import java.util.HashMap;
import java.util.Map;

public class RoutedServices {
    public static final String ROUTED_SERVICES_PARAMETER = "routed-services";
    public static final String GATEWAY_URL_PARAMETER = "gateway-url";
    public static final String SERVICE_URL_PARAMETER = "service-url";

    private static final String UI_PREFIX = "ui";

    private final Map<String, RoutedService> routedService = new HashMap<>();

    public void addRoutedService(RoutedService route) {
        routedService.put(route.getGatewayUrl(), route);
    }

    public RoutedService findServiceByGatewayUrl(String gatewayUrl) {
        return routedService.get(gatewayUrl);
    }

    //TODO: Use field in API DOC
    public RoutedService getBestMatchingServiceUrl(String serviceUrl, boolean apiOnly) {
        RoutedService result = null;
        int maxSize = 0;

        for (Map.Entry<String, RoutedService> route : routedService.entrySet()) {
            int size = route.getValue().getServiceUrl().length();

            if (apiOnly && route.getKey().toLowerCase().startsWith(UI_PREFIX)) {
                continue;
            }

            if (size > maxSize &&
                serviceUrl.toLowerCase().startsWith(route.getValue().getServiceUrl().toLowerCase())) {
                result = route.getValue();
                maxSize = size;
            }
        }

        return result;
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
