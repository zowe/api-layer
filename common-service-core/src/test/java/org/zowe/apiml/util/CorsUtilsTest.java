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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class CorsUtilsTest {

    private Map<String, String> metadata = new HashMap<>();
    private List<String> defaultCorsMethods = Stream.of("GET", "HEAD", "POST", "PATCH", "DELETE", "PUT", "OPTIONS").collect(Collectors.toList());

    private List<String> allowedEndpoints = new ArrayList<>(Collections.singletonList("/gateway/**"));

    @BeforeEach
    void setup() {
        metadata.clear();
        metadata.put("apiml.routes.v1.gateway", "api/v1");
        metadata.put("apiml.corsEnabled", "true");
    }

    @Nested
    class GivenCorsEnabled {

        @Nested
        class GivenDefaultCorsAllowedMethods {

            private CorsUtils corsUtils;

            @BeforeEach
            void setUp() {
                corsUtils = CorsUtils.builder()
                    .gatewayCorsEnabled(true)
                    .corsAllowedEndpoints(allowedEndpoints)
                    .defaultAllowedCorsHttpMethods(defaultCorsMethods)
                    .defaultAllowedCorsOrigins(new ArrayList<>())
                    .defaultAllowedCorsHeaders(new ArrayList<>(Collections.singletonList("*")))
                    .defaultAllowCredentials(true)
                    .build();
            }

            @Test
            void registerDefaultConfig() {
                corsUtils.registerDefaultCorsConfiguration((path, configuration) -> {
                        assertTrue(path.contains("gateway"));
                        assertNotNull(configuration.getAllowedHeaders());
                        assertEquals(1, configuration.getAllowedHeaders().size());
                        assertEquals(defaultCorsMethods.size(), configuration.getAllowedMethods().size());
                    }
                );
            }

            @Test
            void registerConfigForService() {

                corsUtils.setCorsConfiguration("service", metadata, (path, configuration) -> {
                        assertEquals(metadata.get("apiml.routes.v1.gateway"), path);
                        assertNotNull(configuration.getAllowedHeaders());
                        assertEquals(1, configuration.getAllowedHeaders().size());
                        assertEquals(defaultCorsMethods.size(), configuration.getAllowedMethods().size());
                    }
                );

            }

            @Test
            void registerDefaultConfigForService() {
                metadata.remove("apiml.corsEnabled");
                corsUtils.setCorsConfiguration("service", metadata, (path, configuration) -> {
                        assertEquals(metadata.get("apiml.routes.v1.gateway"), path);
                        assertTrue(configuration.getAllowCredentials());
                        assertEquals(Arrays.asList("GET", "HEAD", "POST", "PATCH", "DELETE", "PUT", "OPTIONS"), configuration.getAllowedMethods());
                    }
                );
            }

            @Test
            void registerConfigForServiceWithCustomOrigins() {
                Map<String, String> customMetadata = new HashMap<>(metadata);
                customMetadata.put("apiml.corsAllowedOrigins", "https://localhost:3000,http://hostname.com,https://anothehostname:3040");
                corsUtils.setCorsConfiguration("service", customMetadata, (path, configuration) -> {
                        assertEquals(metadata.get("apiml.routes.v1.gateway"), path);
                        assertNotNull(configuration.getAllowedHeaders());
                        assertTrue(configuration.getAllowedOrigins().contains("https://localhost:3000"));
                        assertEquals(3, configuration.getAllowedOrigins().size());
                        assertEquals(1, configuration.getAllowedHeaders().size());
                        assertEquals(defaultCorsMethods.size(), configuration.getAllowedMethods().size());
                    }
                );
            }

        }

    }

    @Nested
    class GivenCorsDisabled {

        private CorsUtils corsUtils;

        @BeforeEach
        void setUp() {
            corsUtils = CorsUtils.builder()
                .gatewayCorsEnabled(false)
                .corsAllowedEndpoints(Arrays.asList("/gateway/**", "/api-docs"))
                .defaultAllowedCorsOrigins(Collections.singletonList("https://localhost3:10010"))
                .defaultAllowedCorsHeaders(Collections.singletonList("*"))
                .defaultAllowedCorsHttpMethods(Arrays.asList("GET", "HEAD"))
                .build();
        }

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
            corsUtils.setCorsConfiguration("service", metadata, (path, configuration) -> {
                    assertNull(configuration.getAllowedHeaders());
                    assertNull(configuration.getAllowedMethods());
                }
            );
        }
    }

}
