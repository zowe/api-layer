/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.eurekaservice.client.util;

import com.ca.mfaas.config.ApiInfo;
import com.ca.mfaas.product.routing.RoutedService;
import com.ca.mfaas.product.routing.RoutedServices;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

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

        eurekaMetadata.entrySet()
            .stream()
            .filter(metadata -> metadata.getKey().startsWith(API_INFO))
            .forEach(metadata -> {
                String[] keys = metadata.getKey().split("\\.");
                if (keys.length == 4) {
                    apiInfo.putIfAbsent(keys[2], new ApiInfo());
                    ApiInfo api = apiInfo.get(keys[2]);
                    switch (keys[3]) {
                        case API_INFO_API_ID:
                            api.setApiId(metadata.getValue());
                            break;
                        case API_INFO_GATEWAY_URL:
                            api.setGatewayUrl(metadata.getValue());
                            break;
                        case API_INFO_VERSION:
                            api.setVersion(metadata.getValue());
                            break;
                        case API_INFO_SWAGGER_URL:
                            api.setSwaggerUrl(metadata.getValue());
                            break;
                        case API_INFO_DOCUMENTATION_URL:
                            api.setDocumentationUrl(metadata.getValue());
                            break;
                        default:
                            log.warn("Invalid parameter in metadata: {}", metadata);
                            break;
                    }
                }
            });

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
        parseToListRoute(eurekaMetadata)
            .forEach(routes::addRoutedService);
        return routes;
    }


    /**
     * Parse eureka metadata and return list of routes
     *
     * @param eurekaMetadata the eureka metadata
     * @return list of all routes
     */
    public List<RoutedService> parseToListRoute(Map<String, String> eurekaMetadata) {
        Map<String, String> routeMap = new HashMap<>();

        return eurekaMetadata.entrySet()
            .stream()
            .filter(this::filterMetadata)
            .map(metadata -> mapMetadataToRoutedService(metadata, routeMap))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private boolean filterMetadata(Map.Entry<String, String> metadata) {
        return metadata.getKey().startsWith(ROUTES)
            && (metadata.getKey().endsWith(ROUTES_GATEWAY_URL) || metadata.getKey().endsWith(ROUTES_SERVICE_URL));
    }

    private RoutedService mapMetadataToRoutedService(Map.Entry<String, String> metadata,
                                                     Map<String, String> routeMap) {
        String routeKey = metadata.getKey();
        String routeURL = metadata.getValue();

        String[] routeKeys = routeKey.split("\\.");
        if (routeKeys.length != 4) {
            return null;
        }

        String subServiceId = routeKeys[2];
        String routeKeyURL = routeKeys[3];

        return processUrls(routeMap, routeKeyURL, subServiceId, routeURL);
    }

    private RoutedService processUrls(Map<String, String> routeMap,
                                      String routeKeyURL,
                                      String subServiceId,
                                      String routeURL) {
        if (routeKeyURL.equals(ROUTES_GATEWAY_URL)) {
            String gatewayURL = UrlUtils.removeFirstAndLastSlash(routeURL);

            if (routeMap.containsKey(subServiceId)) {
                String serviceUrl = routeMap.get(subServiceId);
                routeMap.remove(subServiceId);
                return new RoutedService(subServiceId, gatewayURL, serviceUrl);
            } else {
                routeMap.put(subServiceId, gatewayURL);
            }
        }

        if (routeKeyURL.equals(ROUTES_SERVICE_URL)) {
            String serviceURL = UrlUtils.addFirstSlash(routeURL);

            if (routeMap.containsKey(subServiceId)) {
                String gatewayUrl = routeMap.get(subServiceId);
                routeMap.remove(subServiceId);
                return new RoutedService(subServiceId, gatewayUrl, serviceURL);
            } else {
                routeMap.put(subServiceId, serviceURL);
            }
        }

        return null;
    }
}
