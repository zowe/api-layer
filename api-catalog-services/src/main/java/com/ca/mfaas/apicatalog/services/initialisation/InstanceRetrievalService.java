/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.services.initialisation;

import com.ca.mfaas.enable.services.DiscoveredServiceInstance;
import com.ca.mfaas.enable.services.DiscoveredServiceInstances;
import com.ca.mfaas.enable.services.MfaasServiceLocator;
import com.ca.mfaas.product.config.MFaaSConfigPropertiesContainer;
import com.ca.mfaas.product.registry.ApplicationWrapper;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.converters.jackson.EurekaJsonJacksonCodec;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.RetryException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.validation.constraints.NotBlank;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;

@Slf4j
@Service
@DependsOn("mfaasServiceLocator")
public class InstanceRetrievalService {

    private final MFaaSConfigPropertiesContainer propertiesContainer;
    private final RestTemplate restTemplate;
    private final MfaasServiceLocator mfaasServiceLocator;

    private static final String APPS_ENDPOINT = "apps/";
    private static final String DELTA_ENDPOINT = "delta";
    private static final String UNKNOWN = "unknown";

    @Autowired
    public InstanceRetrievalService(MFaaSConfigPropertiesContainer propertiesContainer,
                                    RestTemplate restTemplate,
                                    MfaasServiceLocator mfaasServiceLocator) {
        this.propertiesContainer = propertiesContainer;
        this.restTemplate = restTemplate;
        this.mfaasServiceLocator = mfaasServiceLocator;
    }

    /**
     * Retrieve instances for a requested serviceId
     * This is the preferred method over getInstanceInfoFromDiscovery as it does not retry
     * @param serviceId the eureka id of the service
     * @return DiscoveredServiceInstances a collection of InstanceInfo or ServiceInstance objects
     */
    public DiscoveredServiceInstance getDiscoveredServiceInstances(String serviceId) {
        return mfaasServiceLocator.getServiceInstances(serviceId);
    }

    /**
     * Retrieve all discovered services instances
     * This is the preferred method over getInstanceInfoFromDiscovery as it does not retry
     * @return DiscoveredServiceInstances a collection of InstanceInfo or ServiceInstance objects
     */
    public DiscoveredServiceInstances getAllDiscoveredServiceInstances() {
        return mfaasServiceLocator.getAllServiceInstances();
    }

    /**
     * Query the Discovery Service directly via REST to get an instance
     * This method retries if the fetch fails
     * @param serviceId the service to search for
     * @return service instance
     */
    public InstanceInfo getInstanceInfoFromDiscovery(@NotBlank(message = "Service Id must be supplied") String serviceId) {
        Pair<String, Pair<String, String>> requestInfo;
        try {
            if (serviceId.equalsIgnoreCase(UNKNOWN)) {
                return null;
            }

            requestInfo = constructServiceInfoQueryRequest(serviceId, false);

            // call Eureka REST endpoint to fetch single or all Instances
            ResponseEntity<String> response = queryDiscoveryForInstances(requestInfo);
            if (response.getStatusCode().is2xxSuccessful()) {
                return extractSingleInstanceFromApplication(serviceId, requestInfo.getLeft(), response);
            }
        } catch (Exception e) {
            String msg = "An error occurred when trying to get instance info for:  " + serviceId;
            log.error(msg, e);
            throw new RetryException(msg);
        }
        return null;
    }

    /**
     * Extract applications
     *
     * @param requestInfo
     * @param response
     * @return
     */
    private Applications extractApplications(Pair<String, Pair<String, String>> requestInfo, ResponseEntity<String> response) {
        Applications applications = null;
        if (!HttpStatus.OK.equals(response.getStatusCode()) || response.getBody() == null) {
            log.warn("Could not retrieve all service info from discovery --" + response.getStatusCode()
                + " -- " + response.getStatusCode().getReasonPhrase() + " -- URL: " + requestInfo.getLeft());
        } else {
            ObjectMapper mapper = new EurekaJsonJacksonCodec().getObjectMapper(Applications.class);
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            try {
                applications = mapper.readValue(response.getBody(), Applications.class);
            } catch (IOException e) {
                log.error("Could not parse service info from discovery --" + e.getMessage(), e);
            }
        }
        return applications;
    }

    /**
     * Query discovery for any changed items (delta)
     * @return a list of changed Applications
     */
    public Applications extractDeltaFromDiscovery() {
        return getApplicationsFromDiscovery(true);
    }

