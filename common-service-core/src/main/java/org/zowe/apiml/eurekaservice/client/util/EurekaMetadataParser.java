/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.eurekaservice.client.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.BooleanUtils;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationSchemes;
import org.zowe.apiml.config.ApiInfo;
import org.zowe.apiml.config.CodeSnippet;
import org.zowe.apiml.exception.MetadataValidationException;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.message.yaml.YamlMessageServiceInstance;
import org.zowe.apiml.product.routing.RoutedService;
import org.zowe.apiml.product.routing.RoutedServices;
import org.zowe.apiml.util.UrlUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.zowe.apiml.constants.EurekaMetadataDefinition.*;

public class EurekaMetadataParser {
    private static final String THREE_STRING_MERGE_FORMAT = "%s.%s.%s";
    private static final String FIVE_STRING_MERGE_FORMAT = "%s.%s.%s.%s.%s";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ApimlLogger apimlLog = ApimlLogger.of(EurekaMetadataParser.class, YamlMessageServiceInstance.getInstance());
    private final AuthenticationSchemes schemes = new AuthenticationSchemes();

    /**
     * Parse eureka metadata and construct ApiInfo with the values found
     *
     * @param eurekaMetadata the eureka metadata
     * @return ApiInfo list
     */
    public List<ApiInfo> parseApiInfo(Map<String, String> eurekaMetadata) {
        Map<String, Map<String, Object>> collectedApiInfoEntries = new HashMap<>();
        eurekaMetadata.entrySet()
            .stream()
            .filter(metadata -> metadata.getKey().startsWith(API_INFO))
            .forEach(metadata -> {
                String[] keys = metadata.getKey().split("\\.");
                if (keys.length >= 4) { // at least 4 keys split by '.' if is an ApiInfo config entry
                    String entryIndex = keys[2];
                    String entryKey = keys[3];
                    collectedApiInfoEntries.putIfAbsent(entryIndex, new HashMap<>());
                    Map<String, Object> apiInfoEntries = collectedApiInfoEntries.get(entryIndex);

                    if (metadata.getKey().contains(CODE_SNIPPET) && keys.length >= 6) {
                        String codeSnippetEntryIndex = keys[4];
                        String codeSnippetChildKey = keys[5];

                        apiInfoEntries.putIfAbsent(entryKey, new HashMap<>());

                        @SuppressWarnings("unchecked")
                        Map<String, Map<String, String>> codeSnippetMap = (Map<String, Map<String, String>>) apiInfoEntries.get(entryKey);
                        codeSnippetMap.putIfAbsent(codeSnippetEntryIndex, new HashMap<>());

                        Map<String, String> codeSnippetChildEntry = codeSnippetMap.get(codeSnippetEntryIndex);
                        codeSnippetChildEntry.put(codeSnippetChildKey, metadata.getValue());
                        codeSnippetMap.put(codeSnippetEntryIndex, codeSnippetChildEntry);
                        apiInfoEntries.put(entryKey, codeSnippetMap);
                    } else {
                        apiInfoEntries.put(entryKey, metadata.getValue());
                    }
                    collectedApiInfoEntries.put(entryIndex, apiInfoEntries);
                }
            });

        List<ApiInfo> apiInfoList = new ArrayList<>();
        collectedApiInfoEntries.values().forEach(fields -> {
            try {
                if (fields.containsKey(CODE_SNIPPET)) {

                    @SuppressWarnings("unchecked")
                    Map<String, Map<String, String>> codeSnippetMap = (Map<String, Map<String, String>>) fields.get(CODE_SNIPPET);
                    List<Map<String, String>> codeSnippetList = new ArrayList<>(codeSnippetMap.values());

                    fields.put(CODE_SNIPPET, codeSnippetList);
                }
                apiInfoList.add(objectMapper.convertValue(fields, ApiInfo.class));
            } catch (Exception e) {
                apimlLog.log("org.zowe.apiml.common.apiInfoParsingError", fields);
            }
        });
        return apiInfoList;
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

    /**
     * Generate Eureka metadata for ApiInfo configuration
     *
     * @param serviceId the identifier of a service which ApiInfo configuration belongs
     * @param apiInfo   ApiInfo config data
     * @return the generated Eureka metadata
     */
    public static Map<String, String> generateMetadata(String serviceId, ApiInfo apiInfo) {
        Map<String, String> metadata = new HashMap<>();
        String encodedGatewayUrl = UrlUtils.getEncodedUrl(apiInfo.getGatewayUrl());

        if (apiInfo.getApiId() != null) {
            metadata.put(String.format(THREE_STRING_MERGE_FORMAT, API_INFO, encodedGatewayUrl, API_INFO_API_ID), apiInfo.getApiId());
        }

        if (apiInfo.getGatewayUrl() != null) {
            metadata.put(String.format(THREE_STRING_MERGE_FORMAT, API_INFO, encodedGatewayUrl, API_INFO_GATEWAY_URL), apiInfo.getGatewayUrl());
        }

        if (apiInfo.getVersion() != null) {
            metadata.put(String.format(THREE_STRING_MERGE_FORMAT, API_INFO, encodedGatewayUrl, API_INFO_VERSION), apiInfo.getVersion());
        }

        if (apiInfo.getSwaggerUrl() != null) {
            validateUrl(apiInfo.getSwaggerUrl(),
                () -> String.format("The Swagger URL \"%s\" for service %s is not valid", apiInfo.getSwaggerUrl(), serviceId)
            );

            metadata.put(String.format(THREE_STRING_MERGE_FORMAT, API_INFO, encodedGatewayUrl, API_INFO_SWAGGER_URL), apiInfo.getSwaggerUrl());
        }

        if (apiInfo.getDocumentationUrl() != null) {
            validateUrl(apiInfo.getDocumentationUrl(),
                () -> String.format("The documentation URL \"%s\" for service %s is not valid", apiInfo.getDocumentationUrl(), serviceId)
            );

            metadata.put(String.format(THREE_STRING_MERGE_FORMAT, API_INFO, encodedGatewayUrl, API_INFO_DOCUMENTATION_URL), apiInfo.getDocumentationUrl());
        }

        List<CodeSnippet> codeSnippets = apiInfo.getCodeSnippet();
        if (codeSnippets != null && !codeSnippets.isEmpty()) {
            for (int i = 0; i < codeSnippets.size(); i++) {
                metadata.put(String.format(FIVE_STRING_MERGE_FORMAT, API_INFO, encodedGatewayUrl, CODE_SNIPPET, i, CODE_SNIPPET_ENDPOINT), codeSnippets.get(i).getEndpoint());
                metadata.put(String.format(FIVE_STRING_MERGE_FORMAT, API_INFO, encodedGatewayUrl, CODE_SNIPPET, i, CODE_SNIPPET_CODE_BLOCK), codeSnippets.get(i).getCodeBlock());
                metadata.put(String.format(FIVE_STRING_MERGE_FORMAT, API_INFO, encodedGatewayUrl, CODE_SNIPPET, i, CODE_SNIPPET_LANGUAGE), codeSnippets.get(i).getLanguage());
            }
        }

        metadata.put(String.format(THREE_STRING_MERGE_FORMAT, API_INFO, encodedGatewayUrl, API_INFO_IS_DEFAULT), String.valueOf(apiInfo.isDefaultApi()));

        return metadata;
    }

    private static void validateUrl(String url, Supplier<String> exceptionSupplier) {
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            throw new MetadataValidationException(exceptionSupplier.get(), e);
        }
    }

    public Authentication parseAuthentication(Map<String, String> eurekaMetadata) {
        return Authentication.builder()
            .applid(eurekaMetadata.get(AUTHENTICATION_APPLID))
            .scheme(schemes.map(eurekaMetadata.get(AUTHENTICATION_SCHEME)))
            .headers(eurekaMetadata.get(AUTHENTICATION_HEADERS))
            .supportsSso(BooleanUtils.toBooleanObject(eurekaMetadata.get(AUTHENTICATION_SSO)))
            .build();
    }

}
