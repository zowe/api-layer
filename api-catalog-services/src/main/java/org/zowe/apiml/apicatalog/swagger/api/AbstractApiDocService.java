/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.swagger.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.zowe.apiml.apicatalog.services.cached.model.ApiDocInfo;
import org.zowe.apiml.config.ApiInfo;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.product.routing.RoutedService;
import org.zowe.apiml.product.routing.RoutedServices;
import org.zowe.apiml.product.routing.ServiceType;

import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
public abstract class AbstractApiDocService<T, N> {

    @Value("${apiml.catalog.standalone.enabled:false}")
    protected boolean standalone;

    protected final GatewayClient gatewayClient;

    protected static final String EXTERNAL_DOCUMENTATION = "External documentation";
    protected static final String HIDDEN_TAG = "apimlHidden";

    public abstract String transformApiDoc(String serviceId, ApiDocInfo apiDocInfo);

    protected abstract void updatePaths(T swaggerAPI, String serviceId, ApiDocInfo apiDocInfo, boolean hidden);

    protected abstract void updateExternalDoc(T swaggerAPI, ApiDocInfo apiDocInfo);

    protected String getHostname(String serviceId) {
        String hostname = gatewayClient.getGatewayConfigProperties().getHostname();
        if (!standalone) return hostname;

        StringBuilder sb = new StringBuilder();
        sb.append(hostname);
        if (!hostname.endsWith("/")) sb.append('/');
        sb.append(serviceId);
        return sb.toString();
    }

    protected void preparePath(N path, ApiDocPath<N> apiDocPath, ApiDocInfo apiDocInfo, String basePath, String originalEndpoint, String serviceId) {
        log.trace("Swagger Service Id: " + serviceId);
        log.trace("Original Endpoint: " + originalEndpoint);
        log.trace("Base Path: " + basePath);

        // Retrieve route which matches endpoint
        String endPoint = getEndPoint(basePath, originalEndpoint);
        RoutedService route = getRoutedServiceByApiInfo(apiDocInfo, endPoint);
        if (route == null) {
            route = apiDocInfo.getRoutes().getBestMatchingServiceUrl(endPoint, ServiceType.API);
        }

        if (route == null) {
            log.debug("Could not transform endpoint '{}' for service '{}'. Please check the service configuration.", endPoint, serviceId);
        } else {
            apiDocPath.addPrefix(route.getGatewayUrl());
        }

        Pair<String, String> endPointPairs = getEndPointPairs(endPoint, serviceId, route);
        log.trace("Final Endpoint: " + endPointPairs.getRight());

        apiDocPath.addShortPath(endPointPairs.getLeft(), path);
        apiDocPath.addLongPath(endPointPairs.getRight(), path);
    }

    /**
     * Get endpoint
     *
     * @param swaggerBasePath  swagger base path
     * @param originalEndpoint the endpoint of method
     * @return endpoint
     */
    protected String getEndPoint(String swaggerBasePath, String originalEndpoint) {
        if (swaggerBasePath != null && !swaggerBasePath.equals(OpenApiUtil.SEPARATOR)) {
            String newEndpoint = swaggerBasePath + originalEndpoint;
            // handles case where base path ends in '/' and originalEndpoint starts with '/'
            return newEndpoint.replace("//", "/");
        }
        return originalEndpoint;
    }

    /**
     * Get EndpointPairs
     *
     * @param endPoint  the endpoint of method
     * @param serviceId the unique service id
     * @param route     the route
     * @return the endpoint pairs
     */
    protected Pair<String, String> getEndPointPairs(String endPoint, String serviceId, RoutedService route) {
        if (route == null) {
            return Pair.of(endPoint, endPoint);
        } else {
            String updatedShortEndPoint = getShortEndPoint(route.getServiceUrl(), endPoint);
            String updatedLongEndPoint = OpenApiUtil.SEPARATOR + serviceId + OpenApiUtil.SEPARATOR + route.getGatewayUrl() + updatedShortEndPoint;

            return Pair.of(updatedShortEndPoint, updatedLongEndPoint);
        }
    }

    /**
     * Get short endpoint
     *
     * @param routeServiceUrl service url of route
     * @param endPoint        the endpoint of method
     * @return short endpoint
     */
    protected String getShortEndPoint(String routeServiceUrl, String endPoint) {
        String shortEndPoint = endPoint;
        if (!routeServiceUrl.equals(OpenApiUtil.SEPARATOR)) {
            shortEndPoint = shortEndPoint.replaceFirst(routeServiceUrl, "");
        }
        return shortEndPoint;
    }

    protected boolean isDefinedOnlyBypassRoutes(ApiDocInfo apiDocInfo) {
        return Optional.ofNullable(apiDocInfo)
            .map(ApiDocInfo::getRoutes)
            .map(RoutedServices::isDefinedOnlyBypassRoutes)
            .orElse(true);
    }

    /**
     * Get RoutedService by APIInfo
     *
     * @param apiDocInfo the API doc and additional information about transformation
     * @param endPoint   the endpoint of method
     * @return the RoutedService
     */
    protected RoutedService getRoutedServiceByApiInfo(ApiDocInfo apiDocInfo, String endPoint) {
        ApiInfo apiInfo = apiDocInfo.getApiInfo();
        if (apiInfo == null) {
            return null;
        } else {
            String gatewayUrl = apiInfo.getGatewayUrl();
            RoutedService route = apiDocInfo.getRoutes().findServiceByGatewayUrl(gatewayUrl);
            if ((route != null) && endPoint.toLowerCase().startsWith(route.getServiceUrl())) {
                return route;
            } else {
                return null;
            }
        }
    }
}
