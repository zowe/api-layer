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

import com.ca.mfaas.eurekaservice.model.ApiInfo;
import com.ca.mfaas.product.routing.RoutedService;
import com.ca.mfaas.product.routing.RoutedServices;
import com.ca.mfaas.product.utils.UrlUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.Map.Entry;

import static com.ca.mfaas.constants.EurekaMetadataDefinition.*;

@Slf4j
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
            if (keys.length == 3 && keys[0].equals(APIS)) {
                apiInfo.putIfAbsent(keys[1], new ApiInfo());
                ApiInfo api = apiInfo.get(keys[1]);
                switch (keys[2]) {
                    case APIS_API_ID:
                        api.setApiId(entry.getValue());
                        break;
                    case APIS_GATEWAY_URL:
                        api.setGatewayUrl(entry.getValue());
                        break;
                    case APIS_VERSION:
                        api.setVersion(entry.getValue());
                        break;
                    case APIS_SWAGGER_URL:
                        api.setSwaggerUrl(entry.getValue());
                        break;
                    case APIS_DOCUMENTATION_URL:
                        api.setDocumentationUrl(entry.getValue());
                        break;
                    default:
                        log.warn("Invalid parameter in metadata: {}", entry);
                        break;
                }
            }
        }

        return new ArrayList<>(apiInfo.values());
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
            if (keys.length == 3 && keys[0].equals(ROUTES)) {
                if (keys[2].equals(ROUTES_GATEWAY_URL)) {
                    String gatewayURL = UrlUtils.removeFirstAndLastSlash(metadata.getValue());
                    routeMap.put(keys[1], gatewayURL);
                }

                if (keys[2].equals(ROUTES_SERVICE_URL) && routeMap.containsKey(keys[1])) {
                    String serviceURL = UrlUtils.addFirstSlash(metadata.getValue());
                    routes.addRoutedService(new RoutedService(keys[1], routeMap.get(keys[1]), serviceURL));
                    routeMap.remove(keys[1]);
                }
            }
        }

        return routes;
    }
}
