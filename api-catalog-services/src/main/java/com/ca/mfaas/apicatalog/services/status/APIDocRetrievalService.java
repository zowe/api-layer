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

import com.ca.mfaas.apicatalog.metadata.EurekaMetadataParser;
import com.ca.mfaas.apicatalog.services.cached.model.ApiDocInfo;
import com.ca.mfaas.apicatalog.services.initialisation.InstanceRetrievalService;
import com.ca.mfaas.apicatalog.services.status.model.ApiDocNotFoundException;
import com.ca.mfaas.apicatalog.swagger.SubstituteSwaggerGenerator;
import com.ca.mfaas.product.model.ApiInfo;
import com.ca.mfaas.product.routing.RoutedServices;
import com.netflix.appinfo.InstanceInfo;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Retrieves the API documentation for a registered service
 */
@Slf4j
@Service
public class APIDocRetrievalService {
    private final RestTemplate restTemplate;
    private final InstanceRetrievalService instanceRetrievalService;
    private final EurekaMetadataParser metadataParser = new EurekaMetadataParser();
    private final SubstituteSwaggerGenerator swaggerGenerator = new SubstituteSwaggerGenerator();

    @Autowired
    public APIDocRetrievalService(
        RestTemplate restTemplate, InstanceRetrievalService instanceRetrievalService) {
        this.restTemplate = restTemplate;
        this.instanceRetrievalService = instanceRetrievalService;
    }

    /**
     * Retrieve the API docs for a registered service
     * <p>
     * API doc URL is taken from the application metadata in the following
     * order:
     * <p>
     * 1. 'apiml.apiInfo.swaggerUrl' (preferred way)
     * 2. 'apiml.apiInfo' is present & 'swaggerUrl' is not, ApiDoc info is automatically generated
     * 3. URL is constructed from 'routed-services.api-doc.service-url'. This method is deprecated and used for
     * backwards compatibility only
     *
     * @param serviceId  the unique service id
     * @param apiVersion the version of the API
     * @return the API doc and related information for transformation
     */
    public ApiDocInfo retrieveApiDoc(@NonNull String serviceId, String apiVersion) {
        InstanceInfo instanceInfo = instanceRetrievalService.getInstanceInfo(serviceId);
        if (instanceInfo == null) {
            throw new ApiDocNotFoundException("Could not load instance information for service " + serviceId + " .");
        }

        List<ApiInfo> apiInfoList = metadataParser.parseApiInfo(instanceInfo.getMetadata());
        RoutedServices routes = metadataParser.parseRoutes(instanceInfo.getMetadata());

        ApiInfo apiInfo = findApi(apiInfoList, apiVersion);
        String apiDocUrl = getApiDocUrl(apiInfo, instanceInfo);
        if (apiDocUrl == null) {
            return getApiDocInfoBySubstituteSwagger(instanceInfo, routes, apiInfo);
        }

        String apiDocContent = getApiDocContentByUrl(serviceId, apiDocUrl);
        return new ApiDocInfo(
            apiInfo,
            apiDocContent,
            routes,
            instanceRetrievalService.getGatewayScheme(),
            instanceRetrievalService.getGatewayHostname());
    }

    private String getApiDocUrl(ApiInfo apiInfo, InstanceInfo instanceInfo) {
        String apiDocUrl = null;
        if (apiInfo == null) {
            apiDocUrl = createApiDocUrlFromRouting(instanceInfo);
        } else if (apiInfo.getSwaggerUrl() != null) {
            apiDocUrl = apiInfo.getSwaggerUrl();
        }

        return apiDocUrl;
    }

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

    private ApiDocInfo getApiDocInfoBySubstituteSwagger(InstanceInfo instanceInfo,
                                                        RoutedServices routes,
                                                        ApiInfo apiInfo) {
        String response = swaggerGenerator.generateSubstituteSwaggerForService(
            instanceInfo,
            apiInfo,
            instanceRetrievalService.getGatewayScheme(),
            instanceRetrievalService.getGatewayHostname());
        return new ApiDocInfo(
            apiInfo,
            response,
            routes,
            instanceRetrievalService.getGatewayScheme(),
            instanceRetrievalService.getGatewayHostname());
    }

    /**
     * Find ApiInfo for the corresponding version, if not found the first one is returned
     *
     * @param apiInfos   the list of APIs information
     * @param apiVersion the version to be find
     * @return the information about API
     */
    private ApiInfo findApi(List<ApiInfo> apiInfos, String apiVersion) {
        if (Objects.isNull(apiInfos)) {
            return null;
        }

        String expectedGatewayUrl = "api";
        if (apiVersion != null) {
            expectedGatewayUrl = "api/" + apiVersion;
        }

        for (ApiInfo api : apiInfos) {
            if (api.getGatewayUrl().equals(expectedGatewayUrl)) {
                return api;
            }
        }

        return apiInfos.get(0);
    }

    /**
     * Creates a URL from the routing metadata 'routed-services.api-doc.service-url' when 'apiml.apiInfo.swaggerUrl' is
     * not present
     *
     * @param instanceInfo the information about service instance
     * @return the URL of API doc endpoint
     * @deprecated Added to support services which were on-boarded before 'apiml.apiInfo.swaggerUrl' parameter was
     * introduced. It will be removed when all services will be using the new configuration style.
     */
    @Deprecated
    private String createApiDocUrlFromRouting(InstanceInfo instanceInfo) {
        String scheme;
        int port;
        if (instanceInfo.isPortEnabled(InstanceInfo.PortType.SECURE)) {
            scheme = "https";
            port = instanceInfo.getSecurePort();
        } else {
            scheme = "http";
            port = instanceInfo.getPort();
        }

        String path = instanceInfo.getMetadata().get("routed-services.api-doc.service-url");
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
