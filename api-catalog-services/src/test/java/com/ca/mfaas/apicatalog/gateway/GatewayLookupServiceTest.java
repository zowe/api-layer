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

import com.ca.mfaas.apicatalog.instance.InstanceInitializationException;
import com.ca.mfaas.apicatalog.instance.InstanceRetrievalService;
import com.ca.mfaas.product.constants.CoreService;
import com.ca.mfaas.product.gateway.GatewayConfigProperties;
import com.netflix.appinfo.InstanceInfo;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.MockitoRule;
import org.springframework.retry.support.RetryTemplate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class GatewayLookupServiceTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private InstanceRetrievalService instanceRetrievalService;

    private RetryTemplate retryTemplate;

    @Before
    public void setUp() {
        retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(new GatewayLookupRetryPolicy());
    }

    @Test
    public void shouldSuccessfullyRetrieveAfterSomeAttemps() {
        InstanceInfo sampleInstance =  getStandardInstance("https://localhost:9090/");
        when(instanceRetrievalService.getInstanceInfo(CoreService.GATEWAY.getServiceId()))
            .thenThrow(InstanceInitializationException.class)
            .thenThrow(InstanceInitializationException.class)
            .thenThrow(InstanceInitializationException.class)
            .thenReturn(null)
            .thenReturn(sampleInstance);

        GatewayLookupService gatewayLookupService = new GatewayLookupService(retryTemplate, instanceRetrievalService);
        gatewayLookupService.init();

        GatewayConfigProperties gatewayConfigProperties = gatewayLookupService.getGatewayConfigProperties();


        assertNotNull(gatewayConfigProperties);
        assertEquals("https", gatewayConfigProperties.getScheme());
        assertEquals("localhost:9090", gatewayConfigProperties.getHostname());

        verify(instanceRetrievalService, times(5)).getInstanceInfo(CoreService.GATEWAY.getServiceId());
    }

    @Test(expected = GatewayLookupException.class)
    public void shouldThrowWhenInvalidUrlRetrieved() {
        InstanceInfo sampleInstance =  getStandardInstance("Curious Cat");
        when(instanceRetrievalService.getInstanceInfo(CoreService.GATEWAY.getServiceId())).thenReturn(sampleInstance);

        GatewayLookupService gatewayLookupService = new GatewayLookupService(retryTemplate, instanceRetrievalService);
        gatewayLookupService.init();
    }


    private InstanceInfo getStandardInstance(String homePageUrl) {

        return InstanceInfo.Builder.newBuilder()
            .setAppName("serviceId")
            .setHostName("localhost")
            .setHomePageUrl(homePageUrl, homePageUrl)
            .build();
    }
}
