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
import jakarta.annotation.PostConstruct;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

@Configuration
public class SwaggerConfig {

    private URI servletEndpointDocLocation;

    @PostConstruct
    void initServletEndpointDocLocation() throws IOException {
        servletEndpointDocLocation = new ClassPathResource("zaas-api-doc.json").getURI();
    }

    @Bean
    public OpenApiCustomizer servletEndpoints() {
        return this::customizeSwagger;
    }

    private void customizeSwagger(OpenAPI openApi) {
            if (openApi != null) {
                OpenAPI servletEndpoints = new OpenAPIV3Parser().read(servletEndpointDocLocation.toString());
                if (servletEndpoints != null) {
                    for (var entry : servletEndpoints.getPaths().entrySet()) {
                        openApi.getPaths().addPathItem(entry.getKey(), entry.getValue());
                    }
                    openApi.getComponents().getSchemas().putAll(servletEndpoints.getComponents().getSchemas());
                    if (openApi.getTags() == null) {
                        openApi.setTags(new ArrayList<>());
                    }
                    openApi.getTags().addAll(servletEndpoints.getTags());
                }
            }
    }

    @Bean
    public GroupedOpenApi groupedOpenApiAuth() {
        return GroupedOpenApi.builder()
            .group("auth").pathsToMatch("/zaas/api/v1/auth/**")
            .addOpenApiCustomizer(servletEndpoints())
            .build();
    }

    @Bean
    public GroupedOpenApi groupedOpenApiAll() {
        return GroupedOpenApi.builder()
            .group("v1").pathsToMatch("/**")
                .addOpenApiCustomizer(servletEndpoints())
            .build();
    }

}
