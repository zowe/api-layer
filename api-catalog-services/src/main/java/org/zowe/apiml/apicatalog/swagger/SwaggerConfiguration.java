/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.apicatalog.swagger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.BasicAuth;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableSwagger2
public class SwaggerConfiguration {

    @Value("${apiml.service.apiDoc.title}")
    private String apiTitle;

    @Value("${apiml.service.apiDoc.version}")
    private String apiVersion;

    @Value("${apiml.service.apiDoc.description}")
    private String apiDescription;

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
            .select()
            .apis(RequestHandlerSelectors.basePackage("org.zowe.apiml.apicatalog.controllers.api"))
            .paths(
                PathSelectors.any()
            )
            .build()
            .securitySchemes(
                Arrays.asList(
                    new BasicAuth("LoginBasicAuth"),
                    new ApiKey("Bearer", "Authorization", "header"),
                    new ApiKey("CookieAuth", "apimlAuthenticationToken", "header")
                )
            )
            .apiInfo(
                new ApiInfo(
                    apiTitle,
                    apiDescription,
                    apiVersion,
                    null,
                    null,
                    null,
                    null,
                    Collections.emptyList()
                )
            );
    }
}
