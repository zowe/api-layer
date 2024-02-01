/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.client.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfiguration {

    @Value("${apiml.service.title}")
    private String apiTitle;

    @Value("${apiml.service.apiInfo[0].version}")
    private String apiVersionRest1;

    @Value("${apiml.service.apiInfo[1].version}")
    private String graphqlVersion;

    @Value("${apiml.service.apiInfo[2].version}")
    private String apiVersionRest2;

    @Value("${apiml.service.description}")
    private String apiDescription;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title(apiTitle)
                .description(apiDescription))
            .components(new Components().addSecuritySchemes("ESM token",
                new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("esmToken"))
            );
    }

    @Bean
    public GroupedOpenApi apiV1() {
        return GroupedOpenApi.builder()
            .group("apiv1")
            .pathsToMatch("/api/v1/**")
            .addOpenApiCustomizer(openApi -> openApi.setInfo(openApi.getInfo().version(apiVersionRest1)))
            .build();
    }

    @Bean
    public GroupedOpenApi apiV2() {
        return GroupedOpenApi.builder()
            .group("apiv2")
            .pathsToMatch("/api/v2/**")
            .addOpenApiCustomizer(openApi -> openApi.setInfo(openApi.getInfo().version(apiVersionRest2)))
            .build();
    }

    @Bean
    public GroupedOpenApi graphV1() {
        return GroupedOpenApi.builder()
            .group("graphv1")
            .pathsToMatch("/graphql/v1/**")
            .addOpenApiCustomizer(openApi -> openApi.setInfo(openApi.getInfo().version(graphqlVersion)))
            .build();
    }
}
