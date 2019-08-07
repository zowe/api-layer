/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package com.ca.mfaas.product.gateway;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GatewayClientTest {

    GatewayClient gatewayClient;
    private GatewayConfigProperties gatewayConfigProperties;

    @Before
    public void setup() {
        gatewayConfigProperties = GatewayConfigProperties.builder()
            .scheme("https")
            .hostname("localhost")
            .build();
        gatewayClient = new GatewayClient(gatewayConfigProperties);
    }

    @Test
    public void shouldSetGatewayConfigProperties() {
        gatewayClient.setGatewayConfigProperties(gatewayConfigProperties);
        Assert.assertEquals(gatewayClient.getGatewayConfigProperties(), gatewayConfigProperties);
    }

    @Test
    public void shouldGetGatewayConfigProperties() {
        gatewayConfigProperties = GatewayConfigProperties.builder()
            .scheme("https")
            .hostname("localhost2")
            .build();
        gatewayClient.setGatewayConfigProperties(gatewayConfigProperties);
        Assert.assertEquals(gatewayConfigProperties, gatewayClient.getGatewayConfigProperties());
    }

    @Test
    public void isInitialized() {
        Assert.assertTrue(gatewayClient.isInitialized());
    }
}
