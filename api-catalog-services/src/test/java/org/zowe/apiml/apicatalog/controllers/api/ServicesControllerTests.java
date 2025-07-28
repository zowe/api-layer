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
import com.netflix.discovery.shared.Application;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaServiceInstance;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zowe.apiml.apicatalog.config.BeanConfig;
import org.zowe.apiml.apicatalog.controllers.handlers.DefaultExceptionHandler;
import org.zowe.apiml.apicatalog.controllers.handlers.ApiCatalogControllerExceptionHandler;
import org.zowe.apiml.apicatalog.exceptions.ContainerStatusRetrievalException;
import org.zowe.apiml.apicatalog.model.APIContainer;
import org.zowe.apiml.apicatalog.model.APIService;
import org.zowe.apiml.apicatalog.model.CustomStyleConfig;
import org.zowe.apiml.apicatalog.swagger.ApiDocService;
import org.zowe.apiml.apicatalog.swagger.ContainerService;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.product.instance.ServiceAddress;
import org.zowe.apiml.security.common.error.AuthExceptionHandler;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.CATALOG_ID;

@WebFluxTest(controllers = ServicesControllerMicroservice.class, excludeAutoConfiguration = ReactiveSecurityAutoConfiguration.class)
@ContextConfiguration(classes = {
    ServicesControllerMicroservice.class,
    ApiCatalogControllerExceptionHandler.class,
    ContainerService.class,
    DefaultExceptionHandler.class,
    AuthExceptionHandler.class,
    ServicesControllerTests.Context.class,
    BeanConfig.class
})
class ServicesControllerTests {

    private final String pathToContainers = "/apicatalog/containers";

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ServicesController underTest;

    @MockitoBean
    private DiscoveryClient discoveryClient;

    @MockitoBean
    private CustomStyleConfig customStyleConfig;

    @MockitoSpyBean
    private ContainerService containerService;

    @MockitoBean
    private ApiDocService apiDocService;

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

            private static final String SERVICE_ID = "service1";

            @Test
            void ifExistingInstanceThenReturnOk() {
                var instance = InstanceInfo.Builder.newBuilder().setAppName(SERVICE_ID).setInstanceId("instance1").setMetadata(Map.of(CATALOG_ID, SERVICE_ID)).build();
                doReturn(Collections.singletonList(new EurekaServiceInstance(instance))).when(discoveryClient).getInstances(SERVICE_ID);
                doReturn(Collections.singletonList(SERVICE_ID)).when(discoveryClient).getServices();
                doReturn(Mono.empty()).when(apiDocService).retrieveDefaultApiDoc(SERVICE_ID);

                webTestClient.get().uri(pathToContainers + "/" + SERVICE_ID).exchange()
                    .expectStatus().isOk();
            }

            @Test
            void ifNonExistingInstanceThenReturnNotFound() {
                given(containerService.getContainerById(SERVICE_ID)).willReturn(null);

                webTestClient.get().uri(pathToContainers + "/" + SERVICE_ID).exchange()
                    .expectStatus().isNotFound();
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
            apiVersions = Arrays.asList("1.0.0", "2.0.0");

            given(discoveryClient.getInstances("service1")).willReturn(
                Collections.singletonList(new EurekaServiceInstance(getStandardInstance("service1", InstanceInfo.InstanceStatus.UP)))
            );
            given(apiDocService.retrieveDefaultApiDoc("service1")).willReturn(Mono.just("service1"));
            given(apiDocService.retrieveApiVersions("service1")).willReturn(apiVersions);

            given(discoveryClient.getInstances("service2")).willReturn(
                Collections.singletonList(new EurekaServiceInstance(getStandardInstance("service2", InstanceInfo.InstanceStatus.DOWN)))
            );
            given(apiDocService.retrieveDefaultApiDoc("service2")).willReturn(Mono.just("service2"));
            given(apiDocService.retrieveApiVersions("service2")).willReturn(apiVersions);

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

                given(apiDocService.retrieveDefaultApiVersion("service1")).willReturn(defaultApiVersion);
                given(apiDocService.retrieveDefaultApiVersion("service2")).willReturn(defaultApiVersion);

                var elapsed = StepVerifier.create(underTest.getAPIContainerById("api-one"))
                    .assertNext(containers -> {
                        containers.getBody().forEach(apiContainer ->
                            apiContainer.getServices().forEach(apiService -> {
                                assertEquals(apiService.getServiceId(), apiService.getApiDoc());
                                assertEquals(apiVersions, apiService.getApiVersions());
                                assertEquals(defaultApiVersion, apiService.getDefaultApiVersion());
                            }));
                    })
                    .verifyComplete();
                assertEquals(0L, elapsed.toSeconds());
            }

