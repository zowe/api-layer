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

import com.ca.mfaas.apicatalog.instance.InstanceRetrievalService;
import com.netflix.appinfo.InstanceInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.retry.support.RetryTemplate;

@RunWith(MockitoJUnitRunner.class)
public class GatewayLookupServiceTest {


   // @InjectMocks
    private GatewayLookupService gatewayLookupService;

  //  @Mock
    private RetryTemplate retryTemplate;

   // @Mock
    private InstanceRetrievalService instanceRetrievalService;

    @Before
    public void setup() {

    }

    @Test
    public void testInit() {
//        when(instanceRetrievalService.getInstanceInfo(CoreService.GATEWAY.getServiceId()))
//            .thenReturn(
//                getStandardInstance(CoreService.GATEWAY.getServiceId(), InstanceInfo.InstanceStatus.UP, "https://localhost:9090/"));
//
//
//        gatewayLookupService.init();
      //  GatewayConfigProperties gatewayConfigProperties = gatewayLookupService.getGatewayConfigProperties();

      //  gatewayLookupService.doWithRetry(null);
      //  System.out.println(gatewayConfigProperties.getHostname());
    }


   /* @Mock
    InstanceRetrievalService instanceRetrievalService;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
        gatewayLookupService = new GatewayLookupService(instanceRetrievalService);
    }

    @Test
    public void shouldGetGatewayConfigProperties() throws GatewayConfigInitializerException {

        when(instanceRetrievalService.getInstanceInfo(CoreService.GATEWAY.getServiceId()))
            .thenReturn(
                getStandardInstance(CoreService.GATEWAY.getServiceId(), InstanceInfo.InstanceStatus.UP, "https://localhost:9090/"));
        gatewayConfigProperties = gatewayLookupService.getGatewayConfigProperties();
        Assert.assertEquals("localhost:9090", gatewayConfigProperties.getHostname());
        Assert.assertEquals("https", gatewayConfigProperties.getScheme());
    }


    @Test
    public void shouldReturnMessageIfHomePageIsInvalid() throws GatewayConfigInitializerException {
        when(instanceRetrievalService.getInstanceInfo(CoreService.GATEWAY.getServiceId()))
            .thenReturn(
                getStandardInstance(CoreService.GATEWAY.getServiceId(), InstanceInfo.InstanceStatus.UP, ":{{{"));
        exception.expectMessage("Gateway URL is incorrect");
        gatewayLookupService.getGatewayConfigProperties();

    }

    @Test
    public void shouldReturnMessageIfGatewayInstanceNull() throws GatewayConfigInitializerException {
        when(instanceRetrievalService.getInstanceInfo(null))
            .thenReturn(
                getStandardInstance(null, InstanceInfo.InstanceStatus.UP, ":{{{"));
        exception.expectMessage("Gateway Instance not retrieved from Discovery Service, retrying...");
        gatewayLookupService.getGatewayConfigProperties();

    }

*/

    private InstanceInfo getStandardInstance(String serviceId,
                                             InstanceInfo.InstanceStatus status,
                                             String homePage) {

        return InstanceInfo.Builder.newBuilder()
            .setInstanceId(serviceId)
            .setHostName("localhost")
            .setHomePageUrl(homePage, homePage)
            .setAppName(serviceId)
            .setStatus(status)
            .build();
    }



}
