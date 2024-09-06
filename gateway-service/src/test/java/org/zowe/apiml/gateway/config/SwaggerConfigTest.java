/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.servers.Server;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SwaggerConfigTest {

    @Nested
    class UpdatePaths {

        @Test
        void givenWildCardWithStars_whenUpdating_thenProcess() {
            PathItem pathItem1 = new PathItem();
            PathItem pathItem2 = new PathItem();
            OpenAPI openAPI = new OpenAPI()
                .servers(Collections.singletonList(new Server().url("https://localhost:10010/contextPath")))
                .path("/api/v1/test", pathItem1)
                .path("/api/v1/test2", pathItem2);
            new SwaggerConfig(null, null).updatePaths(openAPI, "/api/v1/**");
            assertEquals(2, openAPI.getPaths().size());
            assertEquals("https://localhost:10010/contextPath/api/v1/", openAPI.getServers().get(0).getUrl());
            assertSame(pathItem1, openAPI.getPaths().get("/test"));
            assertSame(pathItem2, openAPI.getPaths().get("/test2"));
        }

        @Test
        void givenNotMatchingEndpoint_whenUpdating_thenLeaveIt() {
            OpenAPI openAPI = new OpenAPI()
                .servers(Collections.singletonList(new Server().url("https://localhost:10010/")))
                .path("/api/v1/test", new PathItem())
                .path("/different/test", new PathItem());
            new SwaggerConfig(null, null).updatePaths(openAPI, "/api/v1");
            assertEquals("https://localhost:10010/api/v1", openAPI.getServers().get(0).getUrl());
            assertNotNull(openAPI.getPaths().get("/test"));
            assertNotNull(openAPI.getPaths().get("/different/test"));
        }

    }

    @Nested
    class Customizer {

        @Test
        void givenOpenApiCustomizer_whenCustomizing_thenUpdatePaths() throws IOException, URISyntaxException {
            URI zaasUri = new URI("http://service/api-doc.json");
            String openApi = IOUtils.toString(new ClassPathResource("api-doc.json").getInputStream(), StandardCharsets.UTF_8);

            OpenAPI openAPI = new OpenAPI();
            SwaggerConfig swaggerConfig = spy(new SwaggerConfig(null, null));
            doReturn(openApi).when(swaggerConfig).download(zaasUri);
            ReflectionTestUtils.setField(swaggerConfig, "zaasUri", zaasUri);
            swaggerConfig.servletEndpoints("/some/path").customise(openAPI);
            verify(swaggerConfig).updatePaths(openAPI, "/some/path");
        }

    }

}
