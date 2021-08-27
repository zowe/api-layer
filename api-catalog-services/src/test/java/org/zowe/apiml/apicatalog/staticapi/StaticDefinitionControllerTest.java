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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.config.annotation.web.WebSecurityConfigurer;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = {StaticDefinitionController.class},
    excludeFilters = { @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = WebSecurityConfigurer.class) },
    excludeAutoConfiguration = { SecurityAutoConfiguration.class}
)
class StaticDefinitionControllerTest {

    private static final String STATIC_DEF_GENERATE_ENDPOINT = "/static-api/generate";
    private static final String STATIC_DEF_OVERRIDE_ENDPOINT = "/static-api/override";

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
                when(staticDefinitionGenerator.generateFile("services")).thenThrow(
                    new IOException("Exception")
                );

                mockMvc.perform(post(STATIC_DEF_GENERATE_ENDPOINT).content("services"))
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
                when(staticDefinitionGenerator.generateFile("invalid")).thenThrow(
                    new FileAlreadyExistsException("Exception")
                );

                mockMvc.perform(post(STATIC_DEF_GENERATE_ENDPOINT).content("invalid"))
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
                when(staticDefinitionGenerator.generateFile(payload)).thenReturn(
                    new StaticAPIResponse(201, "This is body")
                );

                mockMvc.perform(post(STATIC_DEF_GENERATE_ENDPOINT).content(payload))
                    .andExpect(status().is2xxSuccessful());
            }
        }

        @Nested
        class whenCallStaticOverrideAPI {
            @Test
            void thenResponseIs201() throws Exception {
                String payload = "\"services:\\n  - serviceId: service\\n    title: a\\n    description: description\\n    instanceBaseUrls:\\n      - a\\n   routes:\\n ";
                when(staticDefinitionGenerator.overrideFile(payload)).thenReturn(
                    new StaticAPIResponse(201, "This is body")
                );

                mockMvc.perform(post(STATIC_DEF_OVERRIDE_ENDPOINT).content(payload))
                    .andExpect(status().is2xxSuccessful());
            }
        }
    }

    @Configuration
    static class ContextConfiguration {

        @MockBean
        private StaticDefinitionGenerator staticDefinitionGenerator;

        @Bean
        public MessageService messageService() {
            return new YamlMessageService("/apicatalog-log-messages.yml");
        }

        @Bean
        public StaticDefinitionControllerExceptionHandler staticDefinitionControllerExceptionHandler() {
            return new StaticDefinitionControllerExceptionHandler(messageService());
        }

        @Bean
        public StaticDefinitionController staticAPIRefreshController() {
            return new StaticDefinitionController(staticDefinitionGenerator);
        }
    }
}
