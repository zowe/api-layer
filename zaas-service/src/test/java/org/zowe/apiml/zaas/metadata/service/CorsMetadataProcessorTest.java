/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.metadata.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.zowe.apiml.util.CorsUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CorsMetadataProcessorTest {

    private CorsUtils corsUtils;
    @Mock
    private UrlBasedCorsConfigurationSource configurationSource;
    @Captor
    private ArgumentCaptor<CorsConfiguration> configurationCaptor;
    private List<String> allowedEndpoints = List.of("/gateway/**");

    @BeforeEach
    void setUp() {
        corsUtils = CorsUtils.builder()
            .gatewayCorsEnabled(true)
            .defaultAllowedCorsHttpMethods(List.of("GET", "HEAD", "POST", "PATCH", "DELETE", "PUT", "OPTIONS"))
            .corsAllowedEndpoints(allowedEndpoints)
            .defaultAllowedCorsOrigins(List.of("https://localhost:10010"))
            .defaultAllowedCorsHeaders(List.of("*"))
            .build();
    }

    @Nested
    class GivenCorsEnabled {

        @Test
        void corsIsEnabledPerService_allowedOriginsAreProvided() {

            Map<String, String> metadata = new HashMap<>();
            metadata.put("apiml.corsEnabled", "true");
            metadata.put("apiml.corsAllowedOrigins", "http://local1,http://local2");
            metadata.put("apiml.routes.0.gateway", "gateway");
            corsUtils.setCorsConfiguration("cors-enabled-origins-allowed", metadata, (entry, config) -> configurationSource.registerCorsConfiguration("/" + entry + "/cors-enabled-origins-allowed/**", config));

            verify(configurationSource).registerCorsConfiguration(any(), configurationCaptor.capture());

            CorsConfiguration provided = configurationCaptor.getValue();
            assertDefaultConfiguration(provided);

            assertThat(provided.getAllowedOrigins(), hasSize(3));
            assertThat(provided.getAllowedOrigins().get(0), is("https://localhost:10010"));
            assertThat(provided.getAllowedOrigins().get(1), is("http://local1"));
            assertThat(provided.getAllowedOrigins().get(2), is("http://local2"));
        }

        @Test
        @Disabled("Not anymore")
        void corsIsEnabledPerService_allowedOriginsArentProvided() {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("apiml.corsEnabled", "true");
            metadata.put("apiml.routes.0.gateway", "gateway");
            corsUtils.setCorsConfiguration("cors-enabled-all-origins", metadata, (entry, config) -> configurationSource.registerCorsConfiguration("/" + entry + "/cors-enabled-all-origins/**", config));

            verify(configurationSource).registerCorsConfiguration(any(), configurationCaptor.capture());

            CorsConfiguration provided = configurationCaptor.getValue();
            assertDefaultConfiguration(provided);

            assertThat(provided.getAllowedOriginPatterns(), hasSize(1));
            assertThat(provided.getAllowedOriginPatterns().get(0), is("*"));
        }

        private void assertDefaultConfiguration(CorsConfiguration provided) {
            assertThat(provided.getAllowedHeaders(), hasSize(1));
            assertThat(provided.getAllowedHeaders().get(0), is("*"));
            assertThat(provided.getAllowedMethods(), hasSize(7));
            assertThat(provided.getAllowedMethods().get(0), is("GET"));
        }
    }

    @Nested
    class GivenCorsDisabled {
        @Test
        void corsIsDisabledPerService() {

            Map<String, String> metadata = new HashMap<>();
            metadata.put("apiml.corsEnabled", "false");
            metadata.put("apiml.routes.0.gateway", "gateway");
            corsUtils.setCorsConfiguration("cors-disabled", metadata, (entry, config) -> configurationSource.registerCorsConfiguration("/" + entry + "/cors-disabled/**", config));
            verify(configurationSource).registerCorsConfiguration(any(), configurationCaptor.capture());
        }

    }

}
