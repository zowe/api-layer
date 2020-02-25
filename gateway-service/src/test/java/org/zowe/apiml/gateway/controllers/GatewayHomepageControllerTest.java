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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.ui.Model;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GatewayHomepageControllerTest {

    private GatewayHomepageController gatewayHomepageController;
    private DiscoveryClient discovery;
    private AuthConfigurationProperties authConfigurationProperties;
    private Model model;

    @BeforeEach
    public void setup() {
        discovery = mock(DiscoveryClient.class);
        authConfigurationProperties = new AuthConfigurationProperties();
        model = mock(Model.class);

        gatewayHomepageController = new GatewayHomepageController(discovery, authConfigurationProperties);
    }

    @Test
    public void shouldReturnHomeString() {
        authConfigurationProperties.setZosmfServiceId("zosmf");

        String home = gatewayHomepageController.home(model);

        assertEquals("home", home);
    }

    @Test
    public void shouldSetAuthStatusAndIcon_WhenInitializeAuthentication_IfAuthUp() {
        authConfigurationProperties.setZosmfServiceId("zosmf");
        authConfigurationProperties.setProvider("provider");

        ArrayList<ServiceInstance> serviceInstances = new ArrayList<>();
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        serviceInstances.add(serviceInstance);

        when(discovery.getInstances(authConfigurationProperties.validatedZosmfServiceId())).thenReturn(serviceInstances);

        String home = gatewayHomepageController.home(model);

        assertEquals("home", home);
    }

    @Test
    public void shouldSetDiscoveryStatusAndIcon_WhenInitializeDiscovery_IfOneInstanceIsRunning() {
        authConfigurationProperties.setZosmfServiceId("zosmf");
        authConfigurationProperties.setProvider("provider");

        ArrayList<ServiceInstance> serviceInstances = new ArrayList<>();
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        serviceInstances.add(serviceInstance);

        when(discovery.getInstances("discovery")).thenReturn(serviceInstances);
        String home = gatewayHomepageController.home(model);

        assertEquals("home", home);
    }

    @Test
    public void shouldSetDiscoveryStatusAndIcon_WhenInitializeDiscovery_IfTwoOrMoreInstancesAreRunning() {
        authConfigurationProperties.setZosmfServiceId("zosmf");
        authConfigurationProperties.setProvider("provider");

        ArrayList<ServiceInstance> serviceInstances = new ArrayList<>();
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        ServiceInstance serviceInstance2 = mock(ServiceInstance.class);
        serviceInstances.add(serviceInstance);
        serviceInstances.add(serviceInstance2);

        when(discovery.getInstances("discovery")).thenReturn(serviceInstances);
        String home = gatewayHomepageController.home(model);

        assertEquals("home", home);
    }

    @Test
    public void shouldSetApiCatalogInfo_WhenInitializeCatalog_IfOneInstanceIsRunning() {
        authConfigurationProperties.setZosmfServiceId("zosmf");
        authConfigurationProperties.setProvider("provider");

        ArrayList<ServiceInstance> serviceInstances = new ArrayList<>();
        ServiceInstance serviceInstance = mock(ServiceInstance.class);
        serviceInstances.add(serviceInstance);

        when(discovery.getInstances("apicatalog")).thenReturn(serviceInstances);
        String home = gatewayHomepageController.home(model);

        assertEquals("home", home);
    }
}
