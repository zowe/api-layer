/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.metadata.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CorsMetadataProcessorTest {
    private CorsMetadataProcessor underTest;
    private UrlBasedCorsConfigurationSource configurationSource;
    private ArgumentCaptor<CorsConfiguration> configurationCaptor = ArgumentCaptor.forClass(CorsConfiguration.class);

    @BeforeEach
    void setUp() {
        EurekaApplications applications = mock(EurekaApplications.class);
        configurationSource = mock(UrlBasedCorsConfigurationSource.class);
        List<String> allowedHttpMethods = new ArrayList<>();
        allowedHttpMethods.add("GET");
        underTest = new CorsMetadataProcessor(applications, configurationSource, allowedHttpMethods);
    }

    @Test
    void corsIsEnabledPerService_allowedOriginsAreProvided() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("apiml.corsEnabled", "true");
        metadata.put("apiml.corsAllowedOrigins", "http://local1,http://local2");
        metadata.put("apiml.routes.0.gateway", "gateway");
        underTest.setCorsConfiguration("cors-enabled-origins-allowed", metadata);

        verify(configurationSource).registerCorsConfiguration(any(), configurationCaptor.capture());

        CorsConfiguration provided = configurationCaptor.getValue();
        assertDefaultConfiguration(provided);

        assertThat(provided.getAllowedOrigins(), hasSize(2));
        assertThat(provided.getAllowedOrigins().get(0), is("http://local1"));
        assertThat(provided.getAllowedOrigins().get(1), is("http://local2"));
    }

    @Test
    void corsIsEnabledPerService_allowedOriginsArentProvided() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("apiml.corsEnabled", "true");
        metadata.put("apiml.routes.0.gateway", "gateway");
        underTest.setCorsConfiguration("cors-enabled-all-origins", metadata);

        verify(configurationSource).registerCorsConfiguration(any(), configurationCaptor.capture());

        CorsConfiguration provided = configurationCaptor.getValue();
        assertDefaultConfiguration(provided);

        assertThat(provided.getAllowedOrigins(), hasSize(1));
        assertThat(provided.getAllowedOrigins().get(0), is("*"));
    }

    private void assertDefaultConfiguration(CorsConfiguration provided) {
        assertThat(provided.getAllowedHeaders(), hasSize(1));
        assertThat(provided.getAllowedHeaders().get(0), is("*"));
        assertThat(provided.getAllowedMethods(), hasSize(1));
        assertThat(provided.getAllowedMethods().get(0), is("GET"));
    }

    @Test
    void corsIsDisabledPerService() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("apiml.corsEnabled", "false");
        metadata.put("apiml.routes.0.gateway", "gateway");
        underTest.setCorsConfiguration("cors-disabled", metadata);

        verify(configurationSource).registerCorsConfiguration(any(), configurationCaptor.capture());

        CorsConfiguration provided = configurationCaptor.getValue();
        assertThat(provided.getAllowedHeaders(), is(nullValue()));
        assertThat(provided.getAllowedMethods(), is(nullValue()));
        assertThat(provided.getAllowedOrigins(), is(nullValue()));
    }

}
