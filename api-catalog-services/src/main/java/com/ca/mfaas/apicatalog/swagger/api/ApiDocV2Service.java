/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.swagger.api;

import com.ca.mfaas.apicatalog.services.cached.model.ApiDocInfo;
import com.ca.mfaas.apicatalog.swagger.ApiDocTransformationException;
import com.ca.mfaas.config.ApiInfo;
import com.ca.mfaas.product.constants.CoreService;
import com.ca.mfaas.product.gateway.GatewayClient;
import com.ca.mfaas.product.gateway.GatewayConfigProperties;
import com.ca.mfaas.product.routing.RoutedService;
import com.ca.mfaas.product.routing.ServiceType;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.models.ExternalDocs;
import io.swagger.models.Path;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;
import io.swagger.util.Json;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import javax.validation.UnexpectedTypeException;
import java.io.IOException;
import java.util.*;

@RequiredArgsConstructor
@Slf4j
public class ApiDocV2Service {

    private final GatewayClient gatewayClient;

    private static final String SWAGGER_LOCATION_LINK = "[Swagger/OpenAPI JSON Document]";
    private static final String EXTERNAL_DOCUMENTATION = "External documentation";
    private static final String HIDDEN_TAG = "apimlHidden";
    private static final String CATALOG_VERSION = "/api/v1";
    private static final String CATALOG_APIDOC_ENDPOINT = "/apidoc";
    private static final String HARDCODED_VERSION = "/v1";
    private static final String SEPARATOR = "/";

    public String transformApiDoc(String serviceId, ApiDocInfo apiDocInfo) {
        Swagger swagger;

        try {
            swagger = Json.mapper().readValue(apiDocInfo.getApiDocContent(), Swagger.class);
        } catch (IOException e) {
            log.error("Could not convert response body to a Swagger object.", e);
            throw new UnexpectedTypeException("Response is not a Swagger type object.");
        }

        boolean hidden = swagger.getTag(HIDDEN_TAG) != null;

        updateSchemeHostAndLink(swagger, serviceId, hidden);
        updatePaths(swagger, serviceId, apiDocInfo, hidden);
        updateExternalDoc(swagger, apiDocInfo);

        try {
            return Json.mapper().writeValueAsString(swagger);
        } catch (JsonProcessingException e) {
            log.error("Could not convert Swagger to JSON", e);
            throw new ApiDocTransformationException("Could not convert Swagger to JSON");
        }
    }

    /**
     * Updates scheme and hostname, and adds API doc link to Swagger
     *
     * @param swagger   the API doc
     * @param serviceId the unique service id
     * @param hidden    do not add link for automatically generated API doc
     */
    private void updateSchemeHostAndLink(Swagger swagger, String serviceId, boolean hidden) {
        GatewayConfigProperties gatewayConfigProperties = gatewayClient.getGatewayConfigProperties();
        String link = gatewayConfigProperties.getScheme() + "://" + gatewayConfigProperties.getHostname() + CATALOG_VERSION + SEPARATOR + CoreService.API_CATALOG.getServiceId() +
            CATALOG_APIDOC_ENDPOINT + SEPARATOR + serviceId + HARDCODED_VERSION;
        String swaggerLink = "\n\n" + SWAGGER_LOCATION_LINK + "(" + link + ")";

        swagger.setSchemes(Collections.singletonList(Scheme.forValue(gatewayConfigProperties.getScheme())));
        swagger.setHost(gatewayConfigProperties.getHostname());
        if (!hidden) {
            swagger.getInfo().setDescription(swagger.getInfo().getDescription() + swaggerLink);
        }
    }

