/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnMissingBean(name = "modulithConfig")
@OpenAPIDefinition(
    security = @SecurityRequirement(name = "ClientCert"),
    info = @io.swagger.v3.oas.annotations.info.Info(title = "Caching service", description = """
        REST API for the Caching service, which is a module of the API Mediation Layer.
        Use this API to perform tasks such as store, read and update items under the client certificate as a key.
        """)
)
@SecurityScheme(
    type = SecuritySchemeType.MUTUALTLS,
    name = "ClientCert",
    description = "Client certificate X509"
)
public class SwaggerConfig {

    @Value("${apiml.service.title}")
    private String apiTitle;

    @Value("${apiml.service.apiInfo[0].version}")
    private String apiVersion;

    @Value("${apiml.service.description}")
    private String apiDescription;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
            .title(apiTitle)
            .description(apiDescription)
            .version(apiVersion)
        );
    }
}
