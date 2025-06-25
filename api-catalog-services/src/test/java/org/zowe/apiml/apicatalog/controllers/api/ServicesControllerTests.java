/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.controllers.api;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.zowe.apiml.apicatalog.exceptions.ContainerStatusRetrievalThrowable;
import org.zowe.apiml.apicatalog.instance.InstanceInitializeService;
import org.zowe.apiml.apicatalog.model.APIContainer;
import org.zowe.apiml.apicatalog.model.APIService;
import org.zowe.apiml.apicatalog.model.CustomStyleConfig;
import org.zowe.apiml.apicatalog.services.cached.CachedApiDocService;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.product.instance.ServiceAddress;
import org.zowe.apiml.product.routing.transform.TransformService;

import java.util.*;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.standaloneSetup;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

class ServicesControllerTests {

    private final String pathToContainers = "/containers";

    private EurekaClient eurekaClient;
    private InstanceInitializeService instanceInitializeService;
    private CachedApiDocService cachedApiDocService;

    private ServicesController underTest;

    @BeforeEach
    void setUp() {
        eurekaClient = mock(EurekaClient.class);
        instanceInitializeService = spy(new InstanceInitializeService(
            eurekaClient,
            new TransformService(new GatewayClient(ServiceAddress.builder().scheme("https").hostname("localhost").build())),
            new CustomStyleConfig())
        );
        cachedApiDocService = mock(CachedApiDocService.class);

        underTest = new ServicesController(instanceInitializeService, cachedApiDocService);
        standaloneSetup(underTest);
    }

    @Nested
    class GivenThereAreNoValidContainers {
        @Nested
        class WhenAllContainersAreRequested {
            @Test
            void thenReturnNoContent() {
                given(instanceInitializeService.getAllContainers()).willReturn(null);

                RestAssuredMockMvc.given().
                    when().
                    get(pathToContainers).
                    then().
                    statusCode(HttpStatus.NO_CONTENT.value());
            }
        }

        @Nested
        class WhenSpecificContainerRequested {
            @Test
            void thenReturnOk() {
                String containerId = "service1";
                given(instanceInitializeService.getContainerById(containerId)).willReturn(null);

                RestAssuredMockMvc.given().
                    when().
                    get(pathToContainers + "/" + containerId).
                    then().
                    statusCode(HttpStatus.OK.value());
            }
        }
    }

    @Nested
    class GivenMultipleValidContainers {
        Application service1;
        Application service2;
        List<String> apiVersions;

        @BeforeEach
        void prepareApplications() {
            service1 = new Application("service-1");
            service1.addInstance(getStandardInstance("service1", InstanceInfo.InstanceStatus.UP));

            service2 = new Application("service-2");
            service1.addInstance(getStandardInstance("service2", InstanceInfo.InstanceStatus.DOWN));

            apiVersions = Arrays.asList("1.0.0", "2.0.0");

            given(eurekaClient.getApplication("service1")).willReturn(service1);
            given(cachedApiDocService.getDefaultApiDocForService("service1")).willReturn("service1");
            given(cachedApiDocService.getApiVersionsForService("service1")).willReturn(apiVersions);

            given(eurekaClient.getApplication("service2")).willReturn(service2);
            given(cachedApiDocService.getDefaultApiDocForService("service2")).willReturn("service2");
            given(cachedApiDocService.getApiVersionsForService("service2")).willReturn(apiVersions);

            given(instanceInitializeService.getContainerById("api-one")).willReturn(createContainers().get(0));
        }

        @Nested
        class WhenGettingAllContainers {
            @Test
            void thenReturnContainersWithState() {
                given(instanceInitializeService.getAllContainers()).willReturn(createContainers());

                RestAssuredMockMvc.given().
                    when().
                    get(pathToContainers).
                    then().
                    statusCode(HttpStatus.OK.value());
            }
        }

        @Nested
        class WhenGettingSpecificContainer {
            @Test
            void thenPopulateApiDocForServices() throws ContainerStatusRetrievalThrowable {
                String defaultApiVersion = "v1";

                given(cachedApiDocService.getDefaultApiVersionForService("service1")).willReturn(defaultApiVersion);
                given(cachedApiDocService.getDefaultApiVersionForService("service2")).willReturn(defaultApiVersion);

                ResponseEntity<List<APIContainer>> containers = underTest.getAPIContainerById("api-one");

                containers.getBody().forEach(apiContainer ->
                    apiContainer.getServices().forEach(apiService -> {
                        assertEquals(apiService.getServiceId(), apiService.getApiDoc());
                        assertEquals(apiVersions, apiService.getApiVersions());
                        assertEquals(defaultApiVersion, apiService.getDefaultApiVersion());
                    }));
            }

