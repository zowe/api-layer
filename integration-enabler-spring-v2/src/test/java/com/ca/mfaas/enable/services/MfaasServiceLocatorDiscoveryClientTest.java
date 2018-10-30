/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.enable.services;

import com.ca.mfaas.product.config.MFaaSConfigPropertiesContainer;
import com.ca.mfaas.product.family.ProductFamilyType;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryProperties;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@Ignore
public class MfaasServiceLocatorDiscoveryClientTest {

    private MfaasServiceLocator mfaasServiceLocator;
    private DiscoveryClient discoveryClient;

    @Before
    public void setUp() {
        discoveryClient = mock(DiscoveryClient.class);
        MFaaSConfigPropertiesContainer propertiesContainer = mock(MFaaSConfigPropertiesContainer.class);
        MFaaSConfigPropertiesContainer.DiscoveryProperties discoveryProperties = new MFaaSConfigPropertiesContainer.DiscoveryProperties();
        discoveryProperties.setLocations("http://localhost:10011/eureka");
        when(propertiesContainer.getDiscovery()).thenReturn(discoveryProperties);
        mfaasServiceLocator = new MfaasServiceLocator(discoveryClient, propertiesContainer);
    }

    @Test
    public void testGetGatewayFromDiscoveryClient() throws Exception {
        URI gatewayURI = new URIBuilder().setScheme("https").setHost("localhost").setPort(10010).build();
        List<ServiceInstance> serviceInstances = new ArrayList<>();
        serviceInstances.add(new SimpleDiscoveryProperties.SimpleServiceInstance(gatewayURI));
        when(discoveryClient.getInstances(any())).thenReturn(serviceInstances);
        ServiceInstances gatewayInstances = mfaasServiceLocator.getServiceInstances(ProductFamilyType.GATEWAY.getServiceId());
        Assert.assertNotNull(gatewayInstances);
        List<ServiceInstance> instances = gatewayInstances.getServiceInstances();
        ServiceInstance instance = instances.get(0);
        Assert.assertEquals("https://localhost:10010", instance.getUri().toString());
    }
}
