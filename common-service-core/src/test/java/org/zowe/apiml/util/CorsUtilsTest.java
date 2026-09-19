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
import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CorsUtilsTest {

    private Map<String, String> metadata = new HashMap<>();
    private List<String> defaultCorsMethods = List.of("GET", "HEAD", "POST", "PATCH", "DELETE", "PUT", "OPTIONS");

    private List<String> allowedEndpoints = List.of("/gateway/**");

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
                    .defaultAllowedCorsOrigins(Collections.emptyList())
                    .defaultAllowedCorsHeaders(List.of("*"))
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

                corsUtils.setCorsConfiguration("dclient", metadata, (path, configuration) -> {
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
                corsUtils.setCorsConfiguration("dclient", metadata, (path, configuration) -> {
                        assertEquals(metadata.get("apiml.routes.v1.gateway"), path);
                        assertTrue(configuration.getAllowCredentials());
                        assertEquals(List.of("GET", "HEAD", "POST", "PATCH", "DELETE", "PUT", "OPTIONS"), configuration.getAllowedMethods());
                    }
                );
            }

            @Test
            void registerConfigForServiceWithCustomOrigins() {
                Map<String, String> customMetadata = new HashMap<>(metadata);
                customMetadata.put("apiml.corsAllowedOrigins", "https://localhost:3000,http://hostname.com,https://anothehostname:3040");
                corsUtils.setCorsConfiguration("dclient", customMetadata, (path, configuration) -> {
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
                .defaultAllowedCorsOrigins(List.of("https://localhost3:10010"))
                .defaultAllowedCorsHeaders(List.of("*"))
                .defaultAllowedCorsHttpMethods(List.of("GET", "HEAD"))
                .build();
        }

        @Test
        void registerEmptyDefaultConfig() {
            corsUtils.registerDefaultCorsConfiguration((path, configuration) -> {
                    assertNull(configuration.getAllowedOrigins());
                    assertNull(configuration.getAllowedHeaders());
                    assertNull(configuration.getAllowedMethods());
                }
            );
        }

        @Test
        void registerEmptyConfigForService() {
            corsUtils.setCorsConfiguration("dcclient", metadata, (path, configuration) -> {
                    assertNull(configuration.getAllowedHeaders());
                    assertNull(configuration.getAllowedMethods());
                }
            );
        }

    }

    @Nested
    class GivenWildcardOrigin {

        private static final String CLIENT_ORIGIN = "https://client.example.com";

        private final List<String> defaultCorsMethods = List.of("GET", "HEAD", "POST", "PATCH", "DELETE", "PUT", "OPTIONS");

        private CorsUtils corsUtils(List<String> defaultOrigins, boolean gatewayCorsEnabled, boolean defaultAllowCredentials) {
            return CorsUtils.builder()
                .gatewayCorsEnabled(gatewayCorsEnabled)
                .corsAllowedEndpoints(allowedEndpoints)
                .defaultAllowedCorsOrigins(defaultOrigins)
                .defaultAllowedCorsHeaders(List.of("*"))
                .defaultAllowedCorsHttpMethods(defaultCorsMethods)
                .defaultAllowCredentials(defaultAllowCredentials)
                .build();
        }

        private CorsConfiguration configuredForService(CorsUtils corsUtils, Map<String, String> serviceMetadata) {
            var captured = new AtomicReference<CorsConfiguration>();
            corsUtils.setCorsConfiguration("dclient", serviceMetadata, (path, configuration) -> captured.set(configuration));
            assertNotNull(captured.get(), "No CORS configuration was registered for the service");
            return captured.get();
        }

        private CorsConfiguration defaultConfiguration(CorsUtils corsUtils) {
            var captured = new AtomicReference<CorsConfiguration>();
            corsUtils.registerDefaultCorsConfiguration((path, configuration) -> captured.set(configuration));
            assertNotNull(captured.get(), "No default CORS configuration was registered");
            return captured.get();
        }

        private void assertWildcardPatternUsed(CorsConfiguration configuration) {
            assertTrue(configuration.getAllowedOrigins() == null || !configuration.getAllowedOrigins().contains("*"),
                "A literal wildcard must not be registered as an allowed origin");
            assertEquals(List.of("*"), configuration.getAllowedOriginPatterns());
            assertDoesNotThrow(configuration::validateAllowCredentials);
        }

        @Test
        void givenServiceWildcardAndCredentialsEnabled_thenOriginPatternUsed() {
            var serviceMetadata = new HashMap<>(metadata);
            serviceMetadata.put("apiml.corsAllowedOrigins", "*");
            serviceMetadata.put("apiml.corsAllowCredentials", "true");

            var configuration = configuredForService(corsUtils(Collections.emptyList(), true, true), serviceMetadata);

            assertWildcardPatternUsed(configuration);
            assertTrue(configuration.getAllowCredentials());
            assertEquals(CLIENT_ORIGIN, configuration.checkOrigin(CLIENT_ORIGIN));
        }

        @Test
        void givenServiceWildcardAndCredentialsDisabled_thenWildcardAcceptedWithoutCredentials() {
            var serviceMetadata = new HashMap<>(metadata);
            serviceMetadata.put("apiml.corsAllowedOrigins", "*");
            serviceMetadata.put("apiml.corsAllowCredentials", "false");

            var configuration = configuredForService(corsUtils(Collections.emptyList(), true, true), serviceMetadata);

            assertWildcardPatternUsed(configuration);
            assertFalse(configuration.getAllowCredentials(), "Credentials must not be enabled implicitly");
            assertEquals(CLIENT_ORIGIN, configuration.checkOrigin(CLIENT_ORIGIN));
        }

        @Test
        void givenServiceWildcardMixedWithExplicitOrigins_thenNoLiteralWildcard() {
            var serviceMetadata = new HashMap<>(metadata);
            serviceMetadata.put("apiml.corsAllowedOrigins", "https://a.example.com,*");

            var configuration = configuredForService(corsUtils(Collections.emptyList(), true, true), serviceMetadata);

            assertWildcardPatternUsed(configuration);
            assertEquals(List.of("https://a.example.com"), configuration.getAllowedOrigins());
            assertEquals(CLIENT_ORIGIN, configuration.checkOrigin(CLIENT_ORIGIN));
        }

        @Test
        void givenServiceExplicitOriginsOnly_thenListedAcceptedAndUnlistedRejected() {
            var serviceMetadata = new HashMap<>(metadata);
            serviceMetadata.put("apiml.corsAllowedOrigins", "https://a.example.com");

            var configuration = configuredForService(corsUtils(Collections.emptyList(), true, true), serviceMetadata);

            assertEquals(List.of("https://a.example.com"), configuration.getAllowedOrigins());
            assertTrue(configuration.getAllowedOriginPatterns() == null || configuration.getAllowedOriginPatterns().isEmpty());
            assertEquals("https://a.example.com", configuration.checkOrigin("https://a.example.com"));
            assertNull(configuration.checkOrigin(CLIENT_ORIGIN));
        }

        @Test
        void givenGatewayDefaultWildcard_thenOriginPatternUsed() {
            var configuration = configuredForService(corsUtils(List.of("*"), true, true), new HashMap<>(metadata));

            assertWildcardPatternUsed(configuration);
            assertTrue(configuration.getAllowCredentials());
            assertEquals(CLIENT_ORIGIN, configuration.checkOrigin(CLIENT_ORIGIN));
        }

        @Test
        void givenCorsDisabledForServiceWithWildcardDefault_thenOriginPatternUsed() {
            var serviceMetadata = new HashMap<>(metadata);
            serviceMetadata.put("apiml.corsEnabled", "false");

            var configuration = configuredForService(corsUtils(List.of("*"), true, true), serviceMetadata);

            assertWildcardPatternUsed(configuration);
            assertTrue(configuration.getAllowCredentials());
        }

        @Test
        void givenGatewayDefaultsWithWildcard_thenRegisterDefaultConfigUsesPattern() {
            var configuration = defaultConfiguration(corsUtils(List.of("*"), true, true));

            assertWildcardPatternUsed(configuration);
            assertTrue(configuration.getAllowCredentials());
        }

    }

}
