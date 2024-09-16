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


import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.apicatalog.model.APIContainer;
import org.zowe.apiml.apicatalog.services.cached.CachedProductFamilyService;
import org.zowe.apiml.apicatalog.services.cached.CachedServicesService;
import org.zowe.apiml.apicatalog.util.ContainerServiceMockUtil;
import org.zowe.apiml.apicatalog.util.ContainerServiceState;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.product.gateway.GatewayClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static org.mockito.Mockito.*;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.CATALOG_ID;

class InstanceRefreshServiceTest {

    private final ContainerServiceMockUtil containerServiceMockUtil = new ContainerServiceMockUtil();

    private GatewayClient gatewayClient;
    private CachedProductFamilyService cachedProductFamilyService;
    private CachedServicesService cachedServicesService;
    private InstanceRetrievalService instanceRetrievalService;

    private InstanceRefreshService underTest;

    @BeforeEach
    void setup() {
        gatewayClient = mock(GatewayClient.class);
        cachedProductFamilyService = mock(CachedProductFamilyService.class);
        cachedServicesService = mock(CachedServicesService.class);
        instanceRetrievalService = mock(InstanceRetrievalService.class);

        underTest = new InstanceRefreshService(cachedProductFamilyService, cachedServicesService, instanceRetrievalService);
        underTest.start();

        addApiCatalogToCache();
    }

    @Nested
    class WhenRefreshingCacheFromDiscovery {
        @Nested
        class GivenValidCache {
            ContainerServiceState discoveredState;
            ContainerServiceState cachedState;

            @BeforeEach
            void prepareState() {
                cachedState = containerServiceMockUtil.createContainersServicesAndInstances();
                containerServiceMockUtil.mockServiceRetrievalFromCache(cachedServicesService, cachedState.getApplications());

                discoveredState = new ContainerServiceState();
                discoveredState.setServices(new ArrayList<>());
                discoveredState.setContainers(new ArrayList<>());
                discoveredState.setInstances(new ArrayList<>());
                discoveredState.setApplications(new ArrayList<>());
            }

            @Nested
            class AndServiceDoesntExist {
                InstanceInfo newInstanceOfService;

                @BeforeEach
                void prepareApplication() {
                     // start up a new instance of service 5 and add it to the service1 application
                    HashMap<String, String> metadata = new HashMap<>();
                    metadata.put(CATALOG_ID, "api-five");
                    newInstanceOfService
                        = containerServiceMockUtil.createInstance("service5", "service5:9999", InstanceInfo.InstanceStatus.UP,
                        InstanceInfo.ActionType.ADDED, metadata);
                    discoveredState.getInstances().add(newInstanceOfService);
                    Application service5 = new Application("service5", Collections.singletonList(newInstanceOfService));
                    discoveredState.getApplications().add(service5);

                    teachMocks();
                }

                @Test
                void addServiceToCache() {
                    when(cachedProductFamilyService.saveContainerFromInstance("api-five", newInstanceOfService))
                        .thenReturn(new APIContainer());

                    underTest.refreshCacheFromDiscovery();

                    verify(cachedProductFamilyService, times(1))
                        .saveContainerFromInstance("api-five", newInstanceOfService);
                }
            }

            @Nested
            class AndServiceAlreadyExists {
                Application service3;
                InstanceInfo changedInstanceOfService;

                @BeforeEach
                void prepareService() {
                    service3 = cachedState.getApplications()
                        .stream()
                        .filter(application -> application.getName().equalsIgnoreCase("service3"))
                        .toList().get(0);

                    changedInstanceOfService = service3.getInstances().get(0);
                    changedInstanceOfService.getMetadata().put(CATALOG_ID, "api-three");
                    service3.getInstances().add(0, changedInstanceOfService);
                    discoveredState.getApplications().add(service3);

                    teachMocks();

                }

                @Test
                void serviceIsRemovedFromCache() {
                    changedInstanceOfService.setActionType(InstanceInfo.ActionType.DELETED);

                    underTest.refreshCacheFromDiscovery();

                    verify(cachedProductFamilyService, times(1))
                        .removeInstance("api-three", changedInstanceOfService);
                    verify(cachedServicesService, never()).updateService(anyString(), any(Application.class));
                    verify(cachedProductFamilyService, never()).saveContainerFromInstance("api-three", changedInstanceOfService);
                }

                @Test
                void serviceIsModifiedInCache() {
                    changedInstanceOfService.setActionType(InstanceInfo.ActionType.MODIFIED);

                    APIContainer apiContainer3 = cachedState.getContainers()
                        .stream()
                        .filter(apiContainer -> apiContainer.getId().equals("api-three"))
                        .findFirst()
                        .orElse(new APIContainer());

                    when(cachedProductFamilyService.saveContainerFromInstance("api-three", changedInstanceOfService))
                        .thenReturn(apiContainer3);

                    underTest.refreshCacheFromDiscovery();

                    verify(cachedServicesService, times(1)).updateService(changedInstanceOfService.getAppName(), service3);
                    verify(cachedProductFamilyService, times(1))
                        .saveContainerFromInstance("api-three", changedInstanceOfService);
                }
            }

            void teachMocks() {
                // Mock the discovery and cached service query
                Applications discoveredServices = new Applications("123", 2L, discoveredState.getApplications());
                when(instanceRetrievalService.getAllInstancesFromDiscovery(true)).thenReturn(discoveredServices);
                Applications cachedServices = new Applications("456", 1L, cachedState.getApplications());
                when(cachedServicesService.getAllCachedServices()).thenReturn(cachedServices);
            }
        }

        @Nested
        class GivenClientIsNotInitialized {
            @Test
            void cacheIsntUpdated() {
                when(gatewayClient.isInitialized()).thenReturn(false);

                underTest.refreshCacheFromDiscovery();

                verify(cachedServicesService, never())
                    .updateService(anyString(), any(Application.class));
            }
        }

        @Nested
        class GivenApiCatalogIsntInCache {
            @Test
            void cacheIsntUpdated() {
                when(cachedServicesService.getService(CoreService.API_CATALOG.getServiceId())).thenReturn(null);

                underTest.refreshCacheFromDiscovery();

                verify(cachedServicesService, never())
                    .updateService(anyString(), any(Application.class));
            }
        }
    }


    private void addApiCatalogToCache() {
        InstanceInfo apiCatalogInstance = containerServiceMockUtil.createInstance(
            CoreService.API_CATALOG.getServiceId(),
            "service:9999",
            InstanceInfo.InstanceStatus.UP,
            InstanceInfo.ActionType.ADDED,
            new HashMap<>());

        when(cachedServicesService.getService(CoreService.API_CATALOG.getServiceId()))
            .thenReturn(
                new Application(CoreService.API_CATALOG.getServiceId(),
                    Collections.singletonList(apiCatalogInstance)
                )
            );
    }
}
