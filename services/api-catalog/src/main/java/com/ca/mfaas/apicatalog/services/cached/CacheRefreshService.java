/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.services.cached;

import com.ca.mfaas.apicatalog.model.APIContainer;
import com.ca.mfaas.apicatalog.services.initialisation.InstanceRetrievalService;
import com.ca.mfaas.apicatalog.services.status.APIDocRetrievalService;
import com.ca.mfaas.apicatalog.services.status.APIServiceStatusService;
import com.ca.mfaas.enable.model.ApiDocConfigException;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Refresh the cache with the latest state of the discovery service
 * Use deltas to get latest changes from Eureka
 */
@Service
@DependsOn("instanceRetrievalService")
public class CacheRefreshService {

    // until versioning is implemented, only v1 API docs are supported
    private static final String API_VERSION = "v1";
    private static final String API_ENABLED_METADATA_KEY = "mfaas.discovery.enableApiDoc";
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(CacheRefreshService.class);
    private final CachedProductFamilyService cachedProductFamilyService;
    private final CachedServicesService cachedServicesService;
    private final APIServiceStatusService apiServiceStatusService;
    private final InstanceRetrievalService instanceRetrievalService;
    private final APIDocRetrievalService apiDocRetrievalService;
    private final CachedApiDocService cachedApiDocService;

    @Autowired
    public CacheRefreshService(CachedProductFamilyService cachedProductFamilyService,
                               CachedServicesService cachedServicesService,
                               APIServiceStatusService apiServiceStatusService,
                               InstanceRetrievalService instanceRetrievalService,
                               APIDocRetrievalService apiDocRetrievalService, CachedApiDocService cachedApiDocService) {
        this.cachedProductFamilyService = cachedProductFamilyService;
        this.cachedServicesService = cachedServicesService;
        this.apiServiceStatusService = apiServiceStatusService;
        this.instanceRetrievalService = instanceRetrievalService;
        this.apiDocRetrievalService = apiDocRetrievalService;
        this.cachedApiDocService = cachedApiDocService;
    }

    /**
     * Periodically refresh the container/service caches
     */
    @Scheduled(
        initialDelayString = "${mfaas.service-registry.cacheRefreshInitialDelayInMillis}",
        fixedDelayString = "${mfaas.service-registry.cacheRefreshRetryDelayInMillis}")
    public void refreshCacheFromDiscovery() {
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
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
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
        Applications cachedServices = apiServiceStatusService.getCachedApplicationState();
        Applications deltaFromDiscovery = instanceRetrievalService.extractDeltaFromDiscovery();

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
     * @param containersUpdated  which containers were updated
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

        processInstance(containersUpdated, instance, application);
    }

    /**
     * Go ahead amd retrieve this instances API doc and update the cache
     *
     * @param containersUpdated which containers were updated
     * @param instance          the instance
     * @param application       the service
     */
    private void processInstance(Set<String> containersUpdated, InstanceInfo instance, Application application) {
        if (InstanceInfo.InstanceStatus.DOWN.equals(instance.getStatus())) {
            application.removeInstance(instance);
        }

        // Update the service cache
        updateService(instance.getAppName(), application);

        // update any containers which contain this service
        updateContainers(containersUpdated, instance.getAppName(), application);
    }

    /**
     * Update the API Doc cache for a service
     *
     * @param serviceId the service Id
     * @param version   the API Doc version
     * @param apiDoc    the API Doc
     */
    private void updateApiDocCache(String serviceId, String version, String apiDoc) {
        try {
            if (apiDoc != null && !apiDoc.isEmpty()) {
                String existingApiDoc = cachedApiDocService.getApiDocForService(serviceId, version);
                if (existingApiDoc == null || !existingApiDoc.equalsIgnoreCase(apiDoc)) {
                    log.debug("Updating API doc for service: " + serviceId);
                    cachedApiDocService.updateApiDocForService(serviceId, version, apiDoc);
                }
            } else {
                log.warn("Ignoring empty API doc for service: " + serviceId);
            }
        } catch (Exception e) {
            throw new ApiDocConfigException("Could not update API Doc for service: " + serviceId + ": "
                + e.getMessage(), e);
        }
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
                    apiEnabled = Boolean.valueOf(value);
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

    private void updateContainers(Set<String> containersUpdated, String serviceId, Application application) {
        List<APIContainer> containers = cachedProductFamilyService.getContainersForService(serviceId);
        if (containers == null || containers.isEmpty()) {
            if (application == null || application.getInstances() == null || application.getInstances().isEmpty()) {
                log.error("Cannot create a tile for an empty instance");
            } else {
                InstanceInfo instanceInfo = application.getInstances().get(0);
                String apiEnabled = instanceInfo.getMetadata().get(API_ENABLED_METADATA_KEY);

                // only register API enabled services
                if (apiEnabled == null || Boolean.valueOf(apiEnabled)) {
                    updateContainer(containersUpdated, serviceId, instanceInfo);
                }
            }
        } else {
            containers.forEach(container -> {
                cachedProductFamilyService.updateContainer(container);
                log.debug("Updated cache for container: " + container.getId() + " @ " + container.getLastUpdatedTimestamp().getTime());
                containersUpdated.add(container.getId());
            });
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
        String productFamilyId = instanceInfo.getMetadata().get("mfaas.discovery.catalogUiTile.id");
        if (productFamilyId == null) {
            log.error("Cannot create a tile without a parent id, the metadata for service: " + serviceId +
                " must contain an entry for mfaas.discovery.catalogUiTile.id");
        } else {
            APIContainer container = cachedProductFamilyService.createContainerFromInstance(productFamilyId, instanceInfo);
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
}
