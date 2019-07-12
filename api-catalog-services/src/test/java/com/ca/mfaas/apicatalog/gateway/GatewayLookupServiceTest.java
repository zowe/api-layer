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
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.retry.RetryException;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;


public class GatewayLookupServiceTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private final InstanceRetrievalService instanceRetrievalService = mock(InstanceRetrievalService.class);
    private RetryTemplate retryTemplate;
    //private GatewayLookupService gatewayLookupService;

    private final int RETRY_COUNT = 5;

    @Before
    public void setUp() {
        retryTemplate = spy(new RetryTemplate());
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        int backOffMillis = 1;
        backOffPolicy.setBackOffPeriod(backOffMillis);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        retryTemplate.setRetryPolicy(
            new SimpleRetryPolicy(RETRY_COUNT, Collections.singletonMap(RetryException.class, true))
        );
    }

    @Test
    public void postConstructInitializationShouldRepeatExactlyTimes() {
        GatewayLookupService gatewayLookupService = new GatewayLookupService(retryTemplate, instanceRetrievalService);

        gatewayLookupService.init();

        verify(instanceRetrievalService, times(RETRY_COUNT)).getInstanceInfo(any());
    }

    @Test
    public void shouldSuccessfullyRetrieveAfterInit() {
        InstanceInfo instanceInfo = mock(InstanceInfo.class);
        when(instanceInfo.getHomePageUrl()).thenReturn("https://127.0.0.1:3500");
        when(instanceRetrievalService.getInstanceInfo(CoreService.GATEWAY.getServiceId())).thenReturn(instanceInfo);


        GatewayLookupService gatewayLookupService = new GatewayLookupService(retryTemplate, instanceRetrievalService);
        gatewayLookupService.init();
        GatewayConfigProperties gatewayConfigProperties = gatewayLookupService.getGatewayConfigProperties();

        assertNotNull(gatewayConfigProperties);
        assertEquals("https", gatewayConfigProperties.getScheme());
        assertEquals("127.0.0.1:3500", gatewayConfigProperties.getHostname());

    }

    @Test(expected = GatewayLookupException.class)
    public void shouldThrowWhenInvalidUrlRetrieved() {
        InstanceInfo instanceInfo = mock(InstanceInfo.class);
        when(instanceInfo.getHomePageUrl()).thenReturn("Curious Cat");
        when(instanceRetrievalService.getInstanceInfo(CoreService.GATEWAY.getServiceId())).thenReturn(instanceInfo);

        GatewayLookupService gatewayLookupService = new GatewayLookupService(retryTemplate, instanceRetrievalService);
        gatewayLookupService.init();
    }

    @Test(expected = GatewayLookupException.class)
    public void shouldThrowWhenUnexpectedErrorHappens() {
        InstanceInfo instanceInfo = mock(InstanceInfo.class);
        when(instanceInfo.getHomePageUrl()).thenThrow(RuntimeException.class);
        when(instanceRetrievalService.getInstanceInfo(CoreService.GATEWAY.getServiceId())).thenReturn(instanceInfo);

        GatewayLookupService gatewayLookupService = new GatewayLookupService(retryTemplate, instanceRetrievalService);
        gatewayLookupService.init();
    }

    @Test
    public void shouldRetryWhenInvalidMetadataFromEureka() {
        when(instanceRetrievalService.getInstanceInfo(CoreService.GATEWAY.getServiceId())).thenThrow(InstanceInitializationException.class);

        GatewayLookupService gatewayLookupService = new GatewayLookupService(retryTemplate, instanceRetrievalService);
        gatewayLookupService.init();

        verify(instanceRetrievalService, times(RETRY_COUNT)).getInstanceInfo(any());
    }


}