            @Test
            void thenPopulateApiDocForServicesExceptOneWhichFails() throws ContainerStatusRetrievalThrowable {
                given(cachedApiDocService.getDefaultApiDocForService("service2")).willThrow(new RuntimeException());

                ResponseEntity<List<APIContainer>> containers = underTest.getAPIContainerById("api-one");
                assertThereIsOneContainer(containers);

                containers.getBody().forEach(apiContainer ->
                    apiContainer.getServices().forEach(apiService -> {
                        if (apiService.getServiceId().equals("service1")) {
                            assertEquals(apiService.getServiceId(), apiService.getApiDoc());
                            assertEquals(apiService.getApiVersions(), apiVersions);
                        }
                        if (apiService.getServiceId().equals("service2")) {
                            Assertions.assertNull(apiService.getApiDoc());
                        }
                    }));
            }

            @Test
            void thenPopulateApiVersionsForServicesExceptOneWhichFails() throws ContainerStatusRetrievalThrowable {
                given(cachedApiDocService.getApiVersionsForService("service2")).willThrow(new RuntimeException());

                ResponseEntity<List<APIContainer>> containers = underTest.getAPIContainerById("api-one");
                assertThereIsOneContainer(containers);

                containers.getBody().forEach(apiContainer ->
                    apiContainer.getServices().forEach(apiService -> {
                        if (apiService.getServiceId().equals("service1")) {
                            assertEquals(apiService.getServiceId(), apiService.getApiDoc());
                            assertEquals(apiService.getApiVersions(), apiVersions);
                        }
                        if (apiService.getServiceId().equals("service2")) {
                            assertEquals(apiService.getServiceId(), apiService.getApiDoc());
                            Assertions.assertNull(apiService.getApiVersions());
                        }
                    }));
            }

            private void assertThereIsOneContainer(ResponseEntity<List<APIContainer>> containers) {
                assertThat(containers.getBody(), is(not(nullValue())));
                assertThat(containers.getBody().size(), is(1));
            }
        }
    }

    @Nested
    class WhenGettingSpecificService {
        private final String serviceId = "service1";
        private final APIService service =  new APIService.Builder(serviceId)
            .secured(true)
            .baseUrl("url")
            .basePath("base")
            .sso(false)
            .apis(Collections.emptyMap())
            .build();

        @Test
        void thenReturnNotFound() {
            given(eurekaClient.getApplications()).willReturn(null);

            String pathToServices = "/services";
            RestAssuredMockMvc.given().
                when().
                get(pathToServices + "/" + serviceId).
                then().
                statusCode(HttpStatus.NOT_FOUND.value());
        }

        @Test
        void thenReturnOk() throws ContainerStatusRetrievalThrowable {
            String defaultApiVersion = "v1";

            given(instanceInitializeService.getService(serviceId)).willReturn(service);
            given(cachedApiDocService.getDefaultApiVersionForService(serviceId)).willReturn(defaultApiVersion);
            given(cachedApiDocService.getDefaultApiDocForService(serviceId)).willReturn("mockApiDoc");

            ResponseEntity<APIService> apiServicesById = underTest.getAPIServicesById(serviceId);
            assertEquals(HttpStatus.OK, apiServicesById.getStatusCode());
            assertNotNull(apiServicesById.getBody());
            assertEquals( "mockApiDoc", apiServicesById.getBody().getApiDoc());
            assertEquals("v1", apiServicesById.getBody().getDefaultApiVersion());
        }

        @Test
        void thenReturnOkWithApiDocNull() throws ContainerStatusRetrievalThrowable {
            String defaultApiVersion = "v1";

            given(instanceInitializeService.getService(serviceId)).willReturn(service);
            given(cachedApiDocService.getDefaultApiVersionForService(serviceId)).willReturn(defaultApiVersion);
            given(cachedApiDocService.getDefaultApiDocForService(serviceId)).willReturn(null);

            ResponseEntity<APIService> apiServicesById = underTest.getAPIServicesById(serviceId);
            assertEquals(HttpStatus.OK, apiServicesById.getStatusCode());
            assertNotNull(apiServicesById.getBody());
            assertNull(apiServicesById.getBody().getApiDoc());
        }
    }

    // =========================================== Helper Methods ===========================================

    private List<APIContainer> createContainers() {
        Set<APIService> services = new HashSet<>();

        APIService service =  new APIService.Builder("service1")
            .title("service-1")
            .description("service-1")
            .secured(true)
            .baseUrl("url")
            .homePageUrl("home")
            .basePath("base")
            .sso(false)
            .apis(Collections.emptyMap())
            .build();
        services.add(service);

        service =  new APIService.Builder("service2")
            .title("service-2")
            .description("service-2")
            .secured(true)
            .baseUrl("url")
            .homePageUrl("home")
            .basePath("base")
            .sso(false)
            .apis(Collections.emptyMap())
            .build();
        services.add(service);

        APIContainer container = new APIContainer("api-one", "API One", "This is API One", services);

        APIContainer container1 = new APIContainer("api-two", "API Two", "This is API Two", services);

        return Arrays.asList(container, container1);
    }

    private InstanceInfo getStandardInstance(String serviceId, InstanceInfo.InstanceStatus status) {
        return new InstanceInfo(serviceId, null, null, "192.168.0.1", null, new InstanceInfo.PortWrapper(true, 9090),
            null, null, null, null, null, null, null, 0, null, "hostname", status, null, null, null, null, null,
            null, null, null, null);
    }

}
