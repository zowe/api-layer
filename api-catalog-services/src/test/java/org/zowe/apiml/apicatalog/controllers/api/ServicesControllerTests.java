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
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zowe.apiml.apicatalog.controllers.handlers.ApiCatalogControllerExceptionHandler;
import org.zowe.apiml.apicatalog.exceptions.ContainerStatusRetrievalException;
import org.zowe.apiml.apicatalog.model.APIContainer;
import org.zowe.apiml.apicatalog.model.APIService;
import org.zowe.apiml.apicatalog.model.CustomStyleConfig;
import org.zowe.apiml.apicatalog.swagger.ApiDocRetrievalService;
import org.zowe.apiml.apicatalog.swagger.ContainerService;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.product.instance.ServiceAddress;
import org.zowe.apiml.product.routing.transform.TransformService;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(SpringExtension.class)
@WebFluxTest(controllers = ServicesController.class, excludeAutoConfiguration = ReactiveSecurityAutoConfiguration.class)
@ContextConfiguration(classes = {
    ServicesController.class,
    ApiCatalogControllerExceptionHandler.class,
    ContainerService.class,
    ServicesControllerTests.Context.class
})
class ServicesControllerTests {

    private final String pathToContainers = "/apicatalog/containers";

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ServicesController underTest;

    @MockitoBean
    private EurekaClient eurekaClient;

    @MockitoBean
    private CustomStyleConfig customStyleConfig;

    @MockitoSpyBean
    private ContainerService containerService;

    @MockitoBean
    private ApiDocRetrievalService apiDocRetrievalService;

    @Nested
    class GivenThereAreNoValidContainers {

        @Nested
        class WhenAllContainersAreRequested {

            @Test
            void thenReturnNoContent() {
                given(containerService.getAllContainers()).willReturn(null);

                webTestClient.get().uri(pathToContainers).exchange()
                    .expectStatus().isNoContent();
            }

        }

        @Nested
        class WhenSpecificContainerRequested {

            @Test
            void thenReturnOk() {
                String containerId = "service1";
                given(containerService.getContainerById(containerId)).willReturn(null);

                webTestClient.get().uri(pathToContainers + "/" + containerId).exchange()
                    .expectStatus().isOk();
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
            given(apiDocRetrievalService.retrieveDefaultApiDoc("service1")).willReturn("service1");
            given(apiDocRetrievalService.retrieveApiVersions("service1")).willReturn(apiVersions);

            given(eurekaClient.getApplication("service2")).willReturn(service2);
            given(apiDocRetrievalService.retrieveDefaultApiDoc("service2")).willReturn("service2");
            given(apiDocRetrievalService.retrieveApiVersions("service2")).willReturn(apiVersions);

            given(containerService.getContainerById("api-one")).willReturn(createContainers().get(0));
        }

        @Nested
        class WhenGettingAllContainers {

            @Test
            void thenReturnContainersWithState() {
                given(containerService.getAllContainers()).willReturn(createContainers());

                webTestClient.get().uri(pathToContainers).exchange()
                    .expectStatus().isOk();
            }

        }

        @Nested
        class WhenGettingSpecificContainer {

            @Test
            void thenPopulateApiDocForServices() throws ContainerStatusRetrievalException {
                String defaultApiVersion = "v1";

                given(apiDocRetrievalService.retrieveDefaultApiVersion("service1")).willReturn(defaultApiVersion);
                given(apiDocRetrievalService.retrieveDefaultApiVersion("service2")).willReturn(defaultApiVersion);

                ResponseEntity<List<APIContainer>> containers = underTest.getAPIContainerById("api-one").block();

                containers.getBody().forEach(apiContainer ->
                    apiContainer.getServices().forEach(apiService -> {
                        assertEquals(apiService.getServiceId(), apiService.getApiDoc());
                        assertEquals(apiVersions, apiService.getApiVersions());
                        assertEquals(defaultApiVersion, apiService.getDefaultApiVersion());
                    }));
            }

            @Test
            void thenPopulateApiDocForServicesExceptOneWhichFails() throws ContainerStatusRetrievalException {
                given(apiDocRetrievalService.retrieveDefaultApiDoc("service2")).willThrow(new RuntimeException());

                ResponseEntity<List<APIContainer>> containers = underTest.getAPIContainerById("api-one").block();
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
            void thenPopulateApiVersionsForServicesExceptOneWhichFails() throws ContainerStatusRetrievalException {
                given(apiDocRetrievalService.retrieveApiVersions("service2")).willThrow(new RuntimeException());

                ResponseEntity<List<APIContainer>> containers = underTest.getAPIContainerById("api-one").block();
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

            String pathToServices = "/apicatalog/services";
            webTestClient.get().uri(pathToServices + "/" + serviceId).exchange()
                .expectStatus().isNotFound();
        }

        @Test
        void thenReturnOk() throws ContainerStatusRetrievalException {
            String defaultApiVersion = "v1";

            given(containerService.getService(serviceId)).willReturn(service);
            given(apiDocRetrievalService.retrieveDefaultApiVersion(serviceId)).willReturn(defaultApiVersion);
            given(apiDocRetrievalService.retrieveDefaultApiDoc(serviceId)).willReturn("mockApiDoc");

            ResponseEntity<APIService> apiServicesById = underTest.getAPIServicesById(serviceId).block();
            assertEquals(HttpStatus.OK, apiServicesById.getStatusCode());
            assertNotNull(apiServicesById.getBody());
            assertEquals( "mockApiDoc", apiServicesById.getBody().getApiDoc());
            assertEquals("v1", apiServicesById.getBody().getDefaultApiVersion());
        }

        @Test
        void thenReturnOkWithApiDocNull() throws ContainerStatusRetrievalException {
            String defaultApiVersion = "v1";

            given(containerService.getService(serviceId)).willReturn(service);
            given(apiDocRetrievalService.retrieveDefaultApiVersion(serviceId)).willReturn(defaultApiVersion);
            given(apiDocRetrievalService.retrieveDefaultApiDoc(serviceId)).willReturn(null);

            ResponseEntity<APIService> apiServicesById = underTest.getAPIServicesById(serviceId).block();
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

    static class Context {

        @Bean
        public TransformService transformService() {
            return new TransformService(new GatewayClient(ServiceAddress.builder().scheme("https").hostname("localhost").build()));
        }

        @Bean
        public MessageService messageService() {
            return new YamlMessageService("/apicatalog-log-messages.yml");
        }

    }

}
