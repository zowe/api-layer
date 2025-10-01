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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.util.CorsUtils;

import java.lang.reflect.Field;
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

    @Nested
    class GivenATTLSIsEnabled {

        @Test
        void whenGetDefaultOrigins_thenAllowHttps() throws URISyntaxException {
            CorsBeans corsBeans = new CorsBeans(new ZuulProperties());
            when(environment.getActiveProfiles()).thenReturn(new String[]{ "attlsClient", "attlsServer" });

            List<String> allowedOrigins = corsBeans.getDefaultAllowedOrigins(environment, "https://dvipahost:10010", "lparhost", 10010);
            assertEquals(2, allowedOrigins.size());
            assertTrue(allowedOrigins.contains("https://dvipahost:10010"));
            assertTrue(allowedOrigins.contains("https://lparhost:10010"));
        }
    }

    @Nested
    @SpringBootTest(
        properties = {"apiml.service.corsEnabled=true"}
    )
    @ComponentScan(basePackages = "org.zowe.apiml.gateway")
    class GivenCorsEnabled {

        @Nested
        public class WhenCorsAllowedMethodsIsNotSet {

            @Autowired
            private CorsBeans corsBeans;

            @Test
            void validateDefaultCorsAllowedMethods() throws NoSuchFieldException, IllegalAccessException, URISyntaxException {
                CorsUtils corsUtils = corsBeans.corsUtils(environment, "https://dvipahost:10010", "lparhost", 10010);

                Field field = corsUtils.getClass().getDeclaredField("allowedCorsHttpMethods");
                field.setAccessible(true);
                List<String> corsAllowedMethods = (List<String>) field.get(corsUtils);
                assertEquals(7, corsAllowedMethods.size());
            }
        }

        @Nested
        @TestPropertySource(properties = {
            "apiml.service.corsAllowedMethods=GET,POST, PATCH"
        })
        @DirtiesContext
        public class WhenCorsAllowedMethodsIsSet {

            @Autowired
            private CorsBeans corsBeans;

            @Test
            void validateCorsAllowedMethods() throws NoSuchFieldException, IllegalAccessException, URISyntaxException {
                CorsUtils corsUtils = corsBeans.corsUtils(environment, "https://dvipahost:10010", "lparhost", 10010);

                Field field = corsUtils.getClass().getDeclaredField("allowedCorsHttpMethods");
                field.setAccessible(true);
                List<String> corsAllowedMethods = (List<String>) field.get(corsUtils);
                assertEquals(3, corsAllowedMethods.size());
                assertEquals("GET", corsAllowedMethods.get(0));
                assertEquals("POST", corsAllowedMethods.get(1));
                assertEquals("PATCH", corsAllowedMethods.get(2));
            }
        }
    }
}
