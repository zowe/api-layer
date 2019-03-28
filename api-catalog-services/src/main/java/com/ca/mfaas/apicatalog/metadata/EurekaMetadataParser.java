/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.metadata;

import com.ca.mfaas.product.model.ApiInfo;
import com.ca.mfaas.product.routing.RoutedService;
import com.ca.mfaas.product.routing.RoutedServices;
import com.ca.mfaas.product.utils.UrlUtils;

import java.util.*;
import java.util.Map.Entry;

public class EurekaMetadataParser {

    /**
     * Parse eureka metadata and construct ApiInfo with the values found
     *
     * @param eurekaMetadata the eureka metadata
     * @return ApiInfo list
     */
    public List<ApiInfo> parseApiInfo(Map<String, String> eurekaMetadata) {
        Map<String, ApiInfo> apiInfo = new HashMap<>();

        for (Entry<String, String> entry : eurekaMetadata.entrySet()) {
            String[] keys = entry.getKey().split("\\.");
            if (keys.length == 4)
                if (keys[0].equals("apiml") && keys[1].equals("apiInfo")) {
                    apiInfo.putIfAbsent(keys[2], new ApiInfo());
                    ApiInfo api = apiInfo.get(keys[2]);
                    switch (keys[3]) {
                        case "apiId":
                            api.setApiId(entry.getValue());
                            break;
                        case "gatewayUrl":
                            api.setGatewayUrl(entry.getValue());
                            break;
                        case "version":
                            api.setVersion(entry.getValue());
                            break;
                        case "swaggerUrl":
                            api.setSwaggerUrl(entry.getValue());
                            break;
                        case "documentationUrl":
                            api.setDocumentationUrl(entry.getValue());
                            break;
                    }
                }
        }

        if (apiInfo.size() == 0) {
            return null;
        } else {
            return new ArrayList<>(apiInfo.values());
        }
    }

    /**
     * Parse eureka metadata and add the routes found to the RoutedServices
     *
     * @param eurekaMetadata the eureka metadata
     * @return the RoutedServices
     */
    public RoutedServices parseRoutes(Map<String, String> eurekaMetadata) {
        RoutedServices routes = new RoutedServices();
        Map<String, String> routeMap = new HashMap<>();
        Map<String, String> orderedMetadata = new TreeMap<>(eurekaMetadata);

        for (Map.Entry<String, String> metadata : orderedMetadata.entrySet()) {
            String[] keys = metadata.getKey().split("\\.");
            if (keys.length == 3 && keys[0].equals(RoutedServices.ROUTED_SERVICES_PARAMETER)) {

                if (keys[2].equals(RoutedServices.GATEWAY_URL_PARAMETER)) {
                    String gatewayURL = UrlUtils.removeFirstAndLastSlash(metadata.getValue());
                    routeMap.put(keys[1], gatewayURL);
                }

                if (keys[2].equals(RoutedServices.SERVICE_URL_PARAMETER) && routeMap.containsKey(keys[1])) {
                    String serviceURL = UrlUtils.addFirstSlash(metadata.getValue());
                    routes.addRoutedService(new RoutedService(keys[1], routeMap.get(keys[1]), serviceURL));
                    routeMap.remove(keys[1]);
                }
            }
        }

        return routes;
    }
}
