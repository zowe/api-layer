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
import org.zowe.apiml.product.instance.ServiceAddress;

import static org.junit.jupiter.api.Assertions.*;


class GatewayClientTest {

    private final ServiceAddress gatewayConfigProperties = ServiceAddress.builder()
        .scheme("https")
        .hostname("localhost")
        .build();
    private final GatewayClient gatewayClient = new GatewayClient(gatewayConfigProperties);

    @Test
    void testGetGatewayConfigProperties_whenItIsNotNull() {
        assertEquals(gatewayClient.getGatewayConfigProperties(), gatewayConfigProperties);
    }

    @Test
    void testGetGatewayConfigProperties_whenItNull() {
        gatewayClient.setGatewayConfigProperties(null);
        assertThrows(GatewayNotAvailableException.class, () -> {
            gatewayClient.getGatewayConfigProperties();
        });
    }

    @Test
    void testIsInitialized() {
        assertTrue(gatewayClient.isInitialized());
        gatewayClient.setGatewayConfigProperties(null);
        assertFalse(gatewayClient.isInitialized());
    }
}