            @Test
            void thenPopulateApiDocForServicesExceptOneWhichFails() throws ContainerStatusRetrievalException {
                given(apiDocService.retrieveDefaultApiDoc("service2")).willReturn(Mono.error(new RuntimeException()));

                var elapsed = StepVerifier.create(underTest.getAPIContainerById("api-one"))
                    .assertNext(containers -> {
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
                    })
                    .verifyComplete();

                assertEquals(0L, elapsed.toSeconds());
            }

            @Test
            void thenPopulateApiVersionsForServicesExceptOneWhichFails() throws ContainerStatusRetrievalException {
                given(apiDocService.retrieveApiVersions("service2")).willThrow(new RuntimeException());

                var elapsed = StepVerifier.create(underTest.getAPIContainerById("api-one"))
                    .assertNext(containers -> {
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
                    })
                    .verifyComplete();
                assertEquals(0L, elapsed.toSeconds());
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
            given(discoveryClient.getServices()).willReturn(Collections.emptyList());

            String pathToServices = "/apicatalog/services";
            webTestClient.get().uri(pathToServices + "/" + serviceId).exchange()
                .expectStatus().isNotFound();
        }

        @Test
        void thenReturnOk() {
            var defaultApiVersion = "v1";

            given(containerService.getService(serviceId)).willReturn(service);
            given(apiDocService.retrieveDefaultApiVersion(serviceId)).willReturn(defaultApiVersion);
            given(apiDocService.retrieveDefaultApiDoc(serviceId)).willReturn(Mono.just("mockApiDoc"));

            var elapsed = StepVerifier.create(underTest.getAPIServicesById(serviceId))
                .assertNext(apiServicesById -> {
                    assertEquals(HttpStatus.OK, apiServicesById.getStatusCode());
                    assertNotNull(apiServicesById.getBody());
                    assertEquals( "mockApiDoc", apiServicesById.getBody().getApiDoc());
                    assertEquals("v1", apiServicesById.getBody().getDefaultApiVersion());
                })
                .verifyComplete();

            assertEquals(0L, elapsed.toSeconds());
        }

        @Test
        void thenReturnOkWithoutApiDoc() {
            var defaultApiVersion = "v1";

            given(containerService.getService(serviceId)).willReturn(service);
            given(apiDocService.retrieveDefaultApiVersion(serviceId)).willReturn(defaultApiVersion);
            given(apiDocService.retrieveDefaultApiDoc(serviceId)).willReturn(Mono.empty());

            var elapsed = StepVerifier.create(underTest.getAPIServicesById(serviceId))
                .assertNext(apiServicesById -> {
                    assertEquals(HttpStatus.OK, apiServicesById.getStatusCode());
                    assertNotNull(apiServicesById.getBody());
                    assertNull(apiServicesById.getBody().getApiDoc());
                })
                .verifyComplete();

            assertEquals(0L, elapsed.toSeconds());
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

    @TestConfiguration
    static class Context {

        @Bean
        GatewayClient gatewayClient() {
            return new GatewayClient(ServiceAddress.builder().scheme("https").hostname("localhost").build());
        }

    }

}
