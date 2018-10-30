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

import com.netflix.appinfo.InstanceInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

public class MfaasServiceLocatorDiscoveryClientTest {

    private MfaasServiceLocator mfaasServiceLocator;
    private DiscoveryClient discoveryClient;

    @Before
    public void setUp() throws Exception {
        discoveryClient = mock(DiscoveryClient.class);
        mfaasServiceLocator = new MfaasServiceLocator(discoveryClient);
    }

    @Test
    public void testGetGatewayFromDiscoveryClient() throws Exception {
        InstanceInfo instanceInfo = InstanceInfo.Builder.newBuilder()
            .setAppName("gateway").setSecurePort(10010).setHostName("localhost").setSecureVIPAddress("localhost").build();
        List<ServiceInstance> serviceInstances = new ArrayList<>();
        serviceInstances.add(new ServiceInstance() {
            @Override
            public String getServiceId() {
                return instanceInfo.getAppName();
            }

            @Override
            public String getHost() {
                return instanceInfo.getHostName();
            }

            @Override
            public int getPort() {
                return instanceInfo.getSecurePort();
            }

            @Override
            public boolean isSecure() {
                return true;
            }

            @Override
            public URI getUri() {
                return null;
            }

            @Override
            public Map<String, String> getMetadata() {
                return null;
            }
        });
        when(discoveryClient.getInstances(any())).thenReturn(serviceInstances);
        URI uri = mfaasServiceLocator.locateGatewayUrl();
        Assert.assertNotNull(uri);
        Assert.assertEquals("https://localhost:10010", uri.toString());
    }
}
