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
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.zowe.apiml.apicatalog.exceptions.ContainerStatusRetrievalThrowable;
import org.zowe.apiml.apicatalog.model.APIContainer;
import org.zowe.apiml.apicatalog.model.APIService;
import org.zowe.apiml.apicatalog.services.cached.CachedApiDocService;
import org.zowe.apiml.apicatalog.services.cached.CachedProductFamilyService;
import org.zowe.apiml.apicatalog.services.cached.CachedServicesService;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApiCatalogControllerTests {
    private final String pathToContainers = "/containers";

    @Mock
    private CachedServicesService cachedServicesService;

    @Mock
    private CachedProductFamilyService cachedProductFamilyService;

    @Mock
    private CachedApiDocService cachedApiDocService;

    @InjectMocks
    private ApiCatalogController apiCatalogController;

    @Test
    void whenGetAllContainers_givenNothing_thenReturnContainersWithState() {
        Application service1 = new Application("service-1");
        service1.addInstance(getStandardInstance("service1", InstanceInfo.InstanceStatus.UP));

        Application service2 = new Application("service-2");
        service1.addInstance(getStandardInstance("service2", InstanceInfo.InstanceStatus.DOWN));

        given(this.cachedServicesService.getService("service1")).willReturn(service1);
        given(this.cachedServicesService.getService("service2")).willReturn(service2);
        given(this.cachedProductFamilyService.getAllContainers()).willReturn(createContainers());

        RestAssuredMockMvc.standaloneSetup(apiCatalogController);
        RestAssuredMockMvc.given().
            when().
            get(pathToContainers).
            then().
            statusCode(200);
    }

    @Test
    void givenNoContainersAreAvailable_whenAllContainersAreRequested_thenReturnNoContent() {
        given(cachedProductFamilyService.getAllContainers()).willReturn(null);

        RestAssuredMockMvc.standaloneSetup(apiCatalogController);
        RestAssuredMockMvc.given().
            when().
            get(pathToContainers).
            then().
            statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void givenContainerWithGivenIdIsUnavailable_whenRequested_thenReturnOk() {
        String containerId = "service1";
        given(cachedProductFamilyService.getContainerById(containerId)).willReturn(null);

        RestAssuredMockMvc.standaloneSetup(apiCatalogController);
        RestAssuredMockMvc.given().
            when().
            get(pathToContainers + "/" + containerId).
            then().
            statusCode(HttpStatus.OK.value());
    }

    @Test
    void whenGetSingleContainer_thenPopulateApiDocForServices() throws ContainerStatusRetrievalThrowable {
        Application service1 = new Application("service-1");
        service1.addInstance(getStandardInstance("service1", InstanceInfo.InstanceStatus.UP));

        Application service2 = new Application("service-2");
        service1.addInstance(getStandardInstance("service2", InstanceInfo.InstanceStatus.DOWN));

        List<String> apiVersions = Arrays.asList("1.0.0", "2.0.0");
        String defaultApiVersion = "v1";

        given(this.cachedServicesService.getService("service1")).willReturn(service1);
        given(this.cachedServicesService.getService("service2")).willReturn(service2);
        given(this.cachedProductFamilyService.getContainerById("api-one")).willReturn(createContainers().get(0));
        given(this.cachedApiDocService.getDefaultApiDocForService("service1")).willReturn("service1");
        given(this.cachedApiDocService.getDefaultApiDocForService("service2")).willReturn("service2");
        given(this.cachedApiDocService.getApiVersionsForService("service1")).willReturn(apiVersions);
        given(this.cachedApiDocService.getApiVersionsForService("service2")).willReturn(apiVersions);
        given(this.cachedApiDocService.getDefaultApiVersionForService("service1")).willReturn(defaultApiVersion);
        given(this.cachedApiDocService.getDefaultApiVersionForService("service2")).willReturn(defaultApiVersion);

        ResponseEntity<List<APIContainer>> containers = this.apiCatalogController.getAPIContainerById("api-one");
        Assertions.assertNotNull(containers.getBody());
        Assertions.assertEquals(1, containers.getBody().size());
        containers.getBody().forEach(apiContainer ->
            apiContainer.getServices().forEach(apiService -> {
                Assertions.assertEquals(apiService.getServiceId(), apiService.getApiDoc());
                Assertions.assertEquals(apiVersions, apiService.getApiVersions());
                Assertions.assertEquals(defaultApiVersion, apiService.getDefaultApiVersion());
            }));
    }

    @Test
    void whenGetSingleContainer_thenPopulateApiDocForServicesExceptOneWhichFails() throws ContainerStatusRetrievalThrowable {
        Application service1 = new Application("service-1");
        service1.addInstance(getStandardInstance("service1", InstanceInfo.InstanceStatus.UP));

        Application service2 = new Application("service-2");
        service1.addInstance(getStandardInstance("service2", InstanceInfo.InstanceStatus.DOWN));

        List<String> apiVersions = Arrays.asList("1.0.0", "2.0.0");

        given(this.cachedServicesService.getService("service1")).willReturn(service1);
        given(this.cachedServicesService.getService("service2")).willReturn(service2);
        given(this.cachedProductFamilyService.getContainerById("api-one")).willReturn(createContainers().get(0));
        given(this.cachedApiDocService.getDefaultApiDocForService("service1")).willReturn("service1");
        given(this.cachedApiDocService.getDefaultApiDocForService("service2")).willThrow(new RuntimeException());
        given(this.cachedApiDocService.getApiVersionsForService("service1")).willReturn(apiVersions);
        ResponseEntity<List<APIContainer>> containers = this.apiCatalogController.getAPIContainerById("api-one");
        Assertions.assertNotNull(containers.getBody());
        Assertions.assertEquals(1, containers.getBody().size());
        containers.getBody().forEach(apiContainer ->
            apiContainer.getServices().forEach(apiService -> {
                if (apiService.getServiceId().equals("service1")) {
                    Assertions.assertEquals(apiService.getServiceId(), apiService.getApiDoc());
                    Assertions.assertEquals(apiService.getApiVersions(), apiVersions);
                }
                if (apiService.getServiceId().equals("service2")) {
                    Assertions.assertNull(apiService.getApiDoc());
                }
            }));
    }

    @Test
    void whenGetSingleContainer_thenPopulateApiVersionsForServicesExceptOneWhichFails() throws ContainerStatusRetrievalThrowable {
        Application service1 = new Application("service-1");
        service1.addInstance(getStandardInstance("service1", InstanceInfo.InstanceStatus.UP));

        Application service2 = new Application("service-2");
        service1.addInstance(getStandardInstance("service2", InstanceInfo.InstanceStatus.DOWN));

        List<String> apiVersions = Arrays.asList("1.0.0", "2.0.0");

        given(this.cachedServicesService.getService("service1")).willReturn(service1);
        given(this.cachedServicesService.getService("service2")).willReturn(service2);
        given(this.cachedProductFamilyService.getContainerById("api-one")).willReturn(createContainers().get(0));
        given(this.cachedApiDocService.getDefaultApiDocForService("service1")).willReturn("service1");
        given(this.cachedApiDocService.getDefaultApiDocForService("service2")).willReturn("service2");
        given(this.cachedApiDocService.getApiVersionsForService("service1")).willReturn(apiVersions);
        given(this.cachedApiDocService.getApiVersionsForService("service2")).willThrow(new RuntimeException());
        ResponseEntity<List<APIContainer>> containers = this.apiCatalogController.getAPIContainerById("api-one");
        Assertions.assertNotNull(containers.getBody());
        Assertions.assertEquals(1, containers.getBody().size());
        containers.getBody().forEach(apiContainer ->
            apiContainer.getServices().forEach(apiService -> {
                if (apiService.getServiceId().equals("service1")) {
                    Assertions.assertEquals(apiService.getServiceId(), apiService.getApiDoc());
                    Assertions.assertEquals(apiService.getApiVersions(), apiVersions);
                }
                if (apiService.getServiceId().equals("service2")) {
                    Assertions.assertEquals(apiService.getServiceId(), apiService.getApiDoc());
                    Assertions.assertNull(apiService.getApiVersions());
                }
            }));
    }


    // =========================================== Helper Methods ===========================================

    private List<APIContainer> createContainers() {
        Set<APIService> services = new HashSet<>();

        APIService service = new APIService("service1", "service-1", "service-1", false, "url", "home", "base");
        services.add(service);

        service = new APIService("service2", "service-2", "service-2", true, "url", "home", "base");
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
