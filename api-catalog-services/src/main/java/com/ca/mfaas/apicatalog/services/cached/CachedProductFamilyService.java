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

import com.ca.mfaas.apicatalog.gateway.GatewayConfigProperties;
import com.ca.mfaas.apicatalog.metadata.EurekaMetadataParser;
import com.ca.mfaas.apicatalog.model.APIContainer;
import com.ca.mfaas.apicatalog.model.APIService;
import com.ca.mfaas.apicatalog.model.SemanticVersion;
import com.ca.mfaas.product.routing.RoutedServices;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.toList;

/**
 * Caching service for eureka services
 */
@Slf4j
@Service
@CacheConfig(cacheNames = {"products"})
public class CachedProductFamilyService {

    private final Map<String, APIContainer> products = new HashMap<>();

    private final GatewayConfigProperties gatewayConfigProperties;
    private final CachedServicesService cachedServicesService;
    private final Integer cacheRefreshUpdateThresholdInMillis;
    private final EurekaMetadataParser metadataParser = new EurekaMetadataParser();

    @Autowired
    public CachedProductFamilyService(@Lazy GatewayConfigProperties gatewayConfigProperties,
                                      CachedServicesService cachedServicesService,
                                      @Value("${mfaas.service-registry.cacheRefreshUpdateThresholdInMillis}")
                                      Integer cacheRefreshUpdateThresholdInMillis) {
        this.gatewayConfigProperties = gatewayConfigProperties;
        this.cachedServicesService = cachedServicesService;
        this.cacheRefreshUpdateThresholdInMillis = cacheRefreshUpdateThresholdInMillis;
    }

    /**
     * return all cached service instances
     *
     * @return instances
     */
    @Cacheable
    public Collection<APIContainer> getAllContainers() {
        return products.values();
    }

    public APIContainer getContainerById(String id) {
        return products.get(id);
    }

    /**
     * Retrieve any containers which have had their details updated after the threshold figure
     * If performance is slow then possibly cache the result and evict after 'n' seconds
     *
     * @return recently updated containers
     */
    public List<APIContainer> getRecentlyUpdatedContainers() {
        return this.products.values().stream().filter(
            container -> {
                boolean isRecent = container.isRecentUpdated(cacheRefreshUpdateThresholdInMillis);
                if (isRecent) {
                    log.debug("Container: " + container.getId() + " last updated: "
                        + container.getLastUpdatedTimestamp().getTime() +
                        " was updated recently");
                }
                return isRecent;
            }).collect(toList());
    }

    /**
     * return a cached service instance from a container
     *
     * @param productFamilyId the service identifier
     * @return instances for this service (might be empty instances collection)
     */
    @Cacheable(key = "#productFamilyId+ #instanceInfo.appName")
    public APIService getContainerService(final String productFamilyId, final InstanceInfo instanceInfo) {
        APIContainer apiContainer = products.get(productFamilyId.toLowerCase());
        Optional<APIService> result = apiContainer.getServices().stream()
            .filter(service -> instanceInfo.getAppName().equalsIgnoreCase(service.getServiceId()))
            .findFirst();
        return result.orElse(null);
    }

    @CachePut(key = "#productFamilyId")
    public void addServiceToContainer(final String productFamilyId, final InstanceInfo instanceInfo) {
        APIContainer apiContainer = products.get(productFamilyId);
        // fix - throw error if null
        apiContainer.addService(createAPIServiceFromInstance(instanceInfo));
        products.put(productFamilyId, apiContainer);
    }

    /**
     * Retrieve a container from the cache
     *
     * @param productFamilyId the product family id
     * @return a container
     */
    @Cacheable(key = "#productFamilyId", sync = true)
    public APIContainer getContainer(final String productFamilyId, @NonNull InstanceInfo instanceInfo) {
        return createContainerFromInstance(productFamilyId, instanceInfo);
    }

    /**
     * Return an uncached container for a given family id
     *
     * @param productFamilyId find a container with this family id
     * @return a container (or null)
     */
    public APIContainer retrieveContainer(@NonNull final String productFamilyId) {
        return this.products.get(productFamilyId);
    }

