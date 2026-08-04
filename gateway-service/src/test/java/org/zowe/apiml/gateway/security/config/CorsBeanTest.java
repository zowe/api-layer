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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.acceptance.common.AcceptanceTest;
import org.zowe.apiml.acceptance.common.AcceptanceTestWithBasePath;
import org.zowe.apiml.util.CorsUtils;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// For now, it's only to verify at-tls settings are the correct ones
@ExtendWith(MockitoExtension.class)
class CorsBeanTest {

    @Mock
    private Environment environment;

    @Nested
    class GivenATTLSIsEnabled {

        @Test
        void whenGetDefaultOrigins_thenAllowHttps() throws Exception {
            CorsBeans corsBeans = new CorsBeans(new ZuulProperties());
            ReflectionTestUtils.setField(corsBeans, "hostname", "lparhost");
            ReflectionTestUtils.setField(corsBeans, "port", "10010");
            corsBeans.afterPropertiesSet();

            List<String> allowedOrigins = corsBeans.getDefaultAllowedOrigins(new ArrayList<>(Arrays.asList("https://dvipahost:10010")));
            assertEquals(2, allowedOrigins.size());
            assertTrue(allowedOrigins.contains("https://dvipahost:10010"));
            assertTrue(allowedOrigins.contains("https://lparhost:10010"));
        }

    }

}

@TestPropertySource(properties = {"apiml.service.corsEnabled=true"})
@ActiveProfiles({"test", "GivenCorsEnabled"})
@AcceptanceTest
class CorsEnabledAcceptanceTest extends AcceptanceTestWithBasePath {

    @Mock
    private Environment environment;

    @Nested
    public class WhenCorsAllowedMethodsIsNotSet {

        @Autowired
        private CorsBeans corsBeans;

        @Test
        void validateDefaultCors() throws URISyntaxException {
            CorsUtils corsUtils = corsBeans.corsUtils(environment, "https://dvipahost:10010", "lparhost", 10010);

            @SuppressWarnings("unchecked")
            List<String> corsAllowedMethods = (List<String>) ReflectionTestUtils.getField(corsUtils, "defaultAllowedCorsHttpMethods");
            assertEquals(7, corsAllowedMethods.size());
            Boolean allowCredentials = (Boolean) ReflectionTestUtils.getField(corsUtils, "defaultAllowedCredentials");
            assertTrue(allowCredentials);
        }
    }

    @Nested
    @TestPropertySource(properties = {
        "apiml.service.corsEnabled=true",
        "apiml.service.corsAllowedMethods=GET,POST,PATCH"
    })
    @AcceptanceTest
    public class WhenCorsAllowedMethodsIsSet {

        @Autowired
        private CorsBeans corsBeans;

        @Test
        void validateCorsAllowedMethods() throws URISyntaxException {
            CorsUtils corsUtils = corsBeans.corsUtils(environment, "https://dvipahost:10010", "lparhost", 10010);

            @SuppressWarnings("unchecked")
            List<String> corsAllowedMethods = (List<String>) ReflectionTestUtils.getField(corsUtils, "defaultAllowedCorsHttpMethods");
            assertEquals(3, corsAllowedMethods.size());
            assertEquals("GET", corsAllowedMethods.get(0));
            assertEquals("POST", corsAllowedMethods.get(1));
            assertEquals("PATCH", corsAllowedMethods.get(2));
        }

    }

}
