/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.swagger;

import com.netflix.appinfo.InstanceInfo;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaServiceInstance;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.apicatalog.model.APIContainer;
import org.zowe.apiml.apicatalog.model.APIService;
import org.zowe.apiml.apicatalog.model.CustomStyleConfig;
import org.zowe.apiml.apicatalog.util.ServicesBuilder;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.product.instance.ServiceAddress;
import org.zowe.apiml.product.routing.transform.TransformService;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.*;

@ExtendWith(MockitoExtension.class)
class ContainerServiceTest {

    @Nested
    class WhenCalculatingContainerTotals {

        private static final String SERVICE_ID = "service_test_id";

        private DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
        private TransformService transformService = new TransformService(new GatewayClient(ServiceAddress.builder().scheme("https").hostname("localhost").build()));
        private CustomStyleConfig customStyleConfig = new CustomStyleConfig();

        private ServiceInstance serviceInstance1;
        private ServiceInstance serviceInstance2;
        private ContainerService containerService;

        @BeforeEach
        void prepareApplications() {
            serviceInstance1 = ServicesBuilder.createInstance("service1", "demoapp");
            serviceInstance2 = ServicesBuilder.createInstance("service2", "demoapp");

            when(discoveryClient.getInstances("service1")).thenReturn(Collections.singletonList(serviceInstance1));
            when(discoveryClient.getInstances("service2")).thenReturn(Collections.singletonList(serviceInstance2));
            when(discoveryClient.getServices()).thenReturn(Arrays.asList("service1", "service2"));
            containerService = new ContainerService(
                discoveryClient,
                transformService,
                customStyleConfig
            );
        }

        @Nested
        class AndStatusIsInvolved {

            void assertThatContainerHasValidState(APIContainer container, String state, int activeServices) {
                assertNotNull(container);

                assertEquals(state, container.getStatus());
                assertEquals(2, container.getTotalServices().intValue());
                assertEquals(activeServices, container.getActiveServices().intValue());
            }

            @Test
            void givenNoServiceId_getGetContainerById_thenReturnNull() {
                assertNull(containerService.getContainerById(null));
            }

            @Nested
            class GivenAllServicesAreUp {

                @Test
                void containerStatusIsUp() {
                    APIContainer container = containerService.getContainerById("demoapp");
                    assertNotNull(container);

                    assertThatContainerHasValidState(container, "UP", 2);
                }

            }

            @Nested
            class GivenAllServicesAreDown {

                @Test
                void containerStatusIsDown() {
                    ((EurekaServiceInstance) serviceInstance1).getInstanceInfo().setStatus(InstanceInfo.InstanceStatus.DOWN);
                    ((EurekaServiceInstance) serviceInstance2).getInstanceInfo().setStatus(InstanceInfo.InstanceStatus.DOWN);

                    APIContainer container = containerService.getContainerById("demoapp");
                    assertNotNull(container);

                    assertThatContainerHasValidState(container, "DOWN", 0);
                }

            }

            @Nested
            class GivenSomeServicesAreDown {
                @Test
                void containerStatusIsWarning() {
                    ((EurekaServiceInstance) serviceInstance2).getInstanceInfo().setStatus(InstanceInfo.InstanceStatus.DOWN);

                    APIContainer container = containerService.getContainerById("demoapp");
                    assertNotNull(container);

                    assertThatContainerHasValidState(container, "WARNING", 1);
                }
            }

        }

        @Nested
        class GivenMultipleApiIds {

