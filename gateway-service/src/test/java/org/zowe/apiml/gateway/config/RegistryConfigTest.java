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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.product.instance.ServiceAddress;

import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegistryConfigTest {

    @Nested
    class GatewayServiceAddress {

        @Nested
        class WithExternalUrl {

            @Test
            void whenExternalUrlIsDefined_thenTransformIt() throws URISyntaxException {
                ServiceAddress serviceAddress = new RegistryConfig().gatewayServiceAddress("https://host:123/path", false, false, null, 0);
                assertEquals("https", serviceAddress.getScheme());
                assertEquals("host:123", serviceAddress.getHostname());
            }

        }

        @Nested
        class WithoutExternalUrl {

            @Test
            void whenAttls_thenTransformIt() throws URISyntaxException {
                ServiceAddress serviceAddress = new RegistryConfig().gatewayServiceAddress(null, true, false, "hostname", 10010);
                assertEquals("https", serviceAddress.getScheme());
                assertEquals("hostname:10010", serviceAddress.getHostname());
            }

            @Test
            void whenSsl_thenTransformIt() throws URISyntaxException {
                ServiceAddress serviceAddress = new RegistryConfig().gatewayServiceAddress(null, false, true, "localhost", 10010);
                assertEquals("https", serviceAddress.getScheme());
                assertEquals("localhost:10010", serviceAddress.getHostname());
            }

            @Test
            void whenNoTtls_thenTransformIt() throws URISyntaxException {
                ServiceAddress serviceAddress = new RegistryConfig().gatewayServiceAddress(null, false, false, "localhost", 80);
                assertEquals("http", serviceAddress.getScheme());
                assertEquals("localhost:80", serviceAddress.getHostname());
            }

        }

    }

}