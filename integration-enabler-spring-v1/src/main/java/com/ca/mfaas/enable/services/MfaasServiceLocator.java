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

import com.ca.mfaas.enable.model.ApplicationWrapper;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.appinfo.InstanceInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClient;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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

    // do not overide existing RestTemplate
    @Autowired(required = false)
    private RestTemplate restTemplate;

    private static final String APPS_ENDPOINT = "apps";

    @Value("${mfaas.discovery.locations}")
    private String discoveryLocations;

    /**
     * Locate a service
     *
     * @param discoveryClient     internal client discovery
     */
    @Autowired
    public MfaasServiceLocator(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
        if (this.restTemplate == null) {
            this.restTemplate = new RestTemplate();
        }
    }

    /**
     * Locate the Gateway URL via discovery client or Eureka directly
     *
     * @return the Gateway URI
     */
    public URI locateGatewayUrl() throws URISyntaxException {
        try {
            String serviceId = "gateway";
            List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
            if (instances == null || instances.isEmpty()) {
                log.debug("Could not locate any running instances of: " + serviceId
                    + "using DiscoveryClient, querying Eureka");
                ApplicationWrapper application = extractAllInstancesFromDiscovery(serviceId);
                if (application != null && application.getApplication() != null) {
                    List<InstanceInfo> instanceInfos = application.getApplication().getInstances();
                    return checkInstancesInfos(instanceInfos);
                } else {
                    return null;
                }
            } else {
                // Check the instance VIP address for DVIPA location, otherwise send back the instance URL
                return this.checkServiceInstances(instances);
            }
        } catch (Exception e) {
            log.error("Could not locate Gateway location: " + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Check instances of type InstanceInfo
     *
     * @param instances of type InstanceInfo
     * @return the GatewayURI
     * @throws URISyntaxException could not create gateway URI
     */
    private URI checkInstancesInfos(List<InstanceInfo> instances) throws URISyntaxException {
        URI gatewayURI;
        InstanceInfo instanceInfo = instances.get(0);
        String vipAddress = instanceInfo.getVIPAddress();
        String hostName;
        if (instanceInfo.isPortEnabled(InstanceInfo.PortType.SECURE)) {
            if (vipAddress != null) {
                hostName = instanceInfo.getSecureVipAddress();
            } else {
                hostName = instanceInfo.getHostName();
            }
            gatewayURI = new URIBuilder().setScheme("https").setHost(hostName).setPort(instanceInfo.getSecurePort()).build();
        } else {
            if (vipAddress != null) {
                hostName = instanceInfo.getVIPAddress();
            } else {
                hostName = instanceInfo.getHostName();
            }
            gatewayURI = new URIBuilder().setScheme("http").setHost(hostName).setPort(instanceInfo.getPort()).build();
        }

        return gatewayURI;
    }

    /**
     * Check instances for this service
     *
     * @param instances check these instances
     * @return the Gateway URI
     * @throws URISyntaxException cannot construct URI
     */
    private URI checkServiceInstances(List<ServiceInstance> instances) throws URISyntaxException {
        ServiceInstance serviceInstance = instances.get(0);
        if (serviceInstance instanceof InstanceInfo) {
            return checkInstancesInfos(Collections.singletonList((InstanceInfo) serviceInstance));
        } else if (serviceInstance instanceof EurekaDiscoveryClient.EurekaServiceInstance) {
            EurekaDiscoveryClient.EurekaServiceInstance instance = (EurekaDiscoveryClient.EurekaServiceInstance) serviceInstance;
            return checkInstancesInfos(Collections.singletonList(instance.getInstanceInfo()));
        } else {
            String scheme;
            int port;
            if (serviceInstance.isSecure()) {
                scheme = "https";
                port = serviceInstance.getPort();
            } else {
                scheme = "http";
                port = serviceInstance.getPort();
            }
            return new URIBuilder().setScheme(scheme).setHost(serviceInstance.getHost()).setPort(port).build();
        }
    }

    /**
     * Create HTTP headers
     *
     * @return HTTP Headers
     */
    @SuppressWarnings({"squid:CallToDeprecatedMethod", "deprecation"})
    private HttpHeaders createRequestHeader() throws URISyntaxException {
        URI discoveryURI = new URI(discoveryLocations);
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
        String discoveryLocation = discoveryLocations;
        discoveryLocation += APPS_ENDPOINT + "/" + serviceId;
        HttpEntity<?> entity = new HttpEntity<>(null, createRequestHeader());
        ResponseEntity<String> response = restTemplate.exchange(
            discoveryLocation,
            HttpMethod.GET,
            entity,
            String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("Could not locate instance for request: " + discoveryLocation
                + ", " + response.getStatusCode() + " = " + response.getStatusCode().getReasonPhrase());
        }
        return response;
    }
}
