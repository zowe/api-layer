/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.ConfigurableEnvironment;
import org.zowe.apiml.product.instance.ServiceAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class ZaasConfigTest {

    private static final String HOST = "hostA";
    private static final String PORT = "8888";
    private static final String SCHEME = "https";

    ConfigurableEnvironment env;

    @BeforeEach
    void setUp() {
        env = mock(ConfigurableEnvironment.class);
    }

    @Test
    void shouldReturnZaasAddress() {
        ServiceAddress zaasAddress = new ZaasConfig(env).getZaasAddress(HOST, PORT, SCHEME);
        assertEquals(HOST + ":" + PORT, zaasAddress.getHostname());
        assertEquals(SCHEME, zaasAddress.getScheme());
    }

}