    /**
     * Return any containers which have the given service registered
     *
     * @param serviceId check for this service
     * @return a list of containers
     */
    public List<APIContainer> getContainersForService(final String serviceId) {
        return this.products.values().stream().filter(
            container -> container.getServices().stream().anyMatch(service -> serviceId.equalsIgnoreCase(service.getServiceId()))
        ).collect(toList());
    }


    /**
     * Create a container from a service instance and add it to the cache
     * or update an existing container if the instance version number has increased
     *
     * @param productFamilyId the product family id
     * @param instanceInfo    the service instance
     */
    @CachePut(key = "#productFamilyId")
    public APIContainer createContainerFromInstance(final String productFamilyId, InstanceInfo instanceInfo) {
        APIContainer container = products.get(productFamilyId);
        if (container == null) {
            container = createNewContainerFromService(productFamilyId, instanceInfo);
        } else {
            addServiceToContainer(productFamilyId, instanceInfo);
            container = products.get(productFamilyId);
            checkIfContainerShouldBeUpdatedFromInstance(instanceInfo, container);
        }
        return container;
    }

    private String getInstanceHomePageUrl(InstanceInfo instanceInfo) {
        String instanceHomePage = null;
        if (instanceInfo.getHomePageUrl() != null && !instanceInfo.getHomePageUrl().isEmpty()) {
            RoutedServices routes = metadataParser.parseRoutes(instanceInfo.getMetadata());
            String uiServiceRoute = routes.findServiceByGatewayUrl("ui/v1").getServiceUrl();
            URI receivedHomePage = URI.create(instanceInfo.getHomePageUrl());
            String path = receivedHomePage.getPath();
            path = path.replace(uiServiceRoute, "");
            instanceHomePage = String.format("%s://%s/ui/v1/%s%s",
                gatewayConfigProperties.getScheme(),
                gatewayConfigProperties.getHostname(),
                instanceInfo.getVIPAddress(),
                path);
        }
        log.debug("Homepage URL for %s service is: %s", instanceInfo.getVIPAddress(), instanceHomePage);
        return instanceHomePage;
    }

    /**
     * Create a new container based on information in a new instance
     *
     * @param productFamilyId parent id
     * @param instanceInfo    instance
     * @return a new container
     */
    private APIContainer createNewContainerFromService(String productFamilyId, InstanceInfo instanceInfo) {
        String title = instanceInfo.getMetadata().get("mfaas.discovery.catalogUiTile.title");
        String description = instanceInfo.getMetadata().get("mfaas.discovery.catalogUiTile.description");
        String version = instanceInfo.getMetadata().get("mfaas.discovery.catalogUiTile.version");

        APIContainer container = new APIContainer();
        container.setStatus("UP");
        container.setId(productFamilyId);
        container.setDescription(description);
        container.setTitle(title);
        container.setVersion(version);
        log.debug("updated Container cache with product family: " + productFamilyId + ": " + title);

        // create API Service from instance and update container last changed date
        container.addService(createAPIServiceFromInstance(instanceInfo));
        products.put(productFamilyId, container);
        return container;
    }

    /**
     * Compare the version of the parent in the given instance
     * If the version is greater, then update the parent
     *
     * @param instanceInfo service instance
     * @param container    parent container
     */
    private void checkIfContainerShouldBeUpdatedFromInstance(InstanceInfo instanceInfo, APIContainer container) {
        String versionFromInstance = instanceInfo.getMetadata().get("mfaas.discovery.catalogUiTile.version");
        // if the instance has a parent version
        if (versionFromInstance != null) {
            final SemanticVersion instanceVer = new SemanticVersion(versionFromInstance);
            SemanticVersion containerVer;
            if (container.getVersion() == null) {
                containerVer = new SemanticVersion("0.0.0");
            } else {
                containerVer = new SemanticVersion(container.getVersion());
            }

            // Only update if the instance version is greater than the container version
            int result = instanceVer.compareTo(containerVer);
            if (result > 0) {
                container.setVersion(versionFromInstance);
                String title = instanceInfo.getMetadata().get("mfaas.discovery.catalogUiTile.title");
                String description = instanceInfo.getMetadata().get("mfaas.discovery.catalogUiTile.description");
                if (!container.getTitle().equals(title)) {
                    container.setTitle(title);
                }
                if (!container.getDescription().equals(description)) {
                    container.setDescription(description);
                }
                container.updateLastUpdatedTimestamp();
            }
        }
    }