            @Test
            void groupThem() {
                var serviceInstance = ServicesBuilder.createInstance(SERVICE_ID, SERVICE_ID,
                        Pair.of("apiml.apiInfo.api-v1.apiId", "api1"),
                        Pair.of("apiml.apiInfo.api-v1.version", "1.0.0"),
                        Pair.of("apiml.apiInfo.api-v2.apiId", "api2"),
                        Pair.of("apiml.apiInfo.api-v2.version", "2"),
                        Pair.of("apiml.apiInfo.api-v3.apiId", "api3"));
                doReturn(Collections.singletonList(serviceInstance)).when(discoveryClient).getInstances(SERVICE_ID);
                doReturn(Collections.singletonList(SERVICE_ID)).when(discoveryClient).getServices();
                APIContainer apiContainer = containerService.getContainerById(SERVICE_ID);

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
                    doReturn(Arrays.asList(
                        ServicesBuilder.createInstance(SERVICE_ID, SERVICE_ID, Pair.of(AUTHENTICATION_SCHEME, "bypass")),
                        ServicesBuilder.createInstance(SERVICE_ID, SERVICE_ID, Pair.of(AUTHENTICATION_SCHEME, "zoweJwt"))
                    )).when(discoveryClient).getInstances(SERVICE_ID);
                    doReturn(Collections.singletonList(SERVICE_ID)).when(discoveryClient).getServices();

                    APIContainer apiContainer = containerService.getContainerById(SERVICE_ID);

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
                    ServiceInstance serviceInstance = ServicesBuilder.createInstance(SERVICE_ID, SERVICE_ID, Pair.of(AUTHENTICATION_SCHEME, "zoweJwt"));
                    doReturn(Collections.singletonList(serviceInstance)).when(discoveryClient).getInstances(SERVICE_ID);
                    doReturn(Collections.singletonList(SERVICE_ID)).when(discoveryClient).getServices();
                    APIContainer apiContainer = containerService.getContainerById(SERVICE_ID);

                    assertTrue(apiContainer.isSso());
                    for (APIService apiService : apiContainer.getServices()) {
                        assertTrue(apiService.isSso());
                        assertTrue(apiService.isSsoAllInstances());
                    }
                }

                @Test
                void whenGetService_thenUpdateIt() {
                    containerService = spy(containerService);
                    ServiceInstance serviceInstance = ServicesBuilder.createInstance(SERVICE_ID, SERVICE_ID, Pair.of(AUTHENTICATION_SCHEME, "zoweJwt"));
                    doReturn(Collections.singletonList(serviceInstance)).when(discoveryClient).getInstances(SERVICE_ID);
                    var apiService = containerService.getService(SERVICE_ID);
                    assertNotNull(apiService);
                    assertTrue(apiService.isSso());
                    verify(containerService, times(1)).update(apiService);
                }

            }

        }

        @Nested
        class GivenHideServiceInfo {

            @Test
            void thenSetToApiService() {
                ServiceInstance serviceInstance = ServicesBuilder.createInstance(SERVICE_ID, SERVICE_ID, Pair.of(AUTHENTICATION_SCHEME, "zoweJwt"));
                doReturn(Collections.singletonList(SERVICE_ID)).when(discoveryClient).getServices();
                doReturn(Collections.singletonList(serviceInstance)).when(discoveryClient).getInstances(SERVICE_ID);
                ReflectionTestUtils.setField(containerService, "hideServiceInfo", true);
                APIContainer apiContainer = containerService.getContainerById(SERVICE_ID);
                assertTrue(apiContainer.isHideServiceInfo());
            }

        }

    }

    @Nested
    class MultiTenancy {

        private ContainerService containerService;

        @BeforeEach
        void init() {
            containerService = new ContainerService(
                mock(DiscoveryClient.class),
                new TransformService(new GatewayClient(ServiceAddress.builder().scheme("https").hostname("localhost").build())),
                new CustomStyleConfig()
            );
        }

        private APIService createDto(RegistrationType registrationType) {
            Map<String, String> metadata = new HashMap<>();
            metadata.put(APIML_ID, "apimlId");
            metadata.put(SERVICE_TITLE, "title");
            metadata.put(REGISTRATION_TYPE, registrationType.getValue());
            var service = new EurekaServiceInstance(InstanceInfo.Builder.newBuilder()
                .setAppName(CoreService.GATEWAY.getServiceId())
                .setMetadata(metadata)
                .build()
            );
            return containerService.createAPIServiceFromInstance(service);
        }

        @Test
        void givenPrimaryInstance_whenCreateDto_thenDoNotUpdateTitle() {
            var dto = createDto(RegistrationType.ADDITIONAL);
            assertEquals("title (apimlId)", dto.getTitle());
            assertEquals("apimlid", dto.getServiceId());
            assertEquals("/apimlid", dto.getBasePath());
        }

        @Test
        void givenPrimaryInstance_whenCreateDto_thenAddApimlIdIntoTitle() {
            var dto = createDto(RegistrationType.PRIMARY);
            assertEquals("title", dto.getTitle());
            assertEquals("gateway", dto.getServiceId());
            assertEquals("/", dto.getBasePath());
        }

    }

}
