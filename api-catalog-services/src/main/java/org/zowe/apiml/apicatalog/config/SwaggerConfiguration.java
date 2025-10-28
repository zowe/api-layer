/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnMissingBean(name = "modulithConfig")
@OpenAPIDefinition(
    security = {
        @SecurityRequirement(name = "LoginBasicAuth"),
        @SecurityRequirement(name = "Bearer"),
        @SecurityRequirement(name = "CookieAuth")
    },
    info = @io.swagger.v3.oas.annotations.info.Info(title = "API Catalog", description = """
        REST API for the API Catalog, which is a component of the API Mediation Layer.
        Use this API to perform tasks such as retrieve the catalog containers with the Open API documentation.
        """)
)
@io.swagger.v3.oas.annotations.security.SecurityScheme(
    name = "LoginBasicAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "basic"
)
@io.swagger.v3.oas.annotations.security.SecurityScheme(
    name = "Bearer",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
@io.swagger.v3.oas.annotations.security.SecurityScheme(
    name = "CookieAuth",
    type = SecuritySchemeType.APIKEY,
    in = SecuritySchemeIn.COOKIE,
    paramName = "apimlAuthenticationToken"
)
public class SwaggerConfiguration {

    @Value("${apiml.service.apiDoc.title}")
    private String apiTitle;

    @Value("${apiml.service.apiDoc.version}")
    private String apiVersion;

    @Value("${apiml.service.apiDoc.description}")
    private String apiDescription;

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
            .info(new Info()
                .title(apiTitle)
                .description(apiDescription)
                .version(apiVersion)
            )
            .components(new Components()
                .addSecuritySchemes("BasicAuthorization", new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic"))
                .addSecuritySchemes("CookieAuth", new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("apimlAuthenticationToken"))
            );
    }

}
