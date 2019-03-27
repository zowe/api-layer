/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.swagger;

import com.ca.mfaas.apicatalog.exceptions.ApiDocTransformationException;
import com.ca.mfaas.apicatalog.gateway.GatewayConfigProperties;
import com.ca.mfaas.apicatalog.services.cached.model.ApiDocInfo;
import com.ca.mfaas.product.constants.CoreService;
import com.ca.mfaas.product.model.ApiInfo;
import com.ca.mfaas.product.routing.RoutedService;
import com.ca.mfaas.product.routing.RoutedServices;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.models.ExternalDocs;
import io.swagger.models.Path;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;
import io.swagger.util.Json;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.validation.UnexpectedTypeException;

import java.io.IOException;
import java.util.*;

/**
 * Transforms API documentation to documentation relative to Gateway, not the service instance
 */
@Slf4j
@Service
public class TransformApiDocService {
    private static final String SWAGGER_LOCATION_LINK = "[Swagger/OpenAPI JSON Document]";
    private static final String EXTERNAL_DOCUMENTATION = "External documentation";
    private static final String HIDDEN_TAG = "apimlHidden";
    private static final String CATALOG_VERSION = "/api/v1";
    private static final String CATALOG_APIDOC_ENDPOINT = "/apidoc";
    private static final String HARDCODED_VERSION = "/v1";
    private static final String SEPARATOR = "/";

    private final GatewayConfigProperties gatewayConfigProperties;

    public TransformApiDocService(GatewayConfigProperties gatewayConfigProperties) {
        this.gatewayConfigProperties = gatewayConfigProperties;
    }

    /**
     * Does transformation API documentation
     *
     * @param serviceId  the unique service id
     * @param apiDocInfo the API doc and additional information about transformation
     * @return the transformed API documentation relative to Gateway
     */
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
                    route = apiDocInfo.getRoutes().getBestMatchingServiceUrl(endPoint, true);
                }

                String updatedShortEndPoint;
                String updatedLongEndPoint;
                if (route != null) {
                    prefixes.add(route.getGatewayUrl());
                    updatedShortEndPoint = getShortEndPoint(route.getServiceUrl(), endPoint);
                    updatedLongEndPoint = SEPARATOR + route.getGatewayUrl() + SEPARATOR + serviceId + updatedShortEndPoint;
                } else {
                    log.warn("Could not transform endpoint '{}' for service '{}'. Please check the service configuration.", endPoint, serviceId);
                    updatedShortEndPoint = endPoint;
                    updatedLongEndPoint = endPoint;
                }

                log.trace("Final Endpoint: " + updatedLongEndPoint);
                updatedShortPaths.put(updatedShortEndPoint, path);
                updatedLongPaths.put(updatedLongEndPoint, path);
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

    private String getShortEndPoint(String routeServiceUrl, String endPoint) {
        String shortEndPoint = endPoint;
        if (!routeServiceUrl.equals("/")) {
            shortEndPoint = shortEndPoint.replaceFirst(routeServiceUrl, "");
        }
        return shortEndPoint;
    }

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
