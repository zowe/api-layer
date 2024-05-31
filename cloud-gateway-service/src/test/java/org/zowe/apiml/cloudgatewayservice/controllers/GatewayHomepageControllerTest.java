/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.controllers;

import org.hamcrest.collection.IsMapContaining;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.zowe.apiml.product.version.BuildInfo;
import org.zowe.apiml.product.version.BuildInfoDetails;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GatewayHomepageControllerTest {
    private DiscoveryClient discoveryClient;

    private GatewayHomepageController gatewayHomepageController;
    private BuildInfo buildInfo;

    private final String API_CATALOG_ID = "apicatalog";
    private final String AUTHORIZATION_SERVICE_ID = "zaas";

    @BeforeEach
    void setup() {
        discoveryClient = mock(DiscoveryClient.class);

        buildInfo = mock(BuildInfo.class);

        BuildInfoDetails buildInfoDetails = new BuildInfoDetails(new Properties(), new Properties());
        when(buildInfo.getBuildInfoDetails()).thenReturn(buildInfoDetails);

        gatewayHomepageController = new GatewayHomepageController(
            discoveryClient, buildInfo, API_CATALOG_ID);
    }

    @Test
    void givenBuildVersionNull_whenHomePageCalled_thenBuildInfoShouldStaticText() {
        Model model = new ConcurrentModel();
        gatewayHomepageController.home(model);

        Map<String, Object> actualModelMap = model.asMap();

        assertThat(actualModelMap, IsMapContaining.hasEntry("buildInfoText", "Build information is not available"));
    }

    @Test
    void givenSpecificBuildVersion_whenHomePageCalled_thenBuildInfoShouldBeGivenVersionAndNumber() {
        BuildInfo buildInfo = mock(BuildInfo.class);

        Properties buildProperties = new Properties();
        buildProperties.setProperty("build.version", "test-version");
        buildProperties.setProperty("build.number", "test-number");
        BuildInfoDetails buildInfoDetails = new BuildInfoDetails(buildProperties, new Properties());
        when(buildInfo.getBuildInfoDetails()).thenReturn(buildInfoDetails);

        GatewayHomepageController gatewayHomepageController = new GatewayHomepageController(
            discoveryClient, buildInfo, API_CATALOG_ID);

        Model model = new ConcurrentModel();
        gatewayHomepageController.home(model);

        Map<String, Object> actualModelMap = model.asMap();

        assertThat(actualModelMap, IsMapContaining.hasEntry("buildInfoText", "Version test-version build # test-number"));
    }


    @Test
    void givenBuildInfo_whenHomePageCalled_thenHomePageShouldReturnHomeLiteral() {
        String redirectedPage = gatewayHomepageController.home(new ConcurrentModel());
        assertEquals("home", redirectedPage, "Expected page is not 'home'");
    }

    @Test
    void givenApiCatalogWithNullInstances_whenHomePageCalled_thenHomePageModelShouldContain() {
        Model model = new ConcurrentModel();
        gatewayHomepageController.home(model);

        assertCatalogIsDownMessageShown(model.asMap());
    }

    @Test
    void givenApiCatalogueIsEmpty_whenHomePageIsCalled_thenThereIsNoMessageAroundTheCatalog() {
        GatewayHomepageController underTest = new GatewayHomepageController(discoveryClient, buildInfo, null);
        Model model = new ConcurrentModel();
        underTest.home(model);

        Map<String, Object> preparedModel = model.asMap();
        assertThat(preparedModel, hasEntry("isAnyCatalogAvailable", false));
        assertThat(preparedModel, not(hasKey("catalogLink")));
    }

    @Test
    void givenApiCatalogInstanceWithEmptyAuthService_whenHomePageCalled_thenHomePageModelShouldBeReportedDown() {
        discoveryReturnValidApiCatalog(1);

        Model model = new ConcurrentModel();
        gatewayHomepageController.home(model);

        assertCatalogIsDownMessageShown(model.asMap());
    }

    @Test
    void givenApiCatalogWithEmptyInstancesWithEmptyAuthService_whenHomePageCalled_thenHomePageModelShouldBeReportedDown() {
        when(discoveryClient.getInstances(API_CATALOG_ID)).thenReturn(Collections.EMPTY_LIST);
        when(discoveryClient.getInstances(AUTHORIZATION_SERVICE_ID)).thenReturn(Collections.EMPTY_LIST);

        Model model = new ConcurrentModel();
        gatewayHomepageController.home(model);

        assertCatalogIsDownMessageShown(model.asMap());
    }

    @Test
    void givenApiCatalogWithEmptyInstances_whenHomePageCalled_thenHomePageModelShouldContain() {
        when(discoveryClient.getInstances(API_CATALOG_ID)).thenReturn(Collections.EMPTY_LIST);

        Model model = new ConcurrentModel();
        gatewayHomepageController.home(model);

        assertCatalogIsDownMessageShown(model.asMap());
    }

    @Test
    void givenOneApiCatalogInstance_whenHomePageCalled_thenHomePageModelShouldContain() {
        discoveryReturnValidApiCatalog(1);
        discoveryReturnValidZaas(1);
        Model model = new ConcurrentModel();
        gatewayHomepageController.home(model);


        assertCatalogIsUpMessageShown(model.asMap());
        assertThat(model.asMap(), hasEntry("catalogStatusText", "The API Catalog is running"));
    }

    @Test
    void givenApiCatalogInstances_whenHomePageCalled_thenHomePageModelShouldContain() {
        discoveryReturnValidApiCatalog(2);
        discoveryReturnValidZaas(1);
        Model model = new ConcurrentModel();
        gatewayHomepageController.home(model);


        assertCatalogIsUpMessageShown(model.asMap());
        assertThat(model.asMap(), hasEntry("catalogStatusText", "2 API Catalog instances are running"));
    }

    @Test
    void givenMultipleZaasInstances_whenHomePageCalled_thenHomePageModelShouldContain() {
        discoveryReturnValidZaas(2);
        Model model = new ConcurrentModel();
        gatewayHomepageController.home(model);

        Map<String, Object> actualModelMap = model.asMap();
        assertThat(actualModelMap, IsMapContaining.hasEntry("authIconName", "success"));
        assertThat(actualModelMap, IsMapContaining.hasEntry("authStatusText", "2 Authentication Service instances are running"));
    }

    private void assertCatalogIsDownMessageShown(Map<String, Object> preparedModelView) {
        assertThat(preparedModelView, hasEntry("catalogIconName", "warning"));
        assertThat(preparedModelView, hasEntry("catalogStatusText", "The API Catalog is not running"));
        assertThat(preparedModelView, hasEntry("catalogLinkEnabled", false));
        assertThat(preparedModelView, not(hasKey("catalogLink")));
    }

    private void assertCatalogIsUpMessageShown(Map<String, Object> preparedModelView) {
        assertThat(preparedModelView, hasEntry("catalogIconName", "success"));
        assertThat(preparedModelView, hasEntry("catalogLinkEnabled", true));
        assertThat(preparedModelView, hasEntry("catalogLink", "/apicatalog/ui/v1"));
    }

    private void discoveryReturnValidApiCatalog(int numberOfInstances) {
        Map<String, String> metadataMap = new HashMap<>();
        metadataMap.put("apiml.routes.ui-v1.gatewayUrl", "/ui/v1");
        metadataMap.put("apiml.routes.ui-v1.serviceUrl", "/apicatalog");
        ArrayList<ServiceInstance> apiCatalogServiceInstances = new ArrayList<>();
        for (int n = 0; n < numberOfInstances; n++) {
            apiCatalogServiceInstances.add(
                new DefaultServiceInstance("instanceId" + n, "serviceId",
                    "host", 10000 + n, true, metadataMap)
            );
        }

        when(discoveryClient.getInstances(API_CATALOG_ID)).thenReturn(apiCatalogServiceInstances);
    }

    private void discoveryReturnValidZaas(int numberOfInstances) {
        Map<String, String> metadataMap = new HashMap<>();
        metadataMap.put("apiml.routes.ui-v1.gatewayUrl", "/api/v1");
        metadataMap.put("apiml.routes.ui-v1.serviceUrl", "/zaas");
        ArrayList<ServiceInstance> zaasServiceInstances = new ArrayList<>();
        for (int n = 0; n < numberOfInstances; n++) {
            zaasServiceInstances.add(
                new DefaultServiceInstance("instanceId" + n, "serviceId",
                    "host", 10000 + n, true, metadataMap)
            );
        }

        when(discoveryClient.getInstances(AUTHORIZATION_SERVICE_ID)).thenReturn(zaasServiceInstances);
    }

    @Test
    void givenDiscoveryServiceWithNullInstances_whenHomePageCalled_thenHomePageModelShouldContain() {
        Model model = new ConcurrentModel();
        gatewayHomepageController.home(model);

        Map<String, Object> actualModelMap = model.asMap();

        assertThat(actualModelMap, IsMapContaining.hasEntry("discoveryIconName", "danger"));
        assertThat(actualModelMap, IsMapContaining.hasEntry("discoveryStatusText", "The Discovery Service is not running"));
    }


    @Test
    void givenDiscoveryServiceWithEmptyInstances_whenHomePageCalled_thenHomePageModelShouldContain() {
        when(discoveryClient.getInstances("apicatalog")).thenReturn(Collections.EMPTY_LIST);

        Model model = new ConcurrentModel();
        gatewayHomepageController.home(model);

        Map<String, Object> actualModelMap = model.asMap();

        assertThat(actualModelMap, IsMapContaining.hasEntry("discoveryIconName", "danger"));
        assertThat(actualModelMap, IsMapContaining.hasEntry("discoveryStatusText", "The Discovery Service is not running"));
    }

    @Test
    void givenDiscoveryServiceWithOneInstance_whenHomePageCalled_thenHomePageModelShouldContain() {
        ServiceInstance serviceInstance = new DefaultServiceInstance("instanceId", "serviceId",
            "host", 10000, true);

        when(discoveryClient.getInstances("discovery")).thenReturn(Arrays.asList(serviceInstance));

        Model model = new ConcurrentModel();
        gatewayHomepageController.home(model);

        Map<String, Object> actualModelMap = model.asMap();

        assertThat(actualModelMap, IsMapContaining.hasEntry("discoveryIconName", "success"));
        assertThat(actualModelMap, IsMapContaining.hasEntry("discoveryStatusText", "The Discovery Service is running"));
    }

    @Test
    void givenDiscoveryServiceWithMoreThanOneInstance_whenHomePageCalled_thenHomePageModelShouldContain() {
        ServiceInstance serviceInstance = new DefaultServiceInstance("instanceId", "serviceId",
            "host", 10000, true);

        when(discoveryClient.getInstances("discovery")).thenReturn(Arrays.asList(serviceInstance, serviceInstance));

        Model model = new ConcurrentModel();
        gatewayHomepageController.home(model);

        Map<String, Object> actualModelMap = model.asMap();

        assertThat(actualModelMap, IsMapContaining.hasEntry("discoveryIconName", "success"));
        assertThat(actualModelMap, IsMapContaining.hasEntry("discoveryStatusText", "2 Discovery Service instances are running"));
    }


    @Test
    void givenDummyProvider_whenHomePageCalled_thenHomePageModelShouldContain() {
        Model model = new ConcurrentModel();
        discoveryReturnValidZaas(1);
        gatewayHomepageController.home(model);
        Map<String, Object> actualModelMap = model.asMap();

        assertThat(actualModelMap, IsMapContaining.hasEntry("authIconName", "success"));
        assertThat(actualModelMap, IsMapContaining.hasEntry("authStatusText", "The Authentication Service is running"));
    }

    @Test
    void givenZOSMFProviderIsntRunning_whenHomePageCalled_thenHomePageModelShouldContainNotRunning() {

        Model model = new ConcurrentModel();
        gatewayHomepageController.home(model);

        Map<String, Object> actualModelMap = model.asMap();

        assertThat(actualModelMap, IsMapContaining.hasEntry("authIconName", "warning"));
        assertThat(actualModelMap, IsMapContaining.hasEntry("authStatusText", "The Authentication Service is not running"));
    }

    @Test
    void givenZOSMFProviderRunning_whenHomePageCalled_thenHomePageModelShouldContainRunning() {

        discoveryReturnValidZaas(1);
        Model model = new ConcurrentModel();
        gatewayHomepageController.home(model);

        Map<String, Object> actualModelMap = model.asMap();

        assertThat(actualModelMap, IsMapContaining.hasEntry("authIconName", "success"));
        assertThat(actualModelMap, IsMapContaining.hasEntry("authStatusText", "The Authentication Service is running"));
    }

}
