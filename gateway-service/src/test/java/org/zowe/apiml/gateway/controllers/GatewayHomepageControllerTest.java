/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.controllers;

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
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GatewayHomepageControllerTest {
    private AuthConfigurationProperties authConfigurationProperties;
    private DiscoveryClient discoveryClient;

    private GatewayHomepageController gatewayHomepageController;

    private final String API_CATALOG_ID = "apicatalog";
    private final String AUTHORIZATION_SERVICE_ID = "zosmf";

    @BeforeEach
    public void setup() {
        discoveryClient = mock(DiscoveryClient.class);
        authConfigurationProperties = new AuthConfigurationProperties();
        authConfigurationProperties.setProvider("DUMMY");
        authConfigurationProperties.setZosmfServiceId(AUTHORIZATION_SERVICE_ID); // Default based on the local configuration

        BuildInfo buildInfo = mock(BuildInfo.class);

        BuildInfoDetails buildInfoDetails = new BuildInfoDetails(new Properties(), new Properties());
        when(buildInfo.getBuildInfoDetails()).thenReturn(buildInfoDetails);

        gatewayHomepageController = new GatewayHomepageController(
            discoveryClient, authConfigurationProperties, buildInfo);
    }


    @Test
    public void givenBuildVersionNull_whenHomePageCalled_thenBuildInfoShouldStaticText() {
        Model model = new ConcurrentModel();
        gatewayHomepageController.home(model);

        Map<String, Object> actualModelMap = model.asMap();

        assertThat(actualModelMap, IsMapContaining.hasEntry("buildInfoText", "Build information is not available"));
    }

    @Test
    public void givenSpecificBuildVersion_whenHomePageCalled_thenBuildInfoShouldBeGivenVersionAndNumber() {
        BuildInfo buildInfo = mock(BuildInfo.class);

        Properties buildProperties = new Properties();
        buildProperties.setProperty("build.version", "test-version");
        buildProperties.setProperty("build.number", "test-number");
        BuildInfoDetails buildInfoDetails = new BuildInfoDetails(buildProperties, new Properties());
        when(buildInfo.getBuildInfoDetails()).thenReturn(buildInfoDetails);

        GatewayHomepageController gatewayHomepageController = new GatewayHomepageController(
            discoveryClient, authConfigurationProperties, buildInfo);

        Model model = new ConcurrentModel();
        gatewayHomepageController.home(model);

        Map<String, Object> actualModelMap = model.asMap();

        assertThat(actualModelMap, IsMapContaining.hasEntry("buildInfoText", "Version test-version build # test-number"));
    }


    @Test
    public void givenBuildInfo_whenHomePageCalled_thenHomePageShouldReturnHomeLiteral() {
        String redirectedPage = gatewayHomepageController.home(new ConcurrentModel());
        assertEquals("home", redirectedPage, "Expected page is not 'home'");
    }

    @Test
    public void givenApiCatalogWithNullInstances_whenHomePageCalled_thenHomePageModelShouldContain() {
        Model model = new ConcurrentModel();
        gatewayHomepageController.home(model);

        assertCatalogIsDownMessageShown(model.asMap());
    }

    @Test
    public void givenApiCatalogInstanceWithEmptyAuthService_whenHomePageCalled_thenHomePageModelShouldBeReportedDown() {
        discoveryReturnValidApiCatalog();
        when(discoveryClient.getInstances(AUTHORIZATION_SERVICE_ID)).thenReturn(Collections.EMPTY_LIST);

        Model model = new ConcurrentModel();
        gatewayHomepageController.home(model);

        assertCatalogIsDownMessageShown(model.asMap());
    }

    @Test
    public void givenApiCatalogWithEmptyInstancesWithEmptyAuthService_whenHomePageCalled_thenHomePageModelShouldBeReportedDown() {
        when(discoveryClient.getInstances(API_CATALOG_ID)).thenReturn(Collections.EMPTY_LIST);
        when(discoveryClient.getInstances(AUTHORIZATION_SERVICE_ID)).thenReturn(Collections.EMPTY_LIST);

        Model model = new ConcurrentModel();
        gatewayHomepageController.home(model);

        assertCatalogIsDownMessageShown(model.asMap());
    }

    @Test
    public void givenApiCatalogWithEmptyInstances_whenHomePageCalled_thenHomePageModelShouldContain() {
        discoveryReturnValidZosmfAuthorizationInstance();
        when(discoveryClient.getInstances(API_CATALOG_ID)).thenReturn(Collections.EMPTY_LIST);

        Model model = new ConcurrentModel();
        gatewayHomepageController.home(model);

        assertCatalogIsDownMessageShown(model.asMap());
    }

    @Test
    public void givenApiCatalogInstance_whenHomePageCalled_thenHomePageModelShouldContain() {
        discoveryReturnValidZosmfAuthorizationInstance();
        discoveryReturnValidApiCatalog();

        Model model = new ConcurrentModel();
        gatewayHomepageController.home(model);

        assertCatalogIsUpMessageShown(model.asMap());
    }

    private void assertCatalogIsDownMessageShown(Map<String, Object> preparedModelView) {
        assertThat(preparedModelView, hasEntry("catalogIconName", "warning"));
        assertThat(preparedModelView, hasEntry("catalogStatusText", "The API Catalog is not running"));
        assertThat(preparedModelView, hasEntry("linkEnabled", false));
        assertThat(preparedModelView, not(hasKey("catalogLink")));
    }

    private void assertCatalogIsUpMessageShown(Map<String, Object> preparedModelView) {
        assertThat(preparedModelView, hasEntry("catalogIconName", "success"));
        assertThat(preparedModelView, hasEntry("catalogStatusText", "The API Catalog is running"));
        assertThat(preparedModelView, hasEntry("linkEnabled", true));
        assertThat(preparedModelView, hasEntry("catalogLink", "ui/v1/apicatalog"));
    }

    private void discoveryReturnValidZosmfAuthorizationInstance() {
        ServiceInstance authserviceInstance = new DefaultServiceInstance("instanceId", "serviceId",
            "host", 10000, true);
        when(discoveryClient.getInstances("zosmf")).thenReturn(
            Collections.singletonList(authserviceInstance)
        );
    }

    private void discoveryReturnValidApiCatalog() {
        Map<String, String> metadataMap = new HashMap<>();
        metadataMap.put("apiml.routes.ui_v1.gatewayUrl", "ui/v1");
        metadataMap.put("apiml.routes.ui_v1.serviceUrl", "/apicatalog");
        ServiceInstance apiCatalogServiceInstance = new DefaultServiceInstance("instanceId", "serviceId",
            "host", 10000, true, metadataMap);
        authConfigurationProperties.setProvider("zosmf");

        when(discoveryClient.getInstances(API_CATALOG_ID)).thenReturn(
            Collections.singletonList(apiCatalogServiceInstance));
    }

    @Test
    public void givenDiscoveryServiceWithNullInstances_whenHomePageCalled_thenHomePageModelShouldContain() {
        Model model = new ConcurrentModel();
        gatewayHomepageController.home(model);

        Map<String, Object> actualModelMap = model.asMap();

        assertThat(actualModelMap, IsMapContaining.hasEntry("discoveryIconName", "danger"));
        assertThat(actualModelMap, IsMapContaining.hasEntry("discoveryStatusText", "The Discovery Service is not running"));
    }


    @Test
    public void givenDiscoveryServiceWithEmptyInstances_whenHomePageCalled_thenHomePageModelShouldContain() {
        when(discoveryClient.getInstances("apicatalog")).thenReturn(Collections.EMPTY_LIST);

        Model model = new ConcurrentModel();
        gatewayHomepageController.home(model);

        Map<String, Object> actualModelMap = model.asMap();

        assertThat(actualModelMap, IsMapContaining.hasEntry("discoveryIconName", "danger"));
        assertThat(actualModelMap, IsMapContaining.hasEntry("discoveryStatusText", "The Discovery Service is not running"));
    }

    @Test
    public void givenDiscoveryServiceWithOneInstance_whenHomePageCalled_thenHomePageModelShouldContain() {
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
    public void givenDiscoveryServiceWithMoreThanOneInstance_whenHomePageCalled_thenHomePageModelShouldContain() {
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
    public void givenDummyProvider_whenHomePageCalled_thenHomePageModelShouldContain() {
        Model model = new ConcurrentModel();
        gatewayHomepageController.home(model);

        Map<String, Object> actualModelMap = model.asMap();

        assertThat(actualModelMap, IsMapContaining.hasEntry("authIconName", "success"));
        assertThat(actualModelMap, IsMapContaining.hasEntry("authStatusText", "The Authentication service is running"));
    }

    @Test
    public void givenZOSMFProviderWithNullInstances_whenHomePageCalled_thenHomePageModelShouldContain() {
        authConfigurationProperties.setProvider("zosmf");
        authConfigurationProperties.setZosmfServiceId("zosmf");

        Model model = new ConcurrentModel();
        gatewayHomepageController.home(model);

        Map<String, Object> actualModelMap = model.asMap();

        assertThat(actualModelMap, IsMapContaining.hasEntry("authIconName", "warning"));
        assertThat(actualModelMap, IsMapContaining.hasEntry("authStatusText", "The Authentication service is not running"));
    }

    @Test
    public void givenZOSMFProviderWithEmptyInstances_whenHomePageCalled_thenHomePageModelShouldContain() {
        when(discoveryClient.getInstances("zosmf")).thenReturn(Collections.EMPTY_LIST);

        authConfigurationProperties.setProvider("zosmf");
        authConfigurationProperties.setZosmfServiceId("zosmf");

        Model model = new ConcurrentModel();
        gatewayHomepageController.home(model);

        Map<String, Object> actualModelMap = model.asMap();

        assertThat(actualModelMap, IsMapContaining.hasEntry("authIconName", "warning"));
        assertThat(actualModelMap, IsMapContaining.hasEntry("authStatusText", "The Authentication service is not running"));
    }

    @Test
    public void givenZOSMFProviderWithOneInstance_whenHomePageCalled_thenHomePageModelShouldContain() {
        ServiceInstance serviceInstance = new DefaultServiceInstance("instanceId", "serviceId",
            "host", 10000, true);
        when(discoveryClient.getInstances("zosmf")).thenReturn(
            Arrays.asList(serviceInstance)
        );

        authConfigurationProperties.setProvider("zosmf");
        authConfigurationProperties.setZosmfServiceId("zosmf");

        Model model = new ConcurrentModel();
        gatewayHomepageController.home(model);

        Map<String, Object> actualModelMap = model.asMap();

        assertThat(actualModelMap, IsMapContaining.hasEntry("authIconName", "success"));
        assertThat(actualModelMap, IsMapContaining.hasEntry("authStatusText", "The Authentication service is running"));
    }
}
