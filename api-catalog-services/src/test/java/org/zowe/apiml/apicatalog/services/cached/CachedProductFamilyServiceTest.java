/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.services.cached;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.apicatalog.model.APIContainer;
import org.zowe.apiml.apicatalog.model.APIService;
import org.zowe.apiml.apicatalog.model.CustomStyleConfig;
import org.zowe.apiml.apicatalog.util.ServicesBuilder;
import org.zowe.apiml.product.routing.RoutedServices;
import org.zowe.apiml.product.routing.ServiceType;
import org.zowe.apiml.product.routing.transform.TransformService;
import org.zowe.apiml.product.routing.transform.URLTransformationException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.zowe.apiml.constants.EurekaMetadataDefinition.*;

/**
 * Container is used the same way as Tile
 */
@SuppressWarnings({ "squid:S2925" }) // replace with proper wait test library
class CachedProductFamilyServiceTest {

    private static final String SERVICE_ID = "service_test_id";

    private final Integer cacheRefreshUpdateThresholdInMillis = 2000;

    private CachedProductFamilyService underTest;

    private TransformService transformService;
    private CachedServicesService cachedServicesService;
    private ServicesBuilder servicesBuilder;
    private CustomStyleConfig customStyleConfig;
    @BeforeEach
    void setUp() {
        cachedServicesService = mock(CachedServicesService.class);
        transformService = mock(TransformService.class);
        customStyleConfig = mock(CustomStyleConfig.class);

        underTest = new CachedProductFamilyService(
                cachedServicesService,
                transformService,
                cacheRefreshUpdateThresholdInMillis, customStyleConfig);

        servicesBuilder = new ServicesBuilder(underTest);
    }

    @Nested
    class WhenCallSaveContainerFromInstance {
        @Nested
        class AndWhenCachedInstance {
            private InstanceInfo instance;
            private InstanceInfo updatedInstance;

            @BeforeEach
            void prepareInstances() throws URLTransformationException {
                Map<String, String> metadata = new HashMap<>();
                metadata.put(CATALOG_ID, "demoapp");
                metadata.put(CATALOG_TITLE, "Title");
                metadata.put(CATALOG_DESCRIPTION, "Description");
                metadata.put(CATALOG_VERSION, "1.0.0");
                metadata.put(SERVICE_TITLE, "sTitle");
                metadata.put(SERVICE_DESCRIPTION, "sDescription");
                instance = servicesBuilder.createInstance("service1", InstanceInfo.InstanceStatus.UP, metadata);

                Map<String, String> updatedMetadata = new HashMap<>();
                updatedMetadata.put(CATALOG_ID, "demoapp");
                updatedMetadata.put(CATALOG_TITLE, "Title2");
                updatedMetadata.put(CATALOG_DESCRIPTION, "Description2");
                updatedMetadata.put(CATALOG_VERSION, "2.0.0");
                updatedMetadata.put(SERVICE_TITLE, "sTitle2");
                updatedMetadata.put(SERVICE_DESCRIPTION, "sDescription2");
                updatedInstance = servicesBuilder.createInstance("service1", InstanceInfo.InstanceStatus.UP,
                        updatedMetadata);

                when(transformService.transformURL(
                        any(ServiceType.class), any(String.class), any(String.class), any(RoutedServices.class), eq(false)))
                                .thenReturn(instance.getHomePageUrl());
            }

            @Nested
            class GivenInstanceIsNotInCache {
                @Test
                void createNew() {
                    APIContainer originalContainer = underTest.saveContainerFromInstance("demoapp", instance);

                    List<APIContainer> lsContainer = underTest.getRecentlyUpdatedContainers();
                    assertThatContainerIsCorrect(lsContainer, originalContainer, instance);
                }
            }

            @Nested
            class GivenInstanceIsInTheCache {
                @Test
                void update() throws InterruptedException {
                    APIContainer originalContainer = underTest.saveContainerFromInstance("demoapp", instance);
                    Calendar createdTimestamp = originalContainer.getLastUpdatedTimestamp();

                    Thread.sleep(100);

                    APIContainer updatedContainer = underTest.saveContainerFromInstance("demoapp", updatedInstance);
                    Calendar updatedTimestamp = updatedContainer.getLastUpdatedTimestamp();

                    assertThat(updatedTimestamp, is(not(createdTimestamp)));

                    List<APIContainer> lsContainer = underTest.getRecentlyUpdatedContainers();
                    assertThatContainerIsCorrect(lsContainer, updatedContainer, updatedInstance);
                }
            }
        }

