/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.services.routing;

import java.util.HashMap;
import java.util.Map;

public class RoutedServices {

    public static final String ROUTED_SERVICES_PARAMETER = "routed-services";
    public static final String GATEWAY_URL_PARAMETER = "gateway-url";
    public static final String SERVICE_URL_PARAMETER = "service-url";

    private final Map<String, RoutedService> routedService = new HashMap<>();

    public void addRoutedService(RoutedService route) {
        routedService.put(route.getGatewayUrl(), route);
    }

    public RoutedService findServiceByGatewayUrl(String gatewayUrl) {
        return routedService.get(gatewayUrl);
    }

    public RoutedService findGatewayUrlThatMatchesServiceUrl(String serviceUrl, boolean partial) {
        String gatewayUrl = routedService.entrySet().stream()
            .filter(e -> {
                boolean found;
                if (partial) {
                    found = serviceUrl.contains(e.getValue().getServiceUrl());
                    if (!found) {
                        found = ("/" + e.getValue().getSubServiceId() + serviceUrl)
                            .contains(e.getValue().getServiceUrl());
                    }
                } else {
                    found = serviceUrl.equalsIgnoreCase(e.getValue().getServiceUrl());
                }
                return found;
            })
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);
        return gatewayUrl == null ? null : routedService.get(gatewayUrl);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (String gatewayUrl: routedService.keySet()) {
            builder.append(gatewayUrl);
            builder.append(" -> ");
            builder.append(routedService.get(gatewayUrl).toString());
            builder.append(", ");
        }
        if (routedService.size() > 0) {
            builder.setLength(builder.length() - 2); // Remove extra ", "
        }
        builder.append("]");
        return builder.toString();
    }
}
