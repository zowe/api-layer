/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.gateway;

import com.ca.mfaas.apicatalog.services.initialisation.InstanceRetrievalService;
import com.ca.mfaas.product.constants.CoreService;
import com.netflix.appinfo.InstanceInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;

import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class GatewayConfigInitializierTest {


    private GatewayConfigProperties gatewayConfigProperties;
    private GatewayConfigInitializer gatewayConfigInitializer;


    @Mock
    InstanceRetrievalService instanceRetrievalService;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
        gatewayConfigInitializer = new GatewayConfigInitializer(instanceRetrievalService);
    }

    @Test
    public void shouldGetGatewayConfigProperties() throws GatewayConfigInitializerException {

        when(instanceRetrievalService.getInstanceInfo(CoreService.GATEWAY.getServiceId()))
            .thenReturn(
                getStandardInstance(CoreService.GATEWAY.getServiceId(), InstanceInfo.InstanceStatus.UP, "https://localhost:9090/"));
        gatewayConfigProperties = gatewayConfigInitializer.getGatewayConfigProperties();
        Assert.assertEquals("localhost:9090", gatewayConfigProperties.getHostname());
        Assert.assertEquals("https", gatewayConfigProperties.getScheme());
    }


    @Test
    public void shouldReturnMessageIfHomePageIsInvalid() throws GatewayConfigInitializerException {
        when(instanceRetrievalService.getInstanceInfo(CoreService.GATEWAY.getServiceId()))
            .thenReturn(
                getStandardInstance(CoreService.GATEWAY.getServiceId(), InstanceInfo.InstanceStatus.UP, ":{{{"));
        exception.expectMessage("Gateway URL is incorrect");
        gatewayConfigInitializer.getGatewayConfigProperties();

    }

    @Test
    public void shouldReturnMessageIfGatewayInstanceNull() throws GatewayConfigInitializerException {
        when(instanceRetrievalService.getInstanceInfo(null))
            .thenReturn(
                getStandardInstance(null, InstanceInfo.InstanceStatus.UP, ":{{{"));
        exception.expectMessage("Gateway Instance not retrieved from Discovery Service, retrying...");
        gatewayConfigInitializer.getGatewayConfigProperties();

    }


    private InstanceInfo getStandardInstance(String serviceId, InstanceInfo.InstanceStatus status, String homePage) {
        return new InstanceInfo(serviceId, null, null, "192.168.0.1", null, new InstanceInfo.PortWrapper(true, 9090),
            new InstanceInfo.PortWrapper(true, 9090), homePage, null, null, null, "localhost", "localhost", 0, null,
            "localhost", status, null, null, null, null, null, null, null, null, null);
    }





}
