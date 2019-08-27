/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.instance;

import com.ca.mfaas.apicatalog.model.APIContainer;
import com.ca.mfaas.apicatalog.services.cached.CachedProductFamilyService;
import com.ca.mfaas.apicatalog.services.cached.CachedServicesService;
import com.ca.mfaas.product.constants.CoreService;
import com.ca.mfaas.product.gateway.GatewayNotFoundException;
import com.ca.mfaas.product.instance.InstanceInitializationException;
import com.ca.mfaas.product.registry.CannotRegisterServiceException;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.ca.mfaas.constants.EurekaMetadataDefinition.CATALOG_ID;

/**
 * Initialize the API catalog with the running instances.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InstanceInitializeService {

    private final CachedProductFamilyService cachedProductFamilyService;
    private final CachedServicesService cachedServicesService;
    private final InstanceRetrievalService instanceRetrievalService;

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
            InstanceInfo apiCatalogInstance = instanceRetrievalService.getInstanceInfo(serviceId);
            if (apiCatalogInstance == null) {
                String msg = "API Catalog Instance not retrieved from Discovery service";
                log.warn(msg);
                throw new RetryException(msg);
            } else {
                log.info("API Catalog instance found, retrieving all services.");
                getAllInstances(apiCatalogInstance);
            }
        } catch (InstanceInitializationException | GatewayNotFoundException e) {
            throw new RetryException(e.getMessage());
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
        Applications discoveryApplications = instanceRetrievalService.getAllInstancesFromDiscovery(false);

        // Only include services which have a instances
        List<Application> listApplication = discoveryApplications.getRegisteredApplications()
            .stream()
            .filter(application -> !application.getInstances().isEmpty())
            .collect(Collectors.toList());

        // Return an empty string if no services are found after filtering
        if (listApplication.isEmpty()) {
            log.info("No services found");
            return;
        }

        log.debug("Found: " + listApplication.size() + " services on startup.");
        String s = listApplication.stream()
            .map(Application::getName).collect(Collectors.joining(", "));
        log.debug("Discovered Services: " + s);

        // create containers for services
        listApplication.forEach(this::createContainers);

        // populate the cache
        Collection<APIContainer> containers = cachedProductFamilyService.getAllContainers();
        log.debug("Cache contains: " + containers.size() + " tiles.");
    }


    private void createContainers(Application application) {
        cachedServicesService.updateService(application.getName(), application);
        application.getInstances().forEach(instanceInfo -> {
            String productFamilyId = instanceInfo.getMetadata().get(CATALOG_ID);
            if (productFamilyId != null) {
                log.debug("Initialising product family (creating tile for) : " + productFamilyId);
                cachedProductFamilyService.createContainerFromInstance(productFamilyId, instanceInfo);
            }

        });
    }

    private void getAllInstances(InstanceInfo apiCatalogInstance) {
        String productFamilyId = apiCatalogInstance.getMetadata().get(CATALOG_ID);
        if (productFamilyId != null) {
            log.debug("Initialising product family (creating tile for) : " + productFamilyId);
            cachedProductFamilyService.createContainerFromInstance(productFamilyId, apiCatalogInstance);
        }

        updateCacheWithAllInstances();
        log.info("API Catalog initialised with running services..");
    }
}
