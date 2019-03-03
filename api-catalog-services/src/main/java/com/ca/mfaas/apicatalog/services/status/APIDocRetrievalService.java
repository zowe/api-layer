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
import com.ca.mfaas.apicatalog.services.initialisation.InstanceRetrievalService;
import com.ca.mfaas.apicatalog.services.status.model.ApiDocNotFoundException;
import com.ca.mfaas.apicatalog.services.status.model.ServiceNotFoundException;
import com.ca.mfaas.apicatalog.swagger.SubstituteSwaggerGenerator;
import com.ca.mfaas.product.constants.CoreService;
import com.ca.mfaas.product.model.ApiInfo;
import com.netflix.appinfo.InstanceInfo;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@DependsOn("instanceRetrievalService")
public class APIDocRetrievalService {

    private String gatewayUrl;

    private final RestTemplate restTemplate;
    private final InstanceRetrievalService instanceRetrievalService;
    private final EurekaMetadataParser metadataParser = new EurekaMetadataParser();
    private final SubstituteSwaggerGenerator swaggerGenerator = new SubstituteSwaggerGenerator();

    @Autowired
    public APIDocRetrievalService(RestTemplate restTemplate,
                                  InstanceRetrievalService instanceRetrievalService) {
        this.restTemplate = restTemplate;
        this.instanceRetrievalService = instanceRetrievalService;
    }

    /**
     * Retrieve the API docs for a registered service (all versions)
     *
     * @param serviceId  the unqiue service id
     * @param apiVersion the version of the API
     * @return the api docs as a string
     */
    public ResponseEntity<String> retrieveApiDoc(@NonNull String serviceId, String apiVersion) {
        log.info("Attempting to retrieve API doc for service {} version {}",  serviceId, apiVersion);

        String apiDocUrl = null;
        InstanceInfo instanceInfo = instanceRetrievalService.getInstanceInfo(serviceId);
        InstanceInfo gateway = instanceRetrievalService.getInstanceInfo("gateway");

        if (instanceInfo != null) {
            List<ApiInfo> apiInfo = metadataParser.parseApiInfo(instanceInfo.getMetadata());

            if (apiInfo != null) {
                ApiInfo api = findApi(apiInfo, apiVersion);
                if (api.getSwaggerUrl() == null) {
                    return swaggerGenerator.generateSubstituteSwaggerForService(gateway, instanceInfo, api);
                }
                else {
                    apiDocUrl = api.getSwaggerUrl();
                }
            }
        }

        if (apiDocUrl == null) {
            String version = apiVersion;
            if (version == null) {
                version = "v1";
            }
            apiDocUrl = getGatewayUrl() + "/api/" + version + "/api-doc/" + serviceId.toLowerCase();
        }

        try {
            // Create the request
            HttpEntity<?> entity = createRequest();
            ResponseEntity<String> response;
            log.info("Sending API documentation request for service {} version {} to: {}", serviceId, apiVersion, apiDocUrl);
            response = restTemplate.exchange(
                apiDocUrl,
                HttpMethod.GET,
                entity,
                String.class);

            // Handle errors (request may fail if service is unavailable)
            return handleResponse(serviceId, response);
        } catch (Exception e) {
            log.error("General exception thrown when retrieving API documentation for service {} version {} at {}: {}",
                serviceId, apiVersion, apiDocUrl, e.getMessage());
            // more specific exceptions
            throw new ApiDocNotFoundException(e.getMessage());
        }
    }

    private ApiInfo findApi(List<ApiInfo> apiInfo, String apiVersion) {
        String expectedGatewayUrl = "api";

        if (apiVersion != null) {
            expectedGatewayUrl = "api/" + apiVersion;
        }

        for (ApiInfo api: apiInfo) {
            if (api.getGatewayUrl().equals(expectedGatewayUrl)) {
                return api;
            }
        }

        return apiInfo.get(0);
    }

    /**
     * Check the instance metadata and locate the v1/api-doc endpoint
     * Fixed version V1 for now
     * @param instance the instance from which to retrieve the API doc
     * @return the local api-doc endpoint
     */
    private String getLocalApiDocEndPoint(InstanceInfo instance) {
        if (instance.getMetadata() == null || instance.getMetadata().isEmpty()) {
            throw new ApiDocNotFoundException("Instance: " + instance.getInstanceId() + " does not contain any metadata." +
                "Location of API Documentation cannot be ascertained.");
        } else {
            String key = instance.getMetadata().entrySet().stream()
                .filter(x -> x.getValue().toLowerCase().endsWith("/v1/api-doc"))
                .map(Map.Entry::getKey)
                .collect(Collectors.joining());
            key = key.replace(".gateway-url", ".service-url");
            String serviceUrl = instance.getMetadata().get(key);
            if (serviceUrl == null) {
                throw new ApiDocNotFoundException("Could not find Service URL in Instance metadata for: " + instance.getInstanceId()
                    + " -- " + key);
            }
            return serviceUrl;
        }
    }

    /**
     * return or retrieve the location of the Gateway
     * @return the location of the Gateway (full URL)
     */
    public String getGatewayUrl() {
        if (this.gatewayUrl == null) {
            InstanceInfo gatewayInstance = instanceRetrievalService.getInstanceInfo(CoreService.GATEWAY.getServiceId());
            if (gatewayInstance == null) {
                String msg = "Cannot obtain information about API Gateway from Discovery Service";
                log.error(msg);
                throw new ApiDocNotFoundException(msg);
            }
            String homePageUrl = gatewayInstance.getHomePageUrl();
            if (homePageUrl == null) {
                String msg = "Cannot obtain information about API Gateway home page from Discovery Service";
                log.error(msg);
                throw new ApiDocNotFoundException(msg);
            }
            if (homePageUrl.endsWith("/")) {
                homePageUrl = homePageUrl.substring(0, homePageUrl.length() - 1);
            }
            this.gatewayUrl = homePageUrl;
            log.info("Gateway location set: " + this.gatewayUrl);
        }
        return this.gatewayUrl;
    }

    private HttpEntity<?> createRequest() {
        HttpHeaders headers = new HttpHeaders();
        List<MediaType> types = new ArrayList<>();
        types.add(MediaType.APPLICATION_JSON_UTF8);
        headers.setAccept(types);
        return new HttpEntity<>(headers);
    }

    private ResponseEntity<String> handleResponse(@NonNull String serviceId, ResponseEntity<String> response) {
        if (response.getStatusCode().equals(HttpStatus.OK)) {
            log.info("Found API doc for: " + serviceId);
            return response;
        } else {
            log.warn("Could retrieve API doc for: " + serviceId + " reason: " + response.getStatusCode()
                + " -- " + response.getStatusCode().getReasonPhrase());
            String notRunningMessage = "The Gateway or Service: " + serviceId + " is not found or not running.";
            boolean notRunning = false;
            if (response.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                log.warn(notRunningMessage);
                notRunning = true;
            }
            throw new ServiceNotFoundException(notRunning ? notRunningMessage : " The service is running.");
        }
    }
}