    /**
     * Updates BasePath and Paths in Swagger
     *
     * @param swagger    the API doc
     * @param serviceId  the unique service id
     * @param apiDocInfo the service information
     * @param hidden     do not set Paths for automatically generated API doc
     */
    private void updatePaths(Swagger swagger, String serviceId, ApiDocInfo apiDocInfo, boolean hidden) {
        Map<String, Path> updatedShortPaths = new HashMap<>();
        Map<String, Path> updatedLongPaths = new HashMap<>();
        Set<String> prefixes = new HashSet<>();

        if (swagger.getPaths() != null && !swagger.getPaths().isEmpty()) {
            swagger.getPaths().forEach((originalEndpoint, path) -> {
                log.trace("Swagger Service Id: " + serviceId);
                log.trace("Original Endpoint: " + originalEndpoint);
                log.trace("Base Path: " + swagger.getBasePath());

                // Retrieve route which matches endpoint
                String endPoint = getEndPoint(swagger.getBasePath(), originalEndpoint);
                RoutedService route = getRoutedServiceByApiInfo(apiDocInfo, endPoint);
                if (route == null) {
                    route = apiDocInfo.getRoutes().getBestMatchingServiceUrl(endPoint, ServiceType.API);
                }

                if (route == null) {
                    log.warn("Could not transform endpoint '{}' for service '{}'. Please check the service configuration.", endPoint, serviceId);
                } else {
                    prefixes.add(route.getGatewayUrl());
                }

                Pair<String, String> endPointPairs = getEndPointPairs(endPoint, serviceId, route);
                log.trace("Final Endpoint: " + endPointPairs.getRight());

                updatedShortPaths.put(endPointPairs.getLeft(), path);
                updatedLongPaths.put(endPointPairs.getRight(), path);
            });
        }

        Map<String, Path> updatedPaths;
        if (prefixes.size() == 1) {
            swagger.setBasePath(SEPARATOR + prefixes.iterator().next() + SEPARATOR + serviceId);
            updatedPaths = updatedShortPaths;
        } else {
            swagger.setBasePath("");
            updatedPaths = updatedLongPaths;
        }

        if (!hidden) {
            swagger.setPaths(updatedPaths);
        }
    }

    /**
     * Get EndpointPairs
     *
     * @param endPoint  the endpoint of method
     * @param serviceId the unique service id
     * @param route     the route
     * @return the endpoint pairs
     */
    private Pair<String, String> getEndPointPairs(String endPoint, String serviceId, RoutedService route) {
        if (route == null) {
            return Pair.of(endPoint, endPoint);
        } else {
            String updatedShortEndPoint = getShortEndPoint(route.getServiceUrl(), endPoint);
            String updatedLongEndPoint = SEPARATOR + route.getGatewayUrl() + SEPARATOR + serviceId + updatedShortEndPoint;

            return Pair.of(updatedShortEndPoint, updatedLongEndPoint);
        }
    }

    /**
     * Get RoutedService by APIInfo
     *
     * @param apiDocInfo the API doc and additional information about transformation
     * @param endPoint   the endpoint of method
     * @return the RoutedService
     */
    private RoutedService getRoutedServiceByApiInfo(ApiDocInfo apiDocInfo, String endPoint) {
        ApiInfo apiInfo = apiDocInfo.getApiInfo();
        if (apiInfo == null) {
            return null;
        } else {
            String gatewayUrl = apiInfo.getGatewayUrl();
            RoutedService route = apiDocInfo.getRoutes().findServiceByGatewayUrl(gatewayUrl);
            if (endPoint.toLowerCase().startsWith(route.getServiceUrl())) {
                return route;
            } else {
                return null;
            }
        }
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
            shortEndPoint = shortEndPoint.replaceFirst(routeServiceUrl, "");
        }
        return shortEndPoint;
    }

    /**
     * Get endpoint
     *
     * @param swaggerBasePath  swagger base path
     * @param originalEndpoint the endpoint of method
     * @return endpoint
     */
    private String getEndPoint(String swaggerBasePath, String originalEndpoint) {
        String endPoint = originalEndpoint;
        if (!swaggerBasePath.equals(SEPARATOR)) {
            endPoint = swaggerBasePath + endPoint;
        }
        return endPoint;
    }

    /**
     * Updates External documentation in Swagger
     *
     * @param swagger    the API doc
     * @param apiDocInfo the service information
     */
    private void updateExternalDoc(Swagger swagger, ApiDocInfo apiDocInfo) {
        if (apiDocInfo.getApiInfo() == null)
            return;

        String externalDoc = apiDocInfo.getApiInfo().getDocumentationUrl();

        if (externalDoc != null) {
            swagger.setExternalDocs(new ExternalDocs(EXTERNAL_DOCUMENTATION, externalDoc));
        }
    }
}
