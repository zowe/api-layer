/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.apidoc.controllers;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.zowe.apiml.gateway.apidoc.services.LocalApiDocService;

import java.io.IOException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
public class ApiDocControllerTest {

    private MockMvc mockMvc;

    @Mock
    private LocalApiDocService localApiDocService;

    private ApiDocController apiDocController;

    private String swaggerLocation = "gateway-api-doc.json";
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
        apiDocController = new ApiDocController(swaggerLocation, localApiDocService);
        mockMvc = MockMvcBuilders.standaloneSetup(apiDocController).build();
    }

    @Test
    public void callApiDocEndpoint() throws Exception {
        this.mockMvc.perform(get("/api-doc"))
            .andExpect(status().isOk());
    }

    @Test
    public void shouldThrowExpection_whenCallingApiDocEndpoint() throws Exception {
        apiDocController = new ApiDocController("wrongLocation", localApiDocService);
        mockMvc = MockMvcBuilders.standaloneSetup(apiDocController).build();

        exception.expect(IOException.class);
        exception.expectMessage("Cannot find Api Documentation (swagger) file: classpath:wrongLocation");

        this.mockMvc.perform(get("/api-doc"));
    }
}

