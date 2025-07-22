/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.client.api;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.zowe.apiml.client.configuration.SecurityConfiguration;
import org.zowe.apiml.util.config.TestConfig;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {StatusCodeController.class})
@Import({ SecurityConfiguration.class, TestConfig.class })
class StatusCodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    class GivenGetStatusCodeRequest {

        @Test
        void whenCallingWithParam_thenReturnCreated() throws Exception {
            mockMvc.perform(get("/api/v1/status-code").param("code", "201"))
                .andExpect(status().isCreated())
                .andExpect(content().string("status code: 201"));
        }

        @Test
        void whenCallingWithoutParam_thenReturnDefault() throws Exception {
            mockMvc.perform(get("/api/v1/status-code"))
                .andExpect(status().isOk())
                .andExpect(content().string("status code: 200"));
        }

    }

    @Nested
    class GivenPostStatusCodeRequest {

        @Test
        void whenCallingWithParam_thenReturnCreated() throws Exception {
            mockMvc.perform(post("/api/v1/status-code").param("code", "201"))
                .andExpect(status().isCreated())
                .andExpect(content().string("status code: 201"));
        }

        @Test
        void whenCallingWithoutParam_thenReturnDefault() throws Exception {
            mockMvc.perform(post("/api/v1/status-code"))
                .andExpect(status().isOk())
                .andExpect(content().string("status code: 200"));
        }
    }
}
