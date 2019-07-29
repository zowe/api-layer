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
import com.ca.mfaas.product.constants.CoreService;
import com.ca.mfaas.product.gateway.GatewayConfigProperties;
import com.netflix.appinfo.InstanceInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.retry.support.RetryTemplate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GatewayConfigTest {

    private static final String HOST_PORT = "localhost:9090";
    private static final String SCHEME = "https";

    @Mock
    private InstanceRetrievalService instanceRetrievalService;

    private RetryTemplate retryTemplate;

    @Before
    public void setUp() {
        retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(new GatewayLookupRetryPolicy());
    }

    @Test
    public void shouldReturnGatewayConfigProperties() {
        String homePageUrl = SCHEME + "://" + HOST_PORT + "/";
        InstanceInfo sampleInstance =  InstanceInfo.Builder.newBuilder()
            .setAppName("serviceId")
            .setHostName("localhost")
            .setHomePageUrl(homePageUrl, homePageUrl)
            .build();
        when(instanceRetrievalService.getInstanceInfo(CoreService.GATEWAY.getServiceId())).thenReturn(sampleInstance);
        GatewayLookupService gatewayLookupService = new GatewayLookupService(retryTemplate, instanceRetrievalService);
        gatewayLookupService.init();
        GatewayConfigProperties gatewayConfigProperties = new GatewayConfig(gatewayLookupService).getGatewayConfigProperties();
        assertNotNull(gatewayConfigProperties);
        assertEquals(HOST_PORT, gatewayConfigProperties.getHostname());
        assertEquals(SCHEME, gatewayConfigProperties.getScheme());
    }


}
