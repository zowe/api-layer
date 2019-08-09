/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.services.status;

import com.ca.mfaas.product.gateway.GatewayConfigProperties;
import com.ca.mfaas.apicatalog.metadata.EurekaMetadataParser;
import com.ca.mfaas.apicatalog.services.cached.model.ApiDocInfo;
import com.ca.mfaas.apicatalog.instance.InstanceRetrievalService;
import com.ca.mfaas.apicatalog.services.status.model.ApiDocNotFoundException;
import com.ca.mfaas.apicatalog.swagger.SubstituteSwaggerGenerator;
import com.ca.mfaas.product.service.ApiInfo;
import com.ca.mfaas.product.routing.RoutedService;
import com.ca.mfaas.product.routing.RoutedServices;
import com.netflix.appinfo.InstanceInfo;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;

/**
 * Retrieves the API documentation for a registered service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class
APIDocRetrievalService {

    private final RestTemplate restTemplate;
    private final InstanceRetrievalService instanceRetrievalService;
    private final GatewayConfigProperties gatewayConfigProperties;

    private final EurekaMetadataParser metadataParser = new EurekaMetadataParser();
    private final SubstituteSwaggerGenerator swaggerGenerator = new SubstituteSwaggerGenerator();

    /**
     * Retrieve the API docs for a registered service
     * <p>
     * API doc URL is taken from the application metadata in the following
     * order:
     * <p>
     * 1. 'apiml.apiInfo.swaggerUrl' (preferred way)
     * 2. 'apiml.apiInfo' is present and 'swaggerUrl' is not, ApiInfo info is automatically generated
     * 3. URL is constructed from 'routes.api-doc.serviceUrl'. This method is deprecated and used for
     * backwards compatibility only
     *
     * @param serviceId  the unique service id
     * @param apiVersion the version of the API
     * @return the API doc and related information for transformation
     * @throws ApiDocNotFoundException if the response is error
     */
    public ApiDocInfo retrieveApiDoc(@NonNull String serviceId, String apiVersion) {
        InstanceInfo instanceInfo = instanceRetrievalService.getInstanceInfo(serviceId);
        if (instanceInfo == null) {
            throw new ApiDocNotFoundException("Could not load instance information for service " + serviceId + " .");
        }

        List<ApiInfo> apiInfoList = metadataParser.parseApiInfo(instanceInfo.getMetadata());
        RoutedServices routes = metadataParser.parseRoutes(instanceInfo.getMetadata());

        ApiInfo apiInfo = findApi(apiInfoList, apiVersion);
        String apiDocUrl = getApiDocUrl(apiInfo, instanceInfo, routes);
        if (apiDocUrl == null) {
            return getApiDocInfoBySubstituteSwagger(instanceInfo, routes, apiInfo);
        }

        String apiDocContent = getApiDocContentByUrl(serviceId, apiDocUrl);
        return new ApiDocInfo(apiInfo, apiDocContent, routes);
    }


    /**
     * Get ApiInfo url
     *
     * @param apiInfo      the apiinfo of service instance
     * @param instanceInfo the information about service instance
     * @param routes       the routes of service instance
     * @return the url of apidoc
     */
    private String getApiDocUrl(ApiInfo apiInfo, InstanceInfo instanceInfo, RoutedServices routes) {
        String apiDocUrl = null;
        if (apiInfo == null) {
            apiDocUrl = createApiDocUrlFromRouting(instanceInfo, routes);
        } else if (apiInfo.getSwaggerUrl() != null) {
            apiDocUrl = apiInfo.getSwaggerUrl();
        }

        return apiDocUrl;
    }


    /**
     * Get ApiInfo content by Url
     *
     * @param serviceId the unique service id
     * @param apiDocUrl the url of apidoc
     * @return the information about APIDoc content as application/json
     * @throws ApiDocNotFoundException if the response is error
     */
    private String getApiDocContentByUrl(@NonNull String serviceId, String apiDocUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON_UTF8));

        ResponseEntity<String> response = restTemplate.exchange(
            apiDocUrl,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class);

        if (response.getStatusCode().isError()) {
            throw new ApiDocNotFoundException("No API Documentation was retrieved due to " + serviceId + " server error: '" + response.getBody() + "'.");
        }
        return response.getBody();
    }

    /**
     * Get ApiDocInfo by Substitute Swagger
     *
     * @param instanceInfo the information about service instance
     * @param routes       the routes of service instance
     * @param apiInfo      the apiinfo of service instance
     * @return the information about APIDocInfo
     */
    private ApiDocInfo getApiDocInfoBySubstituteSwagger(InstanceInfo instanceInfo,
                                                        RoutedServices routes,
                                                        ApiInfo apiInfo) {
        String response = swaggerGenerator.generateSubstituteSwaggerForService(
            instanceInfo,
            apiInfo,
            gatewayConfigProperties.getScheme(),
            gatewayConfigProperties.getHostname());
        return new ApiDocInfo(apiInfo, response, routes);
    }

    /**
     * Find ApiInfo for the corresponding version, if not found the first one is returned
     *
     * @param apiInfos   the list of APIs information
     * @param apiVersion the version to be find
     * @return the information about API
     */
    private ApiInfo findApi(List<ApiInfo> apiInfos, String apiVersion) {
        if (apiInfos.isEmpty()) {
            return null;
        }

        return apiInfos.stream()
            .filter(
                f -> f.getGatewayUrl().equals(apiVersion == null ? "api" : "api/" + apiVersion)
            )
            .findFirst()
            .orElse(apiInfos.get(0));
    }

    /**
     * Creates a URL from the routing metadata 'routes.api-doc.serviceUrl' when 'apiml.apiInfo.swaggerUrl' is
     * not present
     *
     * @param instanceInfo the information about service instance
     * @return the URL of API doc endpoint
     * @deprecated Added to support services which were on-boarded before 'apiml.apiInfo.swaggerUrl' parameter was
     * introduced. It will be removed when all services will be using the new configuration style.
     */
    @Deprecated
    private String createApiDocUrlFromRouting(InstanceInfo instanceInfo, RoutedServices routes) {
        String scheme;
        int port;
        if (instanceInfo.isPortEnabled(InstanceInfo.PortType.SECURE)) {
            scheme = "https";
            port = instanceInfo.getSecurePort();
        } else {
            scheme = "http";
            port = instanceInfo.getPort();
        }

        String path = null;
        RoutedService route = routes.findServiceByGatewayUrl("api/v1/api-doc");
        if (route != null) {
            path = route.getServiceUrl();
        }

        if (path == null) {
            throw new ApiDocNotFoundException("No API Documentation defined for service " + instanceInfo.getAppName().toLowerCase() + " .");
        }

        UriComponents uri = UriComponentsBuilder
            .newInstance()
            .scheme(scheme)
            .host(instanceInfo.getHostName())
            .port(port)
            .path(path)
            .build();

        return uri.toUriString();
    }
}
