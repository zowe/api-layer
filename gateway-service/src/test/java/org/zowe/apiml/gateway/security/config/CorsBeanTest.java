/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.core.env.Environment;

import java.net.URISyntaxException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

// For now, it's only to verify at-tls settings are the correct ones
@ExtendWith(MockitoExtension.class)
class CorsBeanTest {

    @Mock
    private Environment environment;

    private CorsBeans corsBeans;

    @BeforeEach
    void setUp() {
        this.corsBeans = new CorsBeans(new ZuulProperties());
    }

    @Nested
    class GivenATTLSIsEnabled {

        @Test
        void whenGetDefaultOrigins_thenAllowHttps() throws URISyntaxException {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"attls"});

            List<String> allowedOrigins = corsBeans.getDefaultAllowedOrigins(environment, "https://dvipahost:10010", "lparhost", 10010);
            assertEquals(2, allowedOrigins.size());
            assertTrue(allowedOrigins.contains("https://dvipahost:10010"));
            assertTrue(allowedOrigins.contains("https://lparhost:10010"));
        }
    }
}
