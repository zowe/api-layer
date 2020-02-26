/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.apidoc.reader;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
public class ApiDocControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ApiDocReader apiDocReader;

    @BeforeEach
    public void setUp() {
        ApiDocController apiDocController = new ApiDocController(apiDocReader);
        mockMvc = MockMvcBuilders.standaloneSetup(apiDocController).build();
    }

    @Test
    public void callApiDocEndpoint() throws Exception {

        OpenAPI openApi = getDummyOpenApiObject();

        Mockito.when(apiDocReader.load(any())).thenReturn(openApi);

        this.mockMvc.perform(get("/api-doc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.openapi", is("3.0.0")))
            .andExpect(jsonPath("$.info.title", is("Service title")))
            .andExpect(jsonPath("$.info.description", is("Service description")))
            .andExpect(jsonPath("$.info.version", is("1.0.0")))
            .andExpect(jsonPath("$.servers[0].url", is("/api/v1/apicatalog")));
    }

    private OpenAPI getDummyOpenApiObject() {
        List<Server> servers = new ArrayList<>();
        servers.add(0, new Server().url("/api/v1/apicatalog"));
        OpenAPI openAPI = new OpenAPI();
        openAPI.setPaths(new Paths());
        openAPI.setTags(new ArrayList<>());
        openAPI.setOpenapi("3.0.0");
        openAPI.setServers(servers);

        Info info = new Info();
        info.setTitle("Service title");
        info.setDescription("Service description");
        info.setVersion("1.0.0");
        openAPI.setInfo(info);

        return openAPI;
    }
}
