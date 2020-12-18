/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.gateway;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class GatewayClientTest {

    private final GatewayConfigProperties gatewayConfigProperties = GatewayConfigProperties.builder()
        .scheme("https")
        .hostname("localhost")
        .build();
    private final GatewayClient gatewayClient = new GatewayClient(gatewayConfigProperties);

    @Test
    public void testGetGatewayConfigProperties_whenItIsNotNull() {
        assertEquals(gatewayClient.getGatewayConfigProperties(), gatewayConfigProperties);
    }

    @Test
    public void testGetGatewayConfigProperties_whenItNull() {
        gatewayClient.setGatewayConfigProperties(null);
        assertThrows(GatewayNotAvailableException.class, () -> {
            gatewayClient.getGatewayConfigProperties();
        });
    }

    @Test
    public void testIsInitialized() {
        assertTrue(gatewayClient.isInitialized());
        gatewayClient.setGatewayConfigProperties(null);
        assertFalse(gatewayClient.isInitialized());
    }
}
