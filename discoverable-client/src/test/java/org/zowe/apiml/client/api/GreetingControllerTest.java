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

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = {GreetingController.class})
class GreetingControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void callGreetingEndpoint() throws Exception {
        String name = "Petr";

        this.mockMvc.perform(get("/api/v1/greeting?name=" + name))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", is("Hello, " + name + "!")));
    }

    @Test
    void callGreetingEndpointWithDelay() throws Exception {
        String name = "Petr";

        this.mockMvc.perform(get("/api/v1/greeting?name=" + name + "&delayMs=100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", is("Hello, " + name + "!")));
    }

    @Test
    void callPlainGreeting() throws Exception {

        this.mockMvc.perform(get("/api/v1/greeting"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", is("Hello, world!")));
    }

    @Test
    void callCustomGreetingEndpoint() throws Exception {
        String name = "Petr";

        this.mockMvc.perform(get("/api/v1/" + name + "/greeting"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", is("Hello, " + name + "!")));
    }
}
