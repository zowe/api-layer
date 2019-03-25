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

import com.ca.mfaas.apicatalog.model.APIContainer;
import com.ca.mfaas.apicatalog.services.cached.CachedProductFamilyService;
import com.ca.mfaas.apicatalog.services.cached.CachedServicesService;
import com.ca.mfaas.product.config.MFaaSConfigPropertiesContainer;
import com.ca.mfaas.product.constants.CoreService;
import com.ca.mfaas.product.registry.ApplicationWrapper;
import com.ca.mfaas.product.registry.CannotRegisterServiceException;
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
import org.springframework.http.*;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.retry.RetryException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.validation.constraints.NotBlank;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

@Slf4j
@Service
public class InstanceRetrievalService {

    private final CachedProductFamilyService cachedProductFamilyService;
    private final MFaaSConfigPropertiesContainer propertiesContainer;
    private final CachedServicesService cachedServicesService;
    private final RestTemplate restTemplate;

    private static final String APPS_ENDPOINT = "apps/";
    private static final String DELTA_ENDPOINT = "delta";
    private static final String API_ENABLED_METADATA_KEY = "mfaas.discovery.enableApiDoc";
    private static final String UNKNOWN = "unknown";

    @Autowired
    public InstanceRetrievalService(CachedProductFamilyService cachedProductFamilyService,
                                    MFaaSConfigPropertiesContainer propertiesContainer,
                                    CachedServicesService cachedServicesService,
                                    RestTemplate restTemplate) {
        this.cachedProductFamilyService = cachedProductFamilyService;
        this.propertiesContainer = propertiesContainer;
        this.cachedServicesService = cachedServicesService;
        this.restTemplate = restTemplate;

        configureUnicode(restTemplate);
    }

    /**
     * Initialise the API Catalog with all current running instances
     * The API Catalog itself must be UP before checking all other instances
     * If the catalog is not up, or if the fetch fails, then wait for a defined period and retry up to a max of 5 times
     *
     * @throws CannotRegisterServiceException if the fetch fails or the catalog is not registered with the discovery
     */
    @Retryable(
        value = {RetryException.class},
        exclude = CannotRegisterServiceException.class,
        maxAttempts = 5,
        backoff = @Backoff(delayExpression = "#{${mfaas.service-registry.serviceFetchDelayInMillis}}"))
    public void retrieveAndRegisterAllInstancesWithCatalog() throws CannotRegisterServiceException {
        log.info("Initialising API Catalog with Discovery services.");
        try {
            String serviceId = CoreService.API_CATALOG.getServiceId();
            InstanceInfo apiCatalogInstance = getInstanceInfo(serviceId);
            if (apiCatalogInstance == null) {
                String msg = "API Catalog Instance not retrieved from discovery service";
                log.warn(msg);
                throw new RetryException(msg);
            } else {
                log.info("API Catalog instance found, retrieving all services.");
                getAllInstances(apiCatalogInstance);
            }
        } catch (RetryException e) {
            throw e;
        } catch (Exception e) {
            String msg = "An unexpected exception occurred when trying to retrieve API Catalog instance from Discovery service";
            log.warn(msg, e);
            throw new CannotRegisterServiceException(msg, e);
        }
    }


    @Recover
    public void recover(RetryException e) {
        log.warn("Failed to initialise API Catalog with services running in the Gateway.");
    }

    /**
     * Query the discovery service for all running instances
     */
    private void updateCacheWithAllInstances() {
        Applications allServices = extractAllInstancesFromDiscovery();

        // Only include services which have API doc enabled
        allServices = filterByApiEnabled(allServices);

        // Return an empty string if no services are found after filtering
        if (allServices.getRegisteredApplications().isEmpty()) {
            log.info("No services found");
            return;
        }

        log.debug("Found: " + allServices.size() + " services on startup.");
        String s = allServices.getRegisteredApplications().stream()
            .map(Application::getName).collect(Collectors.joining(", "));
        log.debug("Discovered Services: " + s);

        // create containers for services
        for (Application application : allServices.getRegisteredApplications()) {
            createContainers(application);
        }

        // populate the cache
        Collection<APIContainer> containers = cachedProductFamilyService.getAllContainers();
        log.debug("Cache contains: " + containers.size() + " tiles.");
    }