    /**
     * Create a APIService object using the instances metadata
     *
     * @param instanceInfo the service instance
     * @return a APIService object
     */
    public APIService createAPIServiceFromInstance(InstanceInfo instanceInfo) {
        boolean secureEnabled = instanceInfo.isPortEnabled(InstanceInfo.PortType.SECURE);

        String instanceHomePage = getInstanceHomePageUrl(instanceInfo);
        return new APIService(
            instanceInfo.getAppName().toLowerCase(),
            instanceInfo.getMetadata().get("mfaas.discovery.service.title"),
            instanceInfo.getMetadata().get("mfaas.discovery.service.description"),
            secureEnabled, instanceHomePage);
    }

    /**
     * Update a containers details using a service's metadata
     *
     * @param productFamilyId the product family id of the container
     * @param instanceInfo    the service instance
     */
    @CacheEvict(key = "#productFamilyId")
    public void updateContainerFromInstance(String productFamilyId, InstanceInfo instanceInfo) {
        createContainerFromInstance(productFamilyId, instanceInfo);
    }

    /**
     * Save a containers details using a service's metadata
     *
     * @param productFamilyId the product family id of the container
     * @param instanceInfo    the service instance
     */
    @CachePut(key = "#productFamilyId")
    public APIContainer saveContainerFromInstance(String productFamilyId, InstanceInfo instanceInfo) {
        APIContainer container = products.get(productFamilyId);
        if (container == null) {
            container = createNewContainerFromService(productFamilyId, instanceInfo);
        } else {
            Set<APIService> apiServices = container.getServices();
            APIService service = createAPIServiceFromInstance(instanceInfo);
            apiServices.add(service);
            container.setServices(apiServices);
            //update container
            checkIfContainerShouldBeUpdatedFromInstance(instanceInfo, container);
            products.put(productFamilyId, container);
        }

        return container;
    }

    /**
     * Remove a container
     *
     * @param productFamilyId the product family id of the container
     * @param serviceId       check for this service
     */
    @CacheEvict(key = "#productFamilyId")
    public void removeContainerFromInstance(String productFamilyId, String serviceId) {
        APIContainer container = products.get(productFamilyId);
        if (container != null) {
            Set<APIService> apiServices = container.getServices();
            apiServices.remove(new APIService(serviceId));

            if (apiServices.isEmpty()) {
                products.remove(productFamilyId);
            } else {
                container.setServices(apiServices);
                products.put(container.getId(), container);
            }
        }
    }

    /**
     * Update the summary totals for a container based on it's running services
     *
     * @param apiContainer calculate totals for this container
     */
    public void calculateContainerServiceTotals(APIContainer apiContainer) {
        final AtomicInteger activeServices = new AtomicInteger(0);
        if (apiContainer.getServices() != null) {
            activeServices.set(apiContainer.getServices().size());
            apiContainer.getServices().forEach(apiService -> {
                Application service = this.cachedServicesService.getService(apiService.getServiceId());
                // only use running instances
                if (service != null) {
                    long numInstances = service.getInstances().stream().filter(
                        instance -> instance.getStatus().equals(InstanceInfo.InstanceStatus.UP)).count();
                    if (numInstances == 0) {
                        activeServices.getAndDecrement();
                        apiService.setStatus("DOWN");
                    } else {
                        apiService.setStatus("UP");
                    }
                }
            });
        }

        // set counters for total and active services
        apiContainer.setTotalServices(apiContainer.getServices() == null ? 0 : apiContainer.getServices().size());
        apiContainer.setActiveServices(activeServices.get());

        if (activeServices.get() == 0) {
            apiContainer.setStatus("DOWN");
        } else if (activeServices.get() < apiContainer.getServices().size()) {
            apiContainer.setStatus("WARNING");
        } else {
            apiContainer.setStatus("UP");
        }
    }

    /**
     * Return the number of containers (used for checking if a new container was created)
     *
     * @return the number of containers
     */
    public int getContainerCount() {
        return products.size();
    }
}