        private void assertThatContainerIsCorrect(List<APIContainer> lsContainer, APIContainer containerToVerify,
                InstanceInfo instance) {
            assertThat(lsContainer.size(), is(1));
            assertThatMetadataAreCorrect(containerToVerify, instance.getMetadata());

            Set<APIService> apiServices = containerToVerify.getServices();
            assertThat(apiServices.size(), is(1));
            assertThatInstanceIsCorrect(apiServices.iterator().next(), instance);
        }

        private void assertThatInstanceIsCorrect(APIService result, InstanceInfo correct) {
            assertThat(result.getServiceId(), is(correct.getAppName().toLowerCase()));
            assertThat(result.isSecured(), is(correct.isPortEnabled(InstanceInfo.PortType.SECURE)));
            assertThat(result.getHomePageUrl(), is(correct.getHomePageUrl()));
        }

        private void assertThatMetadataAreCorrect(APIContainer result, Map<String, String> correct) {
            assertThat(result.getId(), is(correct.get(CATALOG_ID)));
            assertThat(result.getTitle(), is(correct.get(CATALOG_TITLE)));
            assertThat(result.getDescription(), is(correct.get(CATALOG_DESCRIPTION)));
            assertThat(result.getVersion(), is(correct.get(CATALOG_VERSION)));
        }
    }

    @Nested
    class WhenRemovingService {
        @Nested
        class GivenServiceExists {
            InstanceInfo removedInstance;
            String removedInstanceFamilyId = "service1";

            @BeforeEach
            void prepareExistingInstance() {
                Map<String, String> metadata = new HashMap<>();
                metadata.put(CATALOG_ID, "demoapp");
                metadata.put(CATALOG_TITLE, "Title");
                metadata.put(CATALOG_DESCRIPTION, "Description");
                metadata.put(CATALOG_VERSION, "1.0.0");
                metadata.put(SERVICE_TITLE, "sTitle");
                metadata.put(SERVICE_DESCRIPTION, "sDescription");
                removedInstance = servicesBuilder.createInstance("service1", InstanceInfo.InstanceStatus.UP, metadata);

                underTest.saveContainerFromInstance(removedInstanceFamilyId, removedInstance);
            }

            @Nested
            class AndWholeTileIsRemoved {
                @Test
                void tileIsntPresentInCache() {
                    underTest.removeInstance(removedInstanceFamilyId, removedInstance);

                    // The container should be removed
                    APIContainer receivedContainer = underTest.getContainerById(removedInstanceFamilyId);
                    assertThat(receivedContainer, is(nullValue()));
                }
            }

            @Nested
            class AndTileRemains {
                InstanceInfo remainingInstance;

                @Nested
                class AndTheInstancesAreFromTheSameService {
                    @BeforeEach
                    void prepareTileWithMultipleInstancesOfSameService() {
                        underTest.saveContainerFromInstance(removedInstanceFamilyId, removedInstance);

                        Map<String, String> metadata = new HashMap<>();
                        metadata.put(CATALOG_ID, "demoapp");
                        metadata.put(CATALOG_TITLE, "Title");
                        metadata.put(CATALOG_DESCRIPTION, "Description");
                        metadata.put(CATALOG_VERSION, "1.0.0");
                        metadata.put(SERVICE_TITLE, "sTitle");
                        metadata.put(SERVICE_DESCRIPTION, "sDescription");
                        remainingInstance = servicesBuilder.createInstance("service1", InstanceInfo.InstanceStatus.UP, metadata);
                        underTest.saveContainerFromInstance(removedInstanceFamilyId, remainingInstance);
                    }

                    @Test
                    void tileIsInCacheButServiceIsntInTile() {
                        underTest.removeInstance(removedInstanceFamilyId, removedInstance);

                        APIContainer result = underTest.getContainerById(removedInstanceFamilyId);
                        assertThat(result, is(not(nullValue())));

                        Set<APIService> remainingServices =  result.getServices();
                        assertThat(remainingServices.size(), is(1));
                        APIService remainingService = remainingServices.iterator().next();
                        assertThat(remainingService.getInstances().size(), is(1));
                        assertThat(remainingService.getInstances().get(0), is("service13"));
                    }
                }

                @Nested
                class AndTheInstancesAreFromDifferentService {
                    @BeforeEach
                    void prepareTileWithMultipleInstancesOfSameService() {
                        underTest.saveContainerFromInstance(removedInstanceFamilyId, removedInstance);

                        Map<String, String> metadata = new HashMap<>();
                        metadata.put(CATALOG_ID, "demoapp");
                        metadata.put(CATALOG_TITLE, "Title");
                        metadata.put(CATALOG_DESCRIPTION, "Description");
                        metadata.put(CATALOG_VERSION, "1.0.0");
                        metadata.put(SERVICE_TITLE, "sTitle");
                        metadata.put(SERVICE_DESCRIPTION, "sDescription");
                        remainingInstance = servicesBuilder.createInstance("service2", InstanceInfo.InstanceStatus.UP, metadata);
                        underTest.saveContainerFromInstance(removedInstanceFamilyId, remainingInstance);
                    }

