/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.config;

import org.junit.jupiter.api.Test;
import org.zowe.apiml.product.gateway.GatewayConfigProperties;

import static org.junit.Assert.assertEquals;

class GatewayConfigTest {

    private static final String HOST = "hostA";
    private static final String PORT = "8888";
    private static final String SCHEME = "https";

    @Test
    void shouldReturnGatewayProperties() {
        GatewayConfigProperties gatewayConfigProperties = new GatewayConfig().getGatewayConfigProperties(HOST, PORT, SCHEME);
        assertEquals(HOST + ":" + PORT, gatewayConfigProperties.getHostname());
        assertEquals(SCHEME, gatewayConfigProperties.getScheme());
    }
}
