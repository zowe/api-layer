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
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
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
class InstanceInitializeServiceTest {

    @Nested
    class WhenCalculatingContainerTotals {

        private static final String SERVICE_ID = "service_test_id";

        private EurekaClient eurekaClient = mock(EurekaClient.class);
        private TransformService transformService = new TransformService(new GatewayClient(ServiceAddress.builder().scheme("https").hostname("localhost").build()));
        private CustomStyleConfig customStyleConfig = new CustomStyleConfig();

        private InstanceInfo instance1;
        private InstanceInfo instance2;
        private InstanceInitializeService instanceInitializeService;

        @BeforeEach
        void prepareApplications() {
            instance1 = ServicesBuilder.createInstance("service1", "demoapp");
            instance2 = ServicesBuilder.createInstance("service2", "demoapp");
            Application application1 = new Application("service1", Collections.singletonList(instance1));
            Application application2 = new Application("service2", Collections.singletonList(instance2));

            when(eurekaClient.getApplication("service1")).thenReturn(application1);
            when(eurekaClient.getApplication("service2")).thenReturn(application2);
            when(eurekaClient.getApplications()).thenReturn(new Applications("hash", 0L, Arrays.asList(application1, application2)));
            instanceInitializeService = new InstanceInitializeService(
                eurekaClient,
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

            @Nested
            class GivenAllServicesAreUp {

                @Test
                void containerStatusIsUp() {
                    APIContainer container = instanceInitializeService.getContainerById("demoapp");
                    assertNotNull(container);

                    assertThatContainerHasValidState(container, "UP", 2);
                }

            }

            @Nested
            class GivenAllServicesAreDown {

                @Test
                void containerStatusIsDown() {
                    instance1.setStatus(InstanceInfo.InstanceStatus.DOWN);
                    instance2.setStatus(InstanceInfo.InstanceStatus.DOWN);

                    APIContainer container = instanceInitializeService.getContainerById("demoapp");
                    assertNotNull(container);

                    assertThatContainerHasValidState(container, "DOWN", 0);
                }

            }

            @Nested
            class GivenSomeServicesAreDown {
                @Test
                void containerStatusIsWarning() {
                    instance2.setStatus(InstanceInfo.InstanceStatus.DOWN);

                    APIContainer container = instanceInitializeService.getContainerById("demoapp");
                    assertNotNull(container);

                    assertThatContainerHasValidState(container, "WARNING", 1);
                }
            }

        }

        @Nested
        class GivenMultipleApiIds {

            @Test
            void groupThem() {
                Application application = ServicesBuilder.createApp(
                    SERVICE_ID,
                    ServicesBuilder.createInstance(SERVICE_ID, SERVICE_ID,
                        Pair.of("apiml.apiInfo.api-v1.apiId", "api1"),
                        Pair.of("apiml.apiInfo.api-v1.version", "1.0.0"),
                        Pair.of("apiml.apiInfo.api-v2.apiId", "api2"),
                        Pair.of("apiml.apiInfo.api-v2.version", "2"),
                        Pair.of("apiml.apiInfo.api-v3.apiId", "api3")));
                Applications applications = new Applications("hash", 0L, Collections.singletonList(application));
                doReturn(application).when(eurekaClient).getApplication(SERVICE_ID);
                doReturn(applications).when(eurekaClient).getApplications();
                APIContainer apiContainer = instanceInitializeService.getContainerById(SERVICE_ID);

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
                    Application application = ServicesBuilder.createApp(
                        SERVICE_ID,
                        ServicesBuilder.createInstance(SERVICE_ID, SERVICE_ID, Pair.of(AUTHENTICATION_SCHEME, "bypass")),
                        ServicesBuilder.createInstance(SERVICE_ID, SERVICE_ID, Pair.of(AUTHENTICATION_SCHEME, "zoweJwt"))
                    );
                    Applications applications = new Applications("hash", 0L, Collections.singletonList(application));
                    doReturn(application).when(eurekaClient).getApplication(SERVICE_ID);
                    doReturn(applications).when(eurekaClient).getApplications();

                    APIContainer apiContainer = instanceInitializeService.getContainerById(SERVICE_ID);

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
                    InstanceInfo instanceInfo = ServicesBuilder.createInstance(SERVICE_ID, SERVICE_ID, Pair.of(AUTHENTICATION_SCHEME, "zoweJwt"));
                    Application application = ServicesBuilder.createApp(SERVICE_ID, instanceInfo);
                    Applications applications = new Applications("hash", 0L, Collections.singletonList(application));
                    doReturn(application).when(eurekaClient).getApplication(SERVICE_ID);
                    doReturn(applications).when(eurekaClient).getApplications();
                    APIContainer apiContainer = instanceInitializeService.getContainerById(SERVICE_ID);

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
                InstanceInfo instanceInfo = ServicesBuilder.createInstance(SERVICE_ID, SERVICE_ID, Pair.of(AUTHENTICATION_SCHEME, "zoweJwt"));
                Application application = ServicesBuilder.createApp(SERVICE_ID, instanceInfo);
                Applications applications = new Applications("hash", 0L, Collections.singletonList(application));
                doReturn(applications).when(eurekaClient).getApplications();
                ReflectionTestUtils.setField(instanceInitializeService, "hideServiceInfo", true);
                APIContainer apiContainer = instanceInitializeService.getContainerById(SERVICE_ID);
                assertTrue(apiContainer.isHideServiceInfo());
            }

        }

    }

    @Nested
    class MultiTenancy {

        private InstanceInitializeService instanceInitializeService;

        @BeforeEach
        void init() {
            instanceInitializeService = new InstanceInitializeService(
                mock(EurekaClient.class),
                new TransformService(new GatewayClient(ServiceAddress.builder().scheme("https").hostname("localhost").build())),
                new CustomStyleConfig()
            );
        }

        private APIService createDto(RegistrationType registrationType) {
            Map<String, String> metadata = new HashMap<>();
            metadata.put(APIML_ID, "apimlId");
            metadata.put(SERVICE_TITLE, "title");
            metadata.put(REGISTRATION_TYPE, registrationType.getValue());
            var service = InstanceInfo.Builder.newBuilder()
                .setAppName(CoreService.GATEWAY.getServiceId())
                .setMetadata(metadata)
                .build();
            return instanceInitializeService.createAPIServiceFromInstance(service);
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
