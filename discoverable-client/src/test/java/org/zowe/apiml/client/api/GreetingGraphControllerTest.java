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
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.zowe.apiml.client.configuration.SecurityConfiguration;

import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = {GreetingGraphController.class})
@Import(SecurityConfiguration.class)
class GreetingGraphControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    class GivenGreetingGraphRequest {

        @Test
        void whenCallingWithParam_thenReturnOk() throws Exception {
            mockMvc.perform(get("/graphql/v1/greeting")
                    .param("name", "Andrea"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", is("Hi, Andrea!")));
        }
    }
}
