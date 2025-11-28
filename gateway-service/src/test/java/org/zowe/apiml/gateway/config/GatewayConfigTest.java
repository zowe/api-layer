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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.ConfigurableEnvironment;
import org.zowe.apiml.product.gateway.GatewayConfigProperties;

import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class GatewayConfigTest {

    ConfigurableEnvironment env;

    @BeforeEach
    void setUp() {
        env = mock(ConfigurableEnvironment.class);
    }

    @Nested
    class WithExternalUrl {

        @Test
        void whenExternalUrlIsDefined_thenTransformIt() throws URISyntaxException {
            GatewayConfigProperties gatewayConfigProperties = new GatewayConfig(env).getGatewayConfigProperties("https://host:123/path", false, false, false, null, "0");
            assertEquals("host:123", gatewayConfigProperties.getHostname());
            assertEquals("https", gatewayConfigProperties.getScheme());
        }

    }

    @Nested
    class WithoutExternalUrl {

        @Test
        void whenClientAttls_thenTransformIt() throws URISyntaxException {
            GatewayConfigProperties gatewayConfigProperties = new GatewayConfig(env).getGatewayConfigProperties(null, true, true, false, "hostname", "10010");
            assertEquals("http", gatewayConfigProperties.getScheme());
            assertEquals("hostname:10010", gatewayConfigProperties.getHostname());
        }

        @Test
        void whenOnlyServerAttls_thenTransformIt() throws URISyntaxException {
            GatewayConfigProperties gatewayConfigProperties = new GatewayConfig(env).getGatewayConfigProperties(null, true, false, false, "hostname", "10010");
            assertEquals("https", gatewayConfigProperties.getScheme());
            assertEquals("hostname:10010", gatewayConfigProperties.getHostname());
        }

        @Test
        void whenSsl_thenTransformIt() throws URISyntaxException {
            GatewayConfigProperties gatewayConfigProperties = new GatewayConfig(env).getGatewayConfigProperties(null, false, false, true, "localhost", "10010");
            assertEquals("https", gatewayConfigProperties.getScheme());
            assertEquals("localhost:10010", gatewayConfigProperties.getHostname());
        }

        @Test
        void whenNoTtls_thenTransformIt() throws URISyntaxException {
            GatewayConfigProperties gatewayConfigProperties = new GatewayConfig(env).getGatewayConfigProperties(null, false, false, false, "localhost", "80");
            assertEquals("http", gatewayConfigProperties.getScheme());
            assertEquals("localhost:80", gatewayConfigProperties.getHostname());
        }

    }

}

