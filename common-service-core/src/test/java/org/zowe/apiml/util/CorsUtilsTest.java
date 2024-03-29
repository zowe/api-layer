/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.cors.CorsConfiguration;

import java.util.*;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CorsUtilsTest {

    Map<String, String> metadata = new HashMap<>();

    @BeforeEach
    void setup() {
        metadata.put("apiml.routes.v1.gateway", "api/v1");
        metadata.put("apiml.corsEnabled", "true");
    }

    @Nested
    class GivenCorsEnabled {
        CorsUtils corsUtils = new CorsUtils(true, Collections.emptyList());

        @Test
        void registerDefaultConfig() {
            corsUtils.registerDefaultCorsConfiguration((path, configuration) -> {
                    assertTrue(path.contains("gateway"));
                    assertNotNull(configuration.getAllowedHeaders());
                    assertEquals(1, configuration.getAllowedHeaders().size());
                    assertEquals(6, configuration.getAllowedMethods().size());
                }
            );
        }

        @Test
        void registerConfigForService() {

            corsUtils.setCorsConfiguration("dclient", metadata, (path, serviceId, configuration) -> {
                    assertEquals(metadata.get("apiml.routes.v1.gateway"), path);
                    assertNotNull(configuration.getAllowedHeaders());
                    assertEquals(1, configuration.getAllowedHeaders().size());
                    assertEquals(6, configuration.getAllowedMethods().size());
                }
            );

        }

        @Test
        void registerDefaultConfigForService() {
            metadata.remove("apiml.corsEnabled");
            corsUtils.setCorsConfiguration("dclient", metadata, (path, serviceId, configuration) -> {
                    assertEquals(metadata.get("apiml.routes.v1.gateway"), path);
                    assertNull(configuration.getAllowedMethods());
                }
            );
        }

        @Test
        void registerConfigForServiceWithCustomOrigins() {
            Map<String, String> customMetadata = new HashMap<>(metadata);
            customMetadata.put("apiml.corsAllowedOrigins", "https://localhost:3000,http://hostname.com,https://anothehostname:3040");
            corsUtils.setCorsConfiguration("dclient", customMetadata, (path, serviceId, configuration) -> {
                    assertEquals(metadata.get("apiml.routes.v1.gateway"), path);
                    assertNotNull(configuration.getAllowedHeaders());
                    assertTrue(configuration.getAllowedOrigins().contains("https://localhost:3000"));
                    assertEquals(3, configuration.getAllowedOrigins().size());
                    assertEquals(1, configuration.getAllowedHeaders().size());
                    assertEquals(6, configuration.getAllowedMethods().size());
                }
            );
        }

    }

    @Nested
    class GivenCorsDisabled {
        CorsUtils corsUtils = new CorsUtils(false, Collections.emptyList());

        @Test
        void registerEmptyDefaultConfig() {
            corsUtils.registerDefaultCorsConfiguration((path, configuration) -> {
                    assertNull(configuration.getAllowedHeaders());
                    assertNull(configuration.getAllowedMethods());
                }
            );
        }

        @Test
        void registerEmptyConfigForService() {
            corsUtils.setCorsConfiguration("dcclient", metadata, (path, serviceId, configuration) -> {
                    assertNull(configuration.getAllowedHeaders());
                    assertNull(configuration.getAllowedMethods());
                }
            );
        }
    }

    @Nested
    class Attls {

        @Test
        void setAllowedOrigins() {
            List<String> allowedOrigins = Arrays.asList("a");
            CorsUtils corsUtils = new CorsUtils(true, allowedOrigins);
            BiConsumer<String, CorsConfiguration> pathMapper = mock(BiConsumer.class);
            corsUtils.registerDefaultCorsConfiguration(pathMapper);

            ArgumentCaptor<CorsConfiguration> corsConfigurationCaptor = ArgumentCaptor.forClass(CorsConfiguration.class);

            verify(pathMapper, times(3)).accept(any(), corsConfigurationCaptor.capture());
            assertEquals(1, corsConfigurationCaptor.getValue().getAllowedOrigins().size());
            assertEquals("a", corsConfigurationCaptor.getValue().getAllowedOrigins().get(0));
        }

    }

}
