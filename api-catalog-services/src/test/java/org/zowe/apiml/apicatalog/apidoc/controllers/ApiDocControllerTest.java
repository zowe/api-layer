/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.apidoc.controllers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.zowe.apiml.apicatalog.apidoc.services.LocalApiDocService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
public class ApiDocControllerTest {

    private MockMvc mockMvc;

    @Mock
    private LocalApiDocService localApiDocService;

    private ApiDocController apiDocController;

    @Before
    public void setUp() {
        apiDocController = new ApiDocController(localApiDocService);
        mockMvc = MockMvcBuilders.standaloneSetup(apiDocController).build();
    }

    @Test
    public void callApiDocEndpoint() throws Exception {

        this.mockMvc.perform(get("/api-doc"))
            .andExpect(status().isOk());
    }
}
