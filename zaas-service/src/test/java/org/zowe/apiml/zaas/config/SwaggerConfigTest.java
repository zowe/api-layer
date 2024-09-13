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

import io.swagger.v3.parser.OpenAPIV3Parser;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

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
}
