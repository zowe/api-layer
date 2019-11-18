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
import com.ca.mfaas.product.gateway.GatewayClient;
import com.ca.mfaas.product.gateway.GatewayConfigProperties;
import com.ca.mfaas.product.routing.RoutedService;
import com.ca.mfaas.product.routing.ServiceType;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import javax.validation.UnexpectedTypeException;
import java.io.IOException;
import java.net.URI;
import java.util.*;

@RequiredArgsConstructor
@Slf4j
public class ApiDocV3Service {

    private final GatewayClient gatewayClient;

    private static final String HIDDEN_TAG = "apimlHidden";
    private static final String SEPARATOR = "/";

    public String transformApiDoc(String serviceId, ApiDocInfo apiDocInfo) {
        OpenAPI openAPI;

        try {
            openAPI = Json.mapper().readValue(apiDocInfo.getApiDocContent(), OpenAPI.class);
        } catch (IOException e) {
            log.error("Could not convert response body to an OpenAPI object. {}",serviceId, e);
            throw new UnexpectedTypeException("Response is not an OpenAPI type object.");
        }

        boolean hidden = isHidden(openAPI.getTags());

        updatePaths(openAPI, serviceId, apiDocInfo, hidden);
        updateServer(openAPI, serviceId, hidden);
        try {
            return Json.mapper().writeValueAsString(openAPI);
        } catch (JsonProcessingException e) {
            log.error("Could not convert Swagger to JSON", e);
            throw new ApiDocTransformationException("Could not convert Swagger to JSON");
        }
    }

    private void updateServer(OpenAPI openAPI, String serviceId, boolean hidden) {
        GatewayConfigProperties gatewayConfigProperties = gatewayClient.getGatewayConfigProperties();
        String swaggerLink = OpenApiUtil.getOpenApiLink(serviceId, gatewayConfigProperties);

        if (openAPI.getServers() != null) {
            openAPI.getServers()
                .forEach(server -> server.setUrl(
                    String.format("%s://%s/%s", gatewayConfigProperties.getScheme(), gatewayConfigProperties.getHostname(), server.getUrl())));
        }
        if (!hidden) {
            openAPI.getInfo().setDescription(openAPI.getInfo().getDescription() + swaggerLink);
        }
    }

    private void updatePaths(OpenAPI openAPI, String serviceId, ApiDocInfo apiDocInfo, boolean hidden) {
        Map<String, PathItem> updatedShortPaths = new HashMap<>();
        Map<String, PathItem> updatedLongPaths = new HashMap<>();
        Set<String> prefixes = new HashSet<>();

        if (openAPI.getPaths() != null && !openAPI.getPaths().isEmpty()) {
            openAPI.getPaths().forEach((originalEndpoint, path) -> {
                log.trace("Swagger Service Id: " + serviceId);
                log.trace("Original Endpoint: " + originalEndpoint);
                log.trace("Base Path: " + openAPI.getServers());

                RoutedService route = getRoutedServiceByApiInfo(apiDocInfo);
                if (route != null) {
                    boolean isMatch = isBasePathMatchesWithRouting(openAPI.getServers(), route);
                    if (!isMatch) {
                        route = null;
                    }
                }
                String basePath = getBasePath(route);
                // Retrieve route which matches endpoint
                String endPoint = getEndPoint(basePath, originalEndpoint);
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

        Map<String, PathItem> updatedPaths;
        if (prefixes.size() == 1) {
            setServerUrl(openAPI,SEPARATOR + prefixes.iterator().next() + SEPARATOR + serviceId);
            updatedPaths = updatedShortPaths;
        } else {
            setServerUrl(openAPI,"");
            updatedPaths = updatedLongPaths;
        }

        if (!hidden) {
            Paths paths = new Paths();
            updatedPaths.keySet().forEach(pathName -> paths.addPathItem(pathName, updatedPaths.get(pathName)));
            openAPI.setPaths(paths);
        }
    }

    private String getBasePath(RoutedService route) {
        if (route == null) {
            return "";
        }
        return route.getServiceUrl();
    }

    private void setServerUrl(OpenAPI openAPI, String basePath) {
        openAPI.addServersItem(new Server().url(basePath));
    }

    private boolean isBasePathMatchesWithRouting(List<Server> servers,
                                          RoutedService routedService) {
        if (servers.isEmpty()) {
            return false;
        } else {
            return servers.stream()
                .map(Server::getUrl)
                .map(this::getBasePath)
                .anyMatch(path -> path != null && path.startsWith(routedService.getServiceUrl()));
        }
    }

    private String getBasePath(String serverUrl) {
        try {
            URI uri = new URI(serverUrl);

            return uri.getPath();
        } catch (Exception e) {
            log.error("serverUrl is not parsable");
        }

        return null;
    }

    private String getEndPoint(String swaggerBasePath, String originalEndpoint) {
        String endPoint = originalEndpoint;
        if (!swaggerBasePath.equals(SEPARATOR)) {
            endPoint = swaggerBasePath + endPoint;
        }
        return endPoint;
    }

    private RoutedService getRoutedServiceByApiInfo(ApiDocInfo apiDocInfo) {
        ApiInfo apiInfo = apiDocInfo.getApiInfo();
        if (apiInfo == null) {
            return null;
        } else {
            String gatewayUrl = apiInfo.getGatewayUrl();
            return apiDocInfo.getRoutes().findServiceByGatewayUrl(gatewayUrl);
        }
    }

    private Pair<String, String> getEndPointPairs(String endPoint, String serviceId, RoutedService route) {
        if (route == null) {
            return Pair.of(endPoint, endPoint);
        } else {
            String updatedShortEndPoint = getShortEndPoint(route.getServiceUrl(), endPoint);
            String updatedLongEndPoint = SEPARATOR + route.getGatewayUrl() + SEPARATOR + serviceId + updatedShortEndPoint;

            return Pair.of(updatedShortEndPoint, updatedLongEndPoint);
        }
    }

    private String getShortEndPoint(String routeServiceUrl, String endPoint) {
        String shortEndPoint = endPoint;
        if (!routeServiceUrl.equals(SEPARATOR)) {
            shortEndPoint = shortEndPoint.replaceFirst(routeServiceUrl, "");
        }
        return shortEndPoint;
    }

    private boolean isHidden(List<Tag> tags) {
        return tags.stream().anyMatch(tag -> tag.getName().equals(HIDDEN_TAG));
    }
}
