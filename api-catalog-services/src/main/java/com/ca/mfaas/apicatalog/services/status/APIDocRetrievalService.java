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
import com.ca.mfaas.apicatalog.services.cached.CachedServicesService;
import com.ca.mfaas.apicatalog.services.status.model.ApiDocNotFoundException;
import com.ca.mfaas.apicatalog.services.status.model.ServiceNotFoundException;
import com.ca.mfaas.apicatalog.swagger.SubstituteSwaggerGenerator;
import com.ca.mfaas.product.family.ProductFamilyType;
import com.ca.mfaas.product.model.ApiInfo;
import com.netflix.appinfo.InstanceInfo;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@DependsOn("cachedServicesService")
public class APIDocRetrievalService {

    private final String HTTPS = "https";
    private final String HTTP = "http";
    private InstanceInfo gatewayInstance;
    private URI gatewayUrl;

    private final RestTemplate restTemplate;
    private final CachedServicesService cachedServicesService;
    private final EurekaMetadataParser metadataParser = new EurekaMetadataParser();
    private final SubstituteSwaggerGenerator swaggerGenerator = new SubstituteSwaggerGenerator();

    @Autowired
    public APIDocRetrievalService(RestTemplate restTemplate,
                                  CachedServicesService cachedServicesService) {
        this.restTemplate = restTemplate;
        this.cachedServicesService = cachedServicesService;
    }

    /**
     * Retrieve the API docs for a registered service (all versions)
     *
     * @param serviceId  the unique service id
     * @param apiVersion the version of the API
     * @return the api docs as a string
     */
    public ResponseEntity<String> retrieveApiDoc(@NonNull String serviceId, String apiVersion) {
        log.info("Attempting to retrieve API doc for service {} version {}",  serviceId, apiVersion);

        String apiDocUrl = null;
        try {
            // set the Gateway instance
            if (gatewayInstance == null) {
                gatewayInstance = cachedServicesService.getInstanceInfoForService(ProductFamilyType.GATEWAY.getServiceId());
                if (gatewayInstance != null) {
                    if (gatewayInstance.isPortEnabled(InstanceInfo.PortType.SECURE)) {
                        gatewayUrl = new URIBuilder().setScheme(HTTPS).setHost(gatewayInstance.getHostName()).setPort(gatewayInstance.getSecurePort()).build();
                    } else {
                        gatewayUrl = new URIBuilder().setScheme(HTTP).setHost(gatewayInstance.getHostName()).setPort(gatewayInstance.getPort()).build();
                    }
                } else {
                    throw new IllegalStateException("Cannot retrieve Gateway instance");
                }
            }

            // Get the Instance of the serviceId
            InstanceInfo serviceInstance = cachedServicesService.getInstanceInfoForService(serviceId);
            if (serviceInstance != null) {
                List<ApiInfo> apiInfo = metadataParser.parseApiInfo(serviceInstance.getMetadata());
                // check for static swagger
                if (apiInfo != null) {
                    ApiInfo api = findApi(apiInfo, apiVersion);
                    if (api.getSwaggerUrl() == null) {
                        return swaggerGenerator.generateSubstituteSwaggerForService(gatewayInstance, serviceInstance, api);
                    }
                    else {
                        apiDocUrl = api.getSwaggerUrl();
                    }
                }
            }

            // construct the API Doc url (gateway relative)
            if (apiDocUrl == null) {
                String version = apiVersion;
                if (version == null) {
                    version = "v1";
                }
                apiDocUrl = gatewayUrl.toASCIIString() + "/api/" + version + "/api-doc/" + serviceId.toLowerCase();
            }
        } catch (URISyntaxException e) {
            log.error("Exception thrown when constructing API Doc url for service {} version {}: {}", serviceId, apiVersion, e.getMessage());
            throw new ApiDocNotFoundException(e.getMessage());
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
            log.error("Exception thrown when retrieving API documentation for service {} version {}: {}", serviceId, apiVersion, e.getMessage());
            // more specific exception
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
     * Retrieve the API docs for a registered service (all versions)
     *
     * @param instance   retrieve API doc for this instance
     * @param apiVersion the version of the API
     * @return the api docs as a string
     */
    public String retrieveApiDocFromInstance(@NonNull InstanceInfo instance, String apiVersion) {
        log.info("Attempting to retrieve API doc for instance: " + instance.getInstanceId());
        try {
            // Create the request header
            HttpEntity<?> entity = createRequest();
            ResponseEntity<String> response;

            // construct the endpoint based on the instance details
            String baseUrl;
            String hostName = instance.getHostName();
            if (instance.isPortEnabled(InstanceInfo.PortType.SECURE)) {
                baseUrl = new URIBuilder().setHost(hostName)
                    .setScheme(HTTPS).setPort(instance.getSecurePort()).build().toString();
            } else {
                baseUrl = new URIBuilder().setHost(hostName)
                    .setScheme(HTTP).setPort(instance.getPort()).build().toString();
            }
            String instanceApiDocEndpoint = baseUrl + getLocalApiDocEndPoint(instance);

            log.info("Sending API Doc info request to: " + instanceApiDocEndpoint);
            response = restTemplate.exchange(
                    instanceApiDocEndpoint,
                    HttpMethod.GET,
                    entity,
                    String.class);

            // Handle errors (request may fail if service is unavailable)
            return response.getBody();
        } catch (Exception e) {
            log.error("Exception thrown when retrieving API Doc: " + e.getMessage(), e);
            // more specific exceptions
            throw new ApiDocNotFoundException("Request for API Doc to instance: " + instance.getInstanceId() +
                " failed with error: " + e.getMessage(), e);
        }
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
