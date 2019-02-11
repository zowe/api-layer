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
import com.ca.mfaas.product.constants.CoreService;
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

@Slf4j
@Service
public class APIDocRetrievalService {
    private final RestTemplate restTemplate;
    private final InstanceRetrievalService instanceRetrievalService;
    private final EurekaMetadataParser metadataParser = new EurekaMetadataParser();
    private final SubstituteSwaggerGenerator swaggerGenerator = new SubstituteSwaggerGenerator();

    @Autowired
    public APIDocRetrievalService(RestTemplate restTemplate, InstanceRetrievalService instanceRetrievalService) {
        this.restTemplate = restTemplate;
        this.instanceRetrievalService = instanceRetrievalService;
    }

    /**
     * Retrieve the API docs for a registered service (all versions)
     *
     * @param serviceId  the unique service id
     * @param apiVersion the version of the API
     * @return the api docs as a string
     */
    public ApiDocInfo retrieveApiDoc(@NonNull String serviceId, String apiVersion) {
        String apiDocUrl;
        InstanceInfo instanceInfo = instanceRetrievalService.getInstanceInfo(serviceId);
        InstanceInfo gateway = instanceRetrievalService.getInstanceInfo(CoreService.GATEWAY.getServiceId());

        if (instanceInfo == null) {
            throw new ApiDocNotFoundException("Could not load instance information for service " + serviceId + " .");
        }

        List<ApiInfo> apiInfoList = metadataParser.parseApiInfo(instanceInfo.getMetadata());
        RoutedServices routes = metadataParser.parseRoutes(instanceInfo.getMetadata());

        ApiInfo apiInfo = null;
        if (apiInfoList != null) {
            apiInfo = findApi(apiInfoList, apiVersion);
            if (apiInfo != null && apiInfo.getSwaggerUrl() != null) {
                apiDocUrl = apiInfo.getSwaggerUrl();
            } else {
                if (gateway != null) {
                    ResponseEntity<String> response = swaggerGenerator.generateSubstituteSwaggerForService(gateway, instanceInfo, apiInfo);
                    return new ApiDocInfo(apiInfo, response, routes, gateway);
                } else {
                    throw new ApiDocNotFoundException("Could not load gateway instance for service " + serviceId + " .");
                }
            }
        } else {
            apiDocUrl = createApiDocUrlFromRouting(instanceInfo);
        }

        if (apiDocUrl == null) {
            throw new ApiDocNotFoundException("No API Documentation defined for service " + serviceId + " .");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON_UTF8));

        ResponseEntity<String> response = restTemplate.exchange(
            apiDocUrl,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class);

        return new ApiDocInfo(apiInfo, response, routes, gateway);
    }

    private ApiInfo findApi(List<ApiInfo> apiInfo, String apiVersion) {
        String expectedGatewayUrl = "api";

        if (apiVersion != null) {
            expectedGatewayUrl = "api/" + apiVersion;
        }

        for (ApiInfo api : apiInfo) {
            if (api.getGatewayUrl().equals(expectedGatewayUrl)) {
                return api;
            }
        }

        return apiInfo.get(0);
    }

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
            return null;
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
