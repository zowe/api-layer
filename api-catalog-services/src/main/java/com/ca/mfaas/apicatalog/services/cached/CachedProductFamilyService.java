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

import com.ca.mfaas.product.constants.CoreService;
import com.ca.mfaas.product.gateway.GatewayConfigProperties;
import com.ca.mfaas.apicatalog.metadata.EurekaMetadataParser;
import com.ca.mfaas.apicatalog.model.APIContainer;
import com.ca.mfaas.apicatalog.model.APIService;
import com.ca.mfaas.apicatalog.model.SemanticVersion;
import com.ca.mfaas.product.routing.RoutedServices;
import com.ca.mfaas.product.routing.ServiceType;
import com.ca.mfaas.product.routing.transform.TransformService;
import com.ca.mfaas.product.routing.transform.URLTransformationException;
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

    private static final String CATALOG_UI_TITLE_KEY = "mfaas.discovery.catalogUiTile.title";
    private static final String CATALOG_UI_DESCRIPTION_KEY = "mfaas.discovery.catalogUiTile.description";
    private static final String CATALOG_UI_VERSION_KEY = "mfaas.discovery.catalogUiTile.version";

    private final Map<String, APIContainer> products = new HashMap<>();

    private final CachedServicesService cachedServicesService;
    private final Integer cacheRefreshUpdateThresholdInMillis;
    private final EurekaMetadataParser metadataParser = new EurekaMetadataParser();
    private final TransformService transformService;

    @Autowired
    public CachedProductFamilyService(@Lazy GatewayConfigProperties gatewayConfigProperties,
                                      CachedServicesService cachedServicesService,
                                      @Value("${mfaas.service-registry.cacheRefreshUpdateThresholdInMillis}")
                                          Integer cacheRefreshUpdateThresholdInMillis) {
        this.cachedServicesService = cachedServicesService;
        this.cacheRefreshUpdateThresholdInMillis = cacheRefreshUpdateThresholdInMillis;
        this.transformService = new TransformService(gatewayConfigProperties);
    }

    /**
     * Return all cached service instances
     *
     * @return instances
     */
    @Cacheable
    public Collection<APIContainer> getAllContainers() {
        return products.values();
    }


    /**
     * return cached service instance by id
     *
     * @param id service identifier
     * @return {@link APIContainer}
     */
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

    /**
     * Add service to container
     *
     * @param productFamilyId the service identifier
     * @param instanceInfo    InstanceInfo
     */
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


    /**
     * Try to transform the service homepage url and return it. If it fails,
     * return the original homepage url
     *
     * @param instanceInfo the service instance
     * @return the transformed homepage url
     */
    private String getInstanceHomePageUrl(InstanceInfo instanceInfo) {
        String instanceHomePage = instanceInfo.getHomePageUrl();

        //Gateway homePage is used to hold DVIPA address and must not be modified
        if (instanceHomePage != null
            && !instanceHomePage.isEmpty()
            && !instanceInfo.getAppName().equalsIgnoreCase(CoreService.GATEWAY.getServiceId())) {
            RoutedServices routes = metadataParser.parseRoutes(instanceInfo.getMetadata());

            try {
                instanceHomePage = transformService.transformURL(
                    ServiceType.UI,
                    instanceInfo.getVIPAddress(),
                    instanceHomePage,
                    routes);
            } catch (URLTransformationException e) {
                log.warn("The home page URI was not transformed. {}",e.getMessage());
            }
        }

        log.debug("Homepage URL for {} service is: {}", instanceInfo.getVIPAddress(), instanceHomePage);
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
        Map<String, String> instanceInfoMetadata = instanceInfo.getMetadata();
        String title = instanceInfoMetadata.get(CATALOG_UI_TITLE_KEY);
        String description = instanceInfoMetadata.get(CATALOG_UI_DESCRIPTION_KEY);
        String version = instanceInfoMetadata.get(CATALOG_UI_VERSION_KEY);
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
        String versionFromInstance = instanceInfo.getMetadata().get(CATALOG_UI_VERSION_KEY);
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
                String title = instanceInfo.getMetadata().get(CATALOG_UI_TITLE_KEY);
                String description = instanceInfo.getMetadata().get(CATALOG_UI_DESCRIPTION_KEY);
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
    private APIService createAPIServiceFromInstance(InstanceInfo instanceInfo) {
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
            apiServices.remove(service);

            apiServices.add(service);
            container.setServices(apiServices);
            //update container
            String versionFromInstance = instanceInfo.getMetadata().get(CATALOG_UI_VERSION_KEY);
            String title = instanceInfo.getMetadata().get(CATALOG_UI_TITLE_KEY);
            String description = instanceInfo.getMetadata().get(CATALOG_UI_DESCRIPTION_KEY);

            container.setVersion(versionFromInstance);
            container.setTitle(title);
            container.setDescription(description);
            container.updateLastUpdatedTimestamp();

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
