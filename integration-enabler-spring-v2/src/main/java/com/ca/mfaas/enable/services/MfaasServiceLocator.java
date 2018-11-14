/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.enable.services;

import com.ca.mfaas.product.config.MFaaSConfigPropertiesContainer;
import com.ca.mfaas.product.registry.ApplicationWrapper;
import com.ca.mfaas.product.registry.DiscoveryServiceNotAvailableException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.appinfo.InstanceInfo;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

/**
 * Find the URL of a service given a service Id
 */
@SuppressWarnings("Duplicates")
@Slf4j
@Service
public class MfaasServiceLocator {

    private final DiscoveryClient discoveryClient;
    private final MFaaSConfigPropertiesContainer propertiesContainer;
    private final RestTemplate restTemplate;

    private static final String APPS_ENDPOINT = "apps/";

    /**
     * Locate a service
     *  @param discoveryClient     internal client discovery
     * @param propertiesContainer Service properties
     */
    @Autowired
    public MfaasServiceLocator(DiscoveryClient discoveryClient, MFaaSConfigPropertiesContainer propertiesContainer) {
        this.discoveryClient = discoveryClient;
        this.propertiesContainer = propertiesContainer;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Locate the Gateway URL via discovery client or Eureka directly
     *
     * @return the Gateway URI
     * @param serviceId
     */
    public ServiceInstances getServiceInstances(@NonNull String serviceId) throws Exception {
        ServiceInstances serviceInstances = new ServiceInstances();
        try {
            serviceId = serviceId.toLowerCase();
            List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
            if (instances == null || instances.isEmpty()) {
                ApplicationWrapper application = extractAllInstancesFromDiscovery(serviceId);
                List<InstanceInfo> instanceInfos = application.getApplication().getInstances();
                serviceInstances.setInstanceInfos(instanceInfos);
            } else {
                // Check the instance VIP address for DVIPA location, otherwise send back the instance URL
                serviceInstances.setServiceInstances(instances);
            }
        } catch (Exception e) {
            log.error("Could not locate service: " + e.getMessage(), e);
            throw e;
        }
        return serviceInstances;
    }

    /**
     * Create HTTP headers
     *
     * @return HTTP Headers
     */
    @SuppressWarnings({"squid:CallToDeprecatedMethod", "deprecation"})
    private HttpHeaders createRequestHeader() throws URISyntaxException {
        URI discoveryURI = new URI(propertiesContainer.getDiscovery().getLocations());
        String credentials = discoveryURI.getUserInfo();
        String userId = null;
        String password = null;
        if (credentials != null) {
            String[] split = credentials.split(":");
            userId = split[0];
            password = split[1];
        }
        HttpHeaders headers = new HttpHeaders();
        if (userId != null && password != null) {
            String basicToken = "Basic " + Base64.getEncoder().encodeToString((userId + ":"
                + password).getBytes());
            headers.add("Authorization", basicToken);
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(new ArrayList<>(Collections.singletonList(MediaType.APPLICATION_JSON)));
        return headers;
    }

    /**
     * Retrieve all instances from the discovery service
     *
     * @return All Instances
     */
    private ApplicationWrapper extractAllInstancesFromDiscovery(String serviceId) throws URISyntaxException {

        // call Eureka REST endpoint to fetch single or all Instances
        ResponseEntity<String> response = queryDiscoveryForInstances(serviceId);

        ApplicationWrapper application = null;
        if (HttpStatus.OK.equals(response.getStatusCode()) && response.getBody() != null) {
            ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            try {
                application = mapper.readValue(response.getBody(), ApplicationWrapper.class);
            } catch (IOException e) {
                log.error("Could not parse service info from discovery --" + e.getMessage(), e);
            }
        }

        return application;
    }

    /**
     * Query Discovery
     *
     * @param serviceId search for this service
     * @return ResponseEntity<String> query response
     */
    private ResponseEntity<String> queryDiscoveryForInstances(String serviceId) throws URISyntaxException {
        String discoveryLocation = propertiesContainer.getDiscovery().getLocations();
        discoveryLocation += APPS_ENDPOINT + serviceId;
        HttpEntity<?> entity = new HttpEntity<>(null, createRequestHeader());
        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(
                discoveryLocation,
                HttpMethod.GET,
                entity,
                String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Could not locate instance for request: " + discoveryLocation
                    + ", " + response.getStatusCode() + " = " + response.getStatusCode().getReasonPhrase());
            }
        } catch (RestClientException e) {
            log.error("Discovery Service query failed: " + e.getMessage(), e);
            throw new DiscoveryServiceNotAvailableException("Discovery Service could not be located", e.getMostSpecificCause());
        }
        return response;
    }
}
