/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.apicatalog.staticapi;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.config.annotation.web.WebSecurityConfigurer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = {StaticDefinitionController.class},
    excludeFilters = {@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = WebSecurityConfigurer.class)},
    excludeAutoConfiguration = {SecurityAutoConfiguration.class}
)
@ContextConfiguration(classes = StaticApiContextConfiguration.class)
class StaticDefinitionControllerTest {

    private static final String STATIC_DEF_GENERATE_ENDPOINT = "/static-api/generate";
    private static final String STATIC_DEF_OVERRIDE_ENDPOINT = "/static-api/override";
    private static final String STATIC_DEF_DELETE_ENDPOINT = "/static-api/delete";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StaticDefinitionGenerator staticDefinitionGenerator;

    @Nested
    class GivenIOException {
        @Nested
        class whenCallStaticGenerationAPI {
            @Test
            void thenResponseShouldBe500WithSpecificMessage() throws Exception {
                when(staticDefinitionGenerator.generateFile("services", "test")).thenThrow(
                    new IOException("Exception")
                );

                mockMvc.perform(post(STATIC_DEF_GENERATE_ENDPOINT).header("Service-Id", "test").content("services"))
                    .andExpect(jsonPath("$.messages", hasSize(1)))
                    .andExpect(jsonPath("$.messages[0].messageType").value("ERROR"))
                    .andExpect(jsonPath("$.messages[0].messageNumber").value("ZWEAC709E"))
                    .andExpect(jsonPath("$.messages[0].messageContent").value("Static definition generation failed, caused by exception: java.io.IOException: Exception"))
                    .andExpect(jsonPath("$.messages[0].messageKey").value("org.zowe.apiml.apicatalog.StaticDefinitionGenerationFailed"))
                    .andExpect(status().isInternalServerError());
            }
        }
    }

    @Nested
    class GivenRequestWithNoContent {
        @Nested
        class whenCallStaticGenerationAPI {
            @Test
            void thenResponseIs400() throws Exception {
                mockMvc.perform(post(STATIC_DEF_GENERATE_ENDPOINT))
                    .andExpect(status().isBadRequest());
            }
        }
    }

    @Nested
    class GivenFileAlreadyExistsException {
        @Nested
        class whenCallStaticGenerationAPI {
            @Test
            void thenResponseShouldBe409WithSpecificMessage() throws Exception {
                when(staticDefinitionGenerator.generateFile("invalid", "test")).thenThrow(
                    new FileAlreadyExistsException("Exception")
                );

                mockMvc.perform(post(STATIC_DEF_GENERATE_ENDPOINT).header("Service-Id", "test").content("invalid"))
                    .andExpect(jsonPath("$.messages", hasSize(1)))
                    .andExpect(jsonPath("$.messages[0].messageType").value("ERROR"))
                    .andExpect(jsonPath("$.messages[0].messageNumber").value("ZWEAC709E"))
                    .andExpect(jsonPath("$.messages[0].messageContent").value("Static definition generation failed, caused by exception: java.nio.file.FileAlreadyExistsException: Exception"))
                    .andExpect(jsonPath("$.messages[0].messageKey").value("org.zowe.apiml.apicatalog.StaticDefinitionGenerationFailed"))
                    .andExpect(status().isConflict());
            }
        }
    }

    @Nested
    class GivenRequestWithValidContent {
        @Nested
        class whenCallStaticGenerationAPI {
            @Test
            void thenResponseIs201() throws Exception {
                String payload = "\"services:\\n  - serviceId: service\\n    title: a\\n    description: description\\n    instanceBaseUrls:\\n      - a\\n   routes:\\n ";
                when(staticDefinitionGenerator.generateFile(payload, "service")).thenReturn(
                    new StaticAPIResponse(201, "This is body")
                );

                mockMvc.perform(post(STATIC_DEF_GENERATE_ENDPOINT).content(payload).header("Service-Id", "service"))
                    .andExpect(status().is2xxSuccessful());
            }
        }

        @Nested
        class whenCallStaticOverrideAPI {
            @Test
            void thenResponseIs201() throws Exception {
                String payload = "\"services:\\n  - serviceId: service\\n    title: a\\n    description: description\\n    instanceBaseUrls:\\n      - a\\n   routes:\\n ";
                when(staticDefinitionGenerator.overrideFile(payload, "service")).thenReturn(
                    new StaticAPIResponse(201, "This is body")
                );

                mockMvc.perform(post(STATIC_DEF_OVERRIDE_ENDPOINT).content(payload).header("Service-Id", "service"))
                    .andExpect(status().is2xxSuccessful());
            }
        }

        @Nested
        class WhenCallDelete {

            @Test
            void givenValidId_thenResponseIsOK() throws Exception {
                when(staticDefinitionGenerator.deleteFile("test-service")).thenReturn(new StaticAPIResponse(201, "OK"));
                mockMvc.perform(delete(STATIC_DEF_DELETE_ENDPOINT).header("Service-Id", "test-service"))
                    .andExpect(status().is2xxSuccessful());
            }
        }
    }
}
