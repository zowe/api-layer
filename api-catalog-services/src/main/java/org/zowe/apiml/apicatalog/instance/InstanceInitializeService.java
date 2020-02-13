/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.apicatalog.instance;

import org.zowe.apiml.apicatalog.model.APIContainer;
import org.zowe.apiml.apicatalog.services.cached.CachedProductFamilyService;
import org.zowe.apiml.apicatalog.services.cached.CachedServicesService;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.product.gateway.GatewayNotAvailableException;
import org.zowe.apiml.product.instance.InstanceInitializationException;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.product.registry.CannotRegisterServiceException;
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

import static org.zowe.apiml.constants.EurekaMetadataDefinition.CATALOG_ID;

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
    private final InstanceRefreshService instanceRefreshService;

    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();

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
        backoff = @Backoff(delayExpression = "#{${apiml.service-registry.serviceFetchDelayInMillis}}"))
    public void retrieveAndRegisterAllInstancesWithCatalog() throws CannotRegisterServiceException {
        log.info("Initialising API Catalog with Discovery services.");
        try {
            String serviceId = CoreService.API_CATALOG.getServiceId();
            InstanceInfo apiCatalogInstance = instanceRetrievalService.getInstanceInfo(serviceId);
            if (apiCatalogInstance == null) {
                String msg = "API Catalog Instance not retrieved from Discovery service";
                log.debug(msg);
                throw new RetryException(msg);
            } else {
                log.info("API Catalog instance found, retrieving all services.");
                getAllInstances(apiCatalogInstance);
                instanceRefreshService.start();
            }
        } catch (InstanceInitializationException | GatewayNotAvailableException e) {
            throw new RetryException(e.getMessage());
        } catch (Exception e) {
            String msg = "An unexpected exception occurred when trying to retrieve API Catalog instance from Discovery service";
            apimlLog.log("org.zowe.apiml.apicatalog.initializeAborted", e.getMessage());
            throw new CannotRegisterServiceException(msg, e);
        }
    }

    @Recover
    public void recover(RetryException e) {
        apimlLog.log("org.zowe.apiml.apicatalog.initializeFailed");
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