                    @Test
                    void tileIsInCacheButServiceIsntInTile() {
                        underTest.removeInstance(removedInstanceFamilyId, removedInstance);

                        APIContainer result = underTest.getContainerById(removedInstanceFamilyId);
                        assertThat(result, is(not(nullValue())));

                        Set<APIService> remainingServices =  result.getServices();
                        assertThat(remainingServices.size(), is(1));

                        APIService remainingService = remainingServices.iterator().next();
                        assertThat(remainingService.getServiceId(), is("service2"));
                    }
                }
            }

            @Nested
            class GivenRemovingNonExistentService {
                @Test
                void nothingHappens() {
                    underTest.removeInstance("nonexistent", removedInstance);

                    APIContainer result = underTest.getContainerById(removedInstanceFamilyId);
                    assertThat(result, is(not(nullValue())));
                }

            }
        }
    }

    @Nested
    class WhenRetrievingUpdatedContainers {
        @Nested
        class GivenExistingTilesAreValid {
            @Test
            void returnExistingTiles() {
                underTest.saveContainerFromInstance("demoapp", servicesBuilder.instance1);
                underTest.saveContainerFromInstance("demoapp2", servicesBuilder.instance2);

                Collection<APIContainer> containers = underTest.getRecentlyUpdatedContainers();
                assertEquals(2, containers.size());
            }
        }

        @Nested
        class GivenOneTileIsTooOld {
            @Test
            void returnOnlyOneService() throws InterruptedException {
                // To speed up the test, create instance which consider even 5 milliseconds as
                // old.
                underTest = new CachedProductFamilyService(
                        null,
                        transformService,
                        5, null);
                // This is considered as old update.
                underTest.saveContainerFromInstance("demoapp", servicesBuilder.instance1);

                Thread.sleep(10);

                underTest.saveContainerFromInstance("demoapp2", servicesBuilder.instance2);

                Collection<APIContainer> containers = underTest.getRecentlyUpdatedContainers();
                assertEquals(1, containers.size());
            }
        }
    }

    @Nested
    class WhenCalculatingContainerTotals {
        @Nested
        class AndStatusIsInvolved {
            InstanceInfo instance1;
            InstanceInfo instance2;

            @BeforeEach
            void prepareApplications() {
                instance1 = servicesBuilder.createInstance("service1", "demoapp");
                instance2 = servicesBuilder.createInstance("service2", "demoapp");
                Application application1 = new Application();
                application1.addInstance(instance1);
                Application application2 = new Application();
                application2.addInstance(instance2);

                when(cachedServicesService.getService("service1")).thenReturn(application1);
                when(cachedServicesService.getService("service2")).thenReturn(application2);
                underTest = new CachedProductFamilyService(
                        cachedServicesService,
                        transformService,
                        cacheRefreshUpdateThresholdInMillis, null);
            }

            @Nested
            class GivenAllServicesAreUp {
                @Test
                void containerStatusIsUp() {
                    underTest.saveContainerFromInstance("demoapp", instance1);
                    underTest.addServiceToContainer("demoapp", instance2);

                    APIContainer container = underTest.getContainerById("demoapp");
                    assertNotNull(container);

                    underTest.calculateContainerServiceValues(container);
                    assertThatContainerHasValidState(container, "UP", 2);
                }
            }

            @Nested
            class GivenAllServicesAreDown {
                @Test
                void containerStatusIsDown() {
                    instance1.setStatus(InstanceInfo.InstanceStatus.DOWN);
                    instance2.setStatus(InstanceInfo.InstanceStatus.DOWN);

                    underTest.saveContainerFromInstance("demoapp", instance1);
                    underTest.addServiceToContainer("demoapp", instance2);

                    APIContainer container = underTest.getContainerById("demoapp");
                    assertNotNull(container);

                    underTest.calculateContainerServiceValues(container);
                    assertThatContainerHasValidState(container, "DOWN", 0);
                }
            }

            @Nested
            class GivenSomeServicesAreDown {
                @Test
                void containerStatusIsWarning() {
                    instance2.setStatus(InstanceInfo.InstanceStatus.DOWN);

                    underTest.saveContainerFromInstance("demoapp", instance1);
                    underTest.addServiceToContainer("demoapp", instance2);

                    APIContainer container = underTest.getContainerById("demoapp");
                    assertNotNull(container);

                    underTest.calculateContainerServiceValues(container);
                    assertThatContainerHasValidState(container, "WARNING", 1);
                }
            }
        }

