/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SwaggerConfigTest {

    private final SwaggerConfig swaggerConfig = new SwaggerConfig();

    private final String DUMMY_OPENAPI =
        //language=JSON
        """
            {
              "openapi": "3.0.0",
              "paths": {},
              "components": {
                "schemas": { }
              }
            }
            """;

    @Test
    void servletEndpointsCustomizer() throws IOException {
        swaggerConfig.initServletEndpointDocLocation();
        var openApi = new OpenAPIV3Parser().readContents(DUMMY_OPENAPI).getOpenAPI();
        swaggerConfig.servletEndpoints().customise(openApi);
        assertFalse(openApi.getPaths().isEmpty());
        assertFalse(openApi.getComponents().getSchemas().isEmpty());
        assertNotNull(openApi.getTags());
    }

    //TODO consider create en exception in Sonar instead
    @Test
    void servletEndpointsCustomizer_works_tagsNotNull() throws IOException {
        swaggerConfig.initServletEndpointDocLocation();
        var openApi = new OpenAPIV3Parser().readContents(DUMMY_OPENAPI).getOpenAPI();
        openApi.setTags(new ArrayList<>());
        swaggerConfig.servletEndpoints().customise(openApi);
        assertFalse(openApi.getPaths().isEmpty());
        assertFalse(openApi.getComponents().getSchemas().isEmpty());
        assertNotNull(openApi.getTags());
    }

    //TODO consider create en exception in Sonar instead
    @Test
    void servletEndpointCustomizer_doesNotFail_whenOpenApiParserReturnsNull() {
        var servletEndpointDocLocationMock = mock(URI.class);
        ReflectionTestUtils.setField(swaggerConfig, "servletEndpointDocLocation", servletEndpointDocLocationMock);
        when(servletEndpointDocLocationMock.toString()).thenReturn(null);

        var openApi = new OpenAPIV3Parser().readContents(DUMMY_OPENAPI).getOpenAPI();
        swaggerConfig.servletEndpoints().customise(openApi);
        assertTrue(openApi.getPaths().isEmpty());
        assertTrue(openApi.getComponents().getSchemas().isEmpty());
        assertNull(openApi.getTags());
    }

    //TODO consider create en exception in Sonar instead
    @Test
    void doNotFailOnNullOpenApi() {
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(swaggerConfig, "customizeSwagger", (OpenAPI) null));
    }
}
