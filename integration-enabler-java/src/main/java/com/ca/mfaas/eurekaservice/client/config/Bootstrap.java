/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.eurekaservice.client.config;

import io.swagger.jaxrs.config.SwaggerContextService;
import io.swagger.models.Info;
import io.swagger.models.Swagger;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;

public class Bootstrap extends HttpServlet {

    private static URI apiDocEndpoint;
    private static final Logger log = LoggerFactory.getLogger(Bootstrap.class);

    @Override
    public void init(ServletConfig config) {
        ApiMediationServiceConfig apimlConfig = new ApiMediationServiceConfig();
        Info info = new Info()
            .title(apimlConfig.getApiInfo().getTitle())
            .description(apimlConfig.getApiInfo().getDescription())
            .version(apimlConfig.getApiInfo().getVersion());

        Swagger swagger = new Swagger().info(info);
        new SwaggerContextService().withServletConfig(config).updateSwagger(swagger);
        constructApiDocLocation(apimlConfig);
    }

    private static void constructApiDocLocation(ApiMediationServiceConfig apimlConfig) {
        String hostname;
        String contextPath = apimlConfig.getContextPath();
        int port;
        URL baseUrl;
        try {
            baseUrl = new URL(apimlConfig.getBaseUrl());
            hostname = baseUrl.getHost();
            port = baseUrl.getPort();
        } catch (MalformedURLException e) {
            throw new RuntimeException(String.format("baseUrl: [%s] is not valid URL", apimlConfig.getBaseUrl()), e);
        }

        try {
            apiDocEndpoint = new URIBuilder()
                .setScheme("https")
                .setHost(hostname)
                .setPort(port)
                .setPath((contextPath.isEmpty() ? "" : contextPath + "/") + "swagger.json").build();
        } catch (URISyntaxException e) {
            log.error("Could not construct API Doc endpoint. API Doc cannot be accessed via /api-doc endpoint.\n"
                + e.getMessage(), e);
        }
    }

    public static URI getApiDocEndpoint() {
        return apiDocEndpoint;
    }
}
