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
import com.ca.mfaas.product.gateway.GatewayClient;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

import static com.ca.mfaas.product.constants.EurekaMetadataDefinition.CATALOG_ID;

/**
 * Refresh the cache with the latest state of the discovery service
 * Use deltas to get latest changes from Eureka
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InstanceRefreshService {

    // until versioning is implemented, only v1 API docs are supported
    private final GatewayClient gatewayClient;
    private final CachedProductFamilyService cachedProductFamilyService;
    private final CachedServicesService cachedServicesService;
    private final InstanceRetrievalService instanceRetrievalService;

    private static final String API_ENABLED_METADATA_KEY = "mfaas.discovery.enableApiDoc";

    /**
     * Periodically refresh the container/service caches
     * Depends on the GatewayClient: no refreshes happen when it's not initialized
     */
    @Scheduled(
        initialDelayString = "${mfaas.service-registry.cacheRefreshInitialDelayInMillis}",
        fixedDelayString = "${mfaas.service-registry.cacheRefreshRetryDelayInMillis}")
    public void refreshCacheFromDiscovery() {
        if (!gatewayClient.isInitialized() || !isApiCatalogInCache()) {
            log.debug("Gateway not found yet, skipping the InstanceRefreshService refresh");
            return;
        }

        log.debug("Refreshing API Catalog with the latest state of discovery service");

        Callable<Set<String>> callableTask = this::compareServices;

        // run the comparison in a separate thread
        ExecutorService executorService =
            new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
        Future<Set<String>> future = executorService.submit(callableTask);
        executorService.shutdown();

        try {
            // get result of future, wait 20 secs for a result , if nothing then throw an exception but continue processing
            final Set<String> containersUpdated = future.get(20, TimeUnit.SECONDS);
            if (containersUpdated.isEmpty()) {
                log.debug("No containers updated from discovered services.");
            } else {
                log.debug(containersUpdated.size() + " containers updated from discovered services.");
                log.debug("Catalog status updates will occur for containers: " + containersUpdated.toString());
            }
        } catch (InterruptedException e) {
            log.error("Failed to update cache with discovered services: " + e.getMessage(), e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException e) {
            log.error("Failed to update cache with discovered services: " + e.getMessage(), e);
        }
    }

    /**
     * @return a list of changed services
     */
    @SuppressWarnings("deprecation")
    private Set<String> compareServices() {
        // Updated containers
        Set<String> containersUpdated = new HashSet<>();
        Applications cachedServices = cachedServicesService.getAllCachedServices();
        Applications deltaFromDiscovery = instanceRetrievalService.getAllInstancesFromDiscovery(true);

        if (deltaFromDiscovery != null && !deltaFromDiscovery.getRegisteredApplications().isEmpty()) {
            // use the version to check if this delta has changed, it is deprecated and should be replaced as soon as a
            // newer identifier is provided by Netflix
            // if getVersion is removed then the process will be slightly more inefficient but will not need to change
            if (cachedServicesService.getVersionDelta() != deltaFromDiscovery.getVersion()) {
                containersUpdated = processServiceInstances(cachedServices, deltaFromDiscovery);
            }
            cachedServicesService.setVersionDelta(deltaFromDiscovery.getVersion());
        }
        return containersUpdated;
    }

    /**
     * Check each delta instance and consider it for processing
     *
     * @param cachedServices     the collection of cached services
     * @param deltaFromDiscovery changed instances
     */
    private Set<String> processServiceInstances(Applications cachedServices, Applications deltaFromDiscovery) {
        Set<String> containersUpdated = new HashSet<>();
        Set<InstanceInfo> updatedServices = updateDelta(deltaFromDiscovery);
        updatedServices.forEach(instance -> {
            try {
                // check if this instance should be processed/updated
                processServiceInstance(containersUpdated, cachedServices, deltaFromDiscovery, instance);
            } catch (Exception e) {
                log.error("could not update cache for service: " + instance + ", processing will continue.", e);
            }
        });
        return containersUpdated;
    }

    /**
     * Get this instance service details and check if it should be processed
     *
     * @param containersUpdated  containers, which were updated
     * @param cachedServices     existing services
     * @param deltaFromDiscovery changed service instances
     * @param instance           this instance
     */
    private void processServiceInstance(Set<String> containersUpdated, Applications cachedServices,
                                        Applications deltaFromDiscovery, InstanceInfo instance) {
        Application application = null;
        // Get the application which this instance belongs to
        if (cachedServices != null && cachedServices.getRegisteredApplications() != null) {
            application = cachedServices.getRegisteredApplications().stream()
                .filter(service -> service.getName().equalsIgnoreCase(instance.getAppName())).findFirst().orElse(null);
        }
        // if its new then it will only be in the delta
        if (application == null || application.getInstances().isEmpty()) {
            application = deltaFromDiscovery.getRegisteredApplications().stream()
                .filter(service -> service.getName().equalsIgnoreCase(instance.getAppName())).findFirst().orElse(null);
        }

        // there's no chance which this case is not called. It's just double check
        if (application == null || application.getInstances().isEmpty()) {
            log.debug("Instance {} couldn't get it from cache and delta", instance.getAppName());
            return;
        }

        processInstance(containersUpdated, instance, application);
    }

    /**
     * Go ahead and retrieve this instances API doc and update the cache
     *
     * @param containersUpdated containers, which were updated
     * @param instance          the instance
     * @param application       the service
     */
    private void processInstance(Set<String> containersUpdated, InstanceInfo instance, Application application) {
        application.addInstance(instance);

        if (!InstanceInfo.InstanceStatus.DOWN.equals(instance.getStatus())) {
            // update any containers which contain this service
            updateContainers(containersUpdated, instance);
        }

        // Update the service cache
        updateService(instance.getAppName(), application);
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

                InstanceInfo instanceInfo = application.getInstances().get(0);
                String value = instanceInfo.getMetadata().get(API_ENABLED_METADATA_KEY);
                boolean apiEnabled = true;
                if (value != null) {
                    apiEnabled = Boolean.parseBoolean(value);
                }

                // only add api enabled services
                if (apiEnabled) {
                    filteredServices.addApplication(application);
                } else {
                    log.debug("Service: " + application.getName() + " is not API enabled, it will be ignored by the API Catalog");
                }
            }
        }

        return filteredServices;
    }

    private void updateService(String serviceId, Application application) {
        if (application == null) {
            log.error("Could not find Application object for serviceId: " + serviceId + " cache not updated with " +
                "current values.");
        } else {
            cachedServicesService.updateService(serviceId, application);
            log.debug("Updated service cache for service: " + serviceId);
        }
    }

    private void updateContainers(Set<String> containersUpdated,
                                  InstanceInfo instanceInfo) {
        String apiEnabled = instanceInfo.getMetadata().get(API_ENABLED_METADATA_KEY);

        // only register API enabled services
        if (apiEnabled == null || Boolean.parseBoolean(apiEnabled)) {
            updateContainer(containersUpdated, instanceInfo.getAppName(), instanceInfo);
        }
    }

    /**
     * Update the container
     *
     * @param containersUpdated what containers were updated
     * @param serviceId         the service
     * @param instanceInfo      the instance
     */
    private void updateContainer(Set<String> containersUpdated, String serviceId, InstanceInfo instanceInfo) {
        String productFamilyId = instanceInfo.getMetadata().get(CATALOG_ID);
        if (productFamilyId == null) {
            log.warn("Cannot create a tile without a parent id, the metadata for service: " + serviceId +
                " must contain an entry for {}", CATALOG_ID);
        } else {
            APIContainer container = cachedProductFamilyService.saveContainerFromInstance(productFamilyId, instanceInfo);
            log.debug("Created/Updated tile and updated cache for container: " + container.getId() + " @ " + container.getLastUpdatedTimestamp().getTime());
            containersUpdated.add(productFamilyId);
        }
    }

    /**
     * Compare cached instances against eureka delta to send back a change-list
     *
     * @param delta retrieved from Eureka
     * @return changed instances
     */
    private Set<InstanceInfo> updateDelta(Applications delta) {
        // only process instances which are API Doc enabled
        delta = filterByApiEnabled(delta);
        int deltaCount = 0;
        Set<InstanceInfo> updatedInstances = new HashSet<>();
        for (Application app : delta.getRegisteredApplications()) {
            for (InstanceInfo instance : app.getInstances()) {
                ++deltaCount;
                if (InstanceInfo.ActionType.ADDED.equals(instance.getActionType())) {
                    log.debug("Added instance {} to the list of changed instances ", instance.getId());
                    updatedInstances.add(instance);
                } else if (InstanceInfo.ActionType.MODIFIED.equals(instance.getActionType())) {
                    log.debug("Modified instance {} added to the list of changed instances ", instance.getId());
                    updatedInstances.add(instance);
                } else if (InstanceInfo.ActionType.DELETED.equals(instance.getActionType())) {
                    log.debug("Deleted instance {} added to the list of changed instances ", instance.getId());
                    instance.setStatus(InstanceInfo.InstanceStatus.DOWN);
                    updatedInstances.add(instance);
                }
            }
        }

        log.debug("The total number of changed instances fetched by the delta processor : {}", deltaCount);
        return updatedInstances;
    }

    private boolean isApiCatalogInCache() {
        Application service = this.cachedServicesService.getService(CoreService.API_CATALOG.getServiceId());
        return service != null;
    }

}