    /**
     * Query discovery for all items
     * @return a list of all Applications
     */
    public Applications extractServicesFromDiscovery() {
        return getApplicationsFromDiscovery(false);
    }

    public boolean isApiEnabled(Application application, String apiEnabledMetadataKey) {
        InstanceInfo instanceInfo = application.getInstances().get(0);
        String value = instanceInfo.getMetadata().get(apiEnabledMetadataKey);
        boolean apiEnabled = true;
        if (value != null) {
            apiEnabled = Boolean.valueOf(value);
        }
        return apiEnabled;
    }

    /**
     * Query Discovery
     *
     * @param requestInfo information used to query the discovery service
     * @return ResponseEntity<String> query response
     */
    private ResponseEntity<String> queryDiscoveryForInstances(Pair<String, Pair<String, String>> requestInfo) {
        HttpEntity<?> entity = new HttpEntity<>(null, createRequestHeader(requestInfo.getRight()));
        ResponseEntity<String> response = restTemplate.exchange(
            requestInfo.getLeft(),
            HttpMethod.GET,
            entity,
            String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("Could not locate instance for request: " + requestInfo.getLeft()
                + ", " + response.getStatusCode() + " = " + response.getStatusCode().getReasonPhrase());
        }
        return response;
    }

    /**
     * @param serviceId the service to search for
     * @param url       try to find instance with this discovery url
     * @param response  the fetch attempt response
     * @return service instance
     */
    private InstanceInfo extractSingleInstanceFromApplication(String serviceId, String url, ResponseEntity<String> response) {
        ApplicationWrapper application = null;
        if (!HttpStatus.OK.equals(response.getStatusCode()) || response.getBody() == null) {
            log.warn("Could not retrieve service: " + serviceId + " instance info from discovery --" + response.getStatusCode()
                + " -- " + response.getStatusCode().getReasonPhrase() + " -- URL: " + url);
            return null;
        } else {
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            try {
                application = mapper.readValue(response.getBody(), ApplicationWrapper.class);
            } catch (IOException e) {
                log.error("Could not extract service: " + serviceId + " info from discovery --" + e.getMessage(), e);
            }
        }

        if (application != null
            && application.getApplication() != null
            && application.getApplication().getInstances() != null
            && !application.getApplication().getInstances().isEmpty()) {
            return application.getApplication().getInstances().get(0);
        } else {
            return null;
        }
    }

    /**
     * Construct a tuple used to query the discovery service
     *
     * @param serviceId optional service id
     * @return request information
     */
    private Pair<String, Pair<String, String>> constructServiceInfoQueryRequest(String serviceId, boolean getDelta) {
        String discoveryServiceLocatorUrl = propertiesContainer.getDiscovery().getLocations() + APPS_ENDPOINT;
        if (getDelta) {
            discoveryServiceLocatorUrl += DELTA_ENDPOINT;
        } else {
            if (serviceId != null) {
                discoveryServiceLocatorUrl += serviceId.toLowerCase();
            }
        }
        Pair<String, String> discoveryServiceCredentials =
            Pair.of(propertiesContainer.getDiscovery().getEurekaUserName(),
                propertiesContainer.getDiscovery().getEurekaUserPassword());
        log.debug("Checking instance info from: " + discoveryServiceLocatorUrl);
        return Pair.of(discoveryServiceLocatorUrl, discoveryServiceCredentials);
    }

    /**
     * Create HTTP headers
     *
     * @return HTTP Headers
     */
    private HttpHeaders createRequestHeader(Pair<String, String> credentials) {
        HttpHeaders headers = new HttpHeaders();
        if (credentials != null && credentials.getLeft() != null && credentials.getRight() != null) {
            String basicToken = "Basic " + Base64.getEncoder().encodeToString((credentials.getLeft() + ":"
                + credentials.getRight()).getBytes());
            headers.add("Authorization", basicToken);
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(new ArrayList<>(Collections.singletonList(MediaType.APPLICATION_JSON)));
        return headers;
    }

    private Applications getApplicationsFromDiscovery(boolean getDelta) {
        Pair<String, Pair<String, String>> requestInfo = constructServiceInfoQueryRequest(null, getDelta);

        // call Eureka REST endpoint to fetch single or all Instances
        ResponseEntity<String> response = queryDiscoveryForInstances(requestInfo);

        return extractApplications(requestInfo, response);
    }
}