        @Nested
        class GivenMultipleApiIds {
            @Test
            void groupThem() {
                Application application = servicesBuilder.createApp(
                        SERVICE_ID,
                        servicesBuilder.createInstance(SERVICE_ID, "catalog1",
                                Pair.of("apiml.apiInfo.api-v1.apiId", "api1"),
                                Pair.of("apiml.apiInfo.api-v1.version", "1.0.0"),
                                Pair.of("apiml.apiInfo.api-v2.apiId", "api2"),
                                Pair.of("apiml.apiInfo.api-v2.version", "2"),
                                Pair.of("apiml.apiInfo.api-v3.apiId", "api3")));
                doReturn(application).when(cachedServicesService).getService(SERVICE_ID);
                APIContainer apiContainer = underTest.getContainerById(SERVICE_ID);
                underTest.calculateContainerServiceValues(apiContainer);

                APIService apiService = apiContainer.getServices().iterator().next();
                assertNotNull(apiService.getApis());
                assertEquals(3, apiService.getApis().size());
                assertNotNull(apiService.getApis().get("api1 v1.0.0"));
                assertNotNull(apiService.getApis().get("api2 v2"));
                assertNotNull(apiService.getApis().get("default"));
            }
        }

        @Nested
        class AndSsoInvolved {

            @Nested
            class GivenSsoAndNonSsoInstances {
                @Test
                void returnNonSso() {
                    Application application = servicesBuilder.createApp(
                            SERVICE_ID,
                            servicesBuilder.createInstance(SERVICE_ID, "catalog1",
                                    Pair.of(AUTHENTICATION_SCHEME, "bypass")),
                            servicesBuilder.createInstance(SERVICE_ID, "catalog2",
                                    Pair.of(AUTHENTICATION_SCHEME, "zoweJwt")));
                    doReturn(application).when(cachedServicesService).getService(SERVICE_ID);

                    APIContainer apiContainer = underTest.getContainerById(SERVICE_ID);
                    underTest.calculateContainerServiceValues(apiContainer);

                    assertFalse(apiContainer.isSso());
                    for (APIService apiService : apiContainer.getServices()) {
                        assertFalse(apiService.isSsoAllInstances());
                    }
                }
            }

            @Nested
            class GivenAllInstancesAreSso {
                @Test
                void returnSso() {
                    InstanceInfo instanceInfo = servicesBuilder.createInstance(SERVICE_ID, "catalog1",
                            Pair.of(AUTHENTICATION_SCHEME, "zoweJwt"));
                    doReturn(servicesBuilder.createApp(SERVICE_ID, instanceInfo)).when(cachedServicesService)
                            .getService(SERVICE_ID);
                    APIContainer apiContainer = underTest.getContainerById(SERVICE_ID);
                    underTest.calculateContainerServiceValues(apiContainer);

                    assertTrue(apiContainer.isSso());
                    for (APIService apiService : apiContainer.getServices()) {
                        assertTrue(apiService.isSso());
                        assertTrue(apiService.isSsoAllInstances());
                    }
                }
            }
        }

        @Nested
        class GivenHideServiceInfo {
            @Test
            void thenSetToApiService() {
                InstanceInfo instanceInfo = servicesBuilder.createInstance(SERVICE_ID, "catalog1",
                    Pair.of(AUTHENTICATION_SCHEME, "zoweJwt"));
                doReturn(servicesBuilder.createApp(SERVICE_ID, instanceInfo)).when(cachedServicesService)
                    .getService(SERVICE_ID);
                APIContainer apiContainer = underTest.getContainerById(SERVICE_ID);
                ReflectionTestUtils.setField(underTest, "hideServiceInfo", true);
                underTest.calculateContainerServiceValues(apiContainer);
                assertTrue(apiContainer.isHideServiceInfo());
            }
        }

        void assertThatContainerHasValidState(APIContainer container, String state, int activeServices) {
            assertNotNull(container);

            underTest.calculateContainerServiceValues(container);
            assertEquals(state, container.getStatus());
            assertEquals(2, container.getTotalServices().intValue());
            assertEquals(activeServices, container.getActiveServices().intValue());
        }
    }

    @Nested
    class WhenGettingContainerWithoutServiceInstances {
        @Test
        void noServicesAreWithinTheContainer() {
            assertThat(underTest.getContainerCount(), is(0));
            assertThat(underTest.getAllContainers().size(), is(0));
        }
    }
}
