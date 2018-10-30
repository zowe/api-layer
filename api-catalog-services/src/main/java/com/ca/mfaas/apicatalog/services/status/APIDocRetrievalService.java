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

import com.ca.mfaas.apicatalog.services.initialisation.InstanceRetrievalService;
import com.ca.mfaas.apicatalog.services.status.model.ApiDocNotFoundException;
import com.ca.mfaas.apicatalog.services.status.model.ServiceNotFoundException;
import com.ca.mfaas.product.family.ProductFamilyType;
import com.netflix.appinfo.InstanceInfo;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.*;
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
@DependsOn("instanceRetrievalService")
public class APIDocRetrievalService {

    private final String HTTPS = "https";
    private final String HTTP = "http";
    private String gatewayUrl;

    private final RestTemplate restTemplate;
    private final InstanceRetrievalService instanceRetrievalService;

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
        log.info("Attempting to retrieve API doc for: " + serviceId);
        String version = apiVersion;
        if (version == null) {
            // ********** HARDCODED V1 ! remove when multi version implemented
            version = "v1";
        }
        try {
            // Create the request
            HttpEntity<?> entity = createRequest();
            ResponseEntity<String> response;
            String targetEndpointInGateway;

            targetEndpointInGateway = getGatewayUrl() +
                "/api/" + version + "/api-doc/" + serviceId.toLowerCase();

            log.info("Sending API Doc info request to: " + targetEndpointInGateway);
            response = restTemplate.exchange(
                targetEndpointInGateway,
                HttpMethod.GET,
                entity,
                String.class);

            // Handle errors (request may fail if service is unavailable)
            return handleResponse(serviceId, response);
        } catch (Exception e) {
            log.error("General Exception thrown when retrieving API Doc: " + e.getMessage(), e);
            // more specific exceptions
            throw new ApiDocNotFoundException(e.getMessage());
        }
    }

    /**
     * Retrieve the API docs for a registered service (all versions)
     *
     * @param instance retrieve API doc for this instance
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

    /**
     * return or retrieve the location of the Gateway
     * @return the location of the Gateway (full URL)
     */
    public String getGatewayUrl() {
        if (this.gatewayUrl == null) {
            InstanceInfo gatewayInstance = instanceRetrievalService.getInstanceInfo(ProductFamilyType.GATEWAY.getServiceId());
            try {
                URI gateway;
                boolean securePortEnabled = gatewayInstance.isPortEnabled(InstanceInfo.PortType.SECURE);
                String homePageUrl = gatewayInstance.getHomePageUrl();
                if (homePageUrl != null && !homePageUrl.toLowerCase().contains(HTTPS)) {
                    log.info("Overriding secure port setting for: " + homePageUrl);
                    securePortEnabled = false;
                }
                if (securePortEnabled) {
                    gateway = new URIBuilder().setScheme(HTTPS).setHost(gatewayInstance.getHostName()).setPort(gatewayInstance.getSecurePort()).build();
                } else {
                    gateway = new URIBuilder().setScheme(HTTP).setHost(gatewayInstance.getHostName()).setPort(gatewayInstance.getPort()).build();
                }
                this.gatewayUrl = gateway.toString();
                log.info("Gateway location set: " + this.gatewayUrl);
            } catch (URISyntaxException e) {
                String msg = "Cannot construct Gateway URL from Instance Info.";
                log.error(msg, e);
                throw new IllegalArgumentException(msg, e);
            }
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
