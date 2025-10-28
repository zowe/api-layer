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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.zowe.apiml.client.configuration.SecurityConfiguration;
import org.zowe.apiml.util.config.TestConfig;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ApiDocRedirectController.class})
@Import({ SecurityConfiguration.class, TestConfig.class })
class ApiDocRedirectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void callApiDocRedirect() throws Exception {
        this.mockMvc.perform(get("/docs/redirect"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", containsString("/v3/api-docs/apiv2")));
    }

}