    /**
     * @param serviceId the service to search for
     * @return service instance
     */
    public InstanceInfo getInstanceInfo(@NotBlank(message = "Service Id must be supplied") String serviceId) {
        if (serviceId.equalsIgnoreCase(UNKNOWN)) {
            return null;
        }

        InstanceInfo instanceInfo = null;
        try {
            Pair<String, Pair<String, String>> requestInfo = constructServiceInfoQueryRequest(serviceId, false);

            // call Eureka REST endpoint to fetch single or all Instances
            ResponseEntity<String> response = queryDiscoveryForInstances(requestInfo);
            if (response.getStatusCode().is2xxSuccessful()) {
                instanceInfo = extractSingleInstanceFromApplication(serviceId, requestInfo.getLeft(), response);
            }
        } catch (Exception e) {
            String msg = "An error occurred when trying to get instance info for:  " + serviceId;
            log.error(msg, e);
            throw new RetryException(msg);
        }

        return instanceInfo;
    }

    /**
     * Retrieve all instances from the discovery service
     *
     * @return All Instances
     */
    public Applications extractAllInstancesFromDiscovery() {

        Pair<String, Pair<String, String>> requestInfo = constructServiceInfoQueryRequest(null, false);

        //  call Eureka REST endpoint to fetch single or all Instances
        ResponseEntity<String> response = queryDiscoveryForInstances(requestInfo);

        return extractApplications(requestInfo, response);
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

    public Applications extractDeltaFromDiscovery() {

        Pair<String, Pair<String, String>> requestInfo = constructServiceInfoQueryRequest(null, true);

        // call Eureka REST endpoint to fetch single or all Instances
        ResponseEntity<String> response = queryDiscoveryForInstances(requestInfo);

        return extractApplications(requestInfo, response);
    }

    /**
     * Only include services for caching if they have API doc enabled in their metadata
     *
     * @param discoveredServices all discovered services
     * @return only API Doc enabled services
     */
    private Applications filterByApiEnabled(Applications discoveredServices) {
        Applications filteredServices = new Applications();
        for (Application application : discoveredServices.getRegisteredApplications()) {
            if (!application.getInstances().isEmpty()) {
                processInstance(filteredServices, application);
            }
        }

        return filteredServices;
    }

    private void processInstance(Applications filteredServices, Application application) {
        InstanceInfo instanceInfo = application.getInstances().get(0);
        String value = instanceInfo.getMetadata().get(API_ENABLED_METADATA_KEY);
        boolean apiEnabled = true;
        if (value != null) {
            apiEnabled = Boolean.valueOf(value);
        }

        // only add api enabled services
        if (apiEnabled) {
            if (filteredServices == null) {
                filteredServices = new Applications();
            }
            filteredServices.addApplication(application);
        } else {
            log.debug("Service: " + application.getName() + " is not API enabled, it will be ignored by the API Catalog");
        }
    }

    private void createContainers(Application application) {
        cachedServicesService.updateService(application.getName(), application);
        application.getInstances().forEach(instanceInfo -> {
            String productFamilyId = instanceInfo.getMetadata().get("mfaas.discovery.catalogUiTile.id");
            if (productFamilyId != null) {
                log.debug("Initialising product family (creating tile for) : " + productFamilyId);
                cachedProductFamilyService.createContainerFromInstance(productFamilyId, instanceInfo);
            }

        });
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
        log.debug("Eureka credentials retrieved for user: " + propertiesContainer.getDiscovery().getEurekaUserName() +
            (!propertiesContainer.getDiscovery().getEurekaUserPassword().isEmpty() ? "*******" : "NO PASSWORD"));

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

    private void getAllInstances(InstanceInfo apiCatalogInstance) {
        String productFamilyId = apiCatalogInstance.getMetadata().get("mfaas.discovery.catalogUiTile.id");
        if (productFamilyId != null) {
            log.debug("Initialising product family (creating tile for) : " + productFamilyId);
            cachedProductFamilyService.createContainerFromInstance(productFamilyId, apiCatalogInstance);
        }

        updateCacheWithAllInstances();
        log.info("API Catalog initialised with running services..");
    }

    private void configureUnicode(RestTemplate restTemplate) {
        restTemplate.getMessageConverters()
            .add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
    }
}
