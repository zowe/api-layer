/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.eurekaservice;

import io.swagger.jaxrs.config.SwaggerContextService;
import io.swagger.models.Info;
import io.swagger.models.Swagger;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ResourceBundle;

public class Bootstrap extends HttpServlet {

    private static URI apiDocEndpoint;
    private static ResourceBundle eurekaProperties = ResourceBundle.getBundle("eureka-client");
    private static final Logger log = LoggerFactory.getLogger(Bootstrap.class);

    @Override
    public void init(ServletConfig config) {
        Info info = new Info()
                .title(eurekaProperties.getString("eureka.metadata.mfaas.api-info.apiVersionProperties.v1.title"))
                .description(eurekaProperties.getString("eureka.metadata.mfaas.api-info.apiVersionProperties.v1.description"))
                .version(eurekaProperties.getString("eureka.metadata.mfaas.api-info.apiVersionProperties.v1.version"));

        Swagger swagger = new Swagger().info(info);
        new SwaggerContextService().withServletConfig(config).updateSwagger(swagger);
        constructApiDocLocation();
    }

    private static void constructApiDocLocation() {
        String eurekaHostName = eurekaProperties.getString("eureka.service.hostname");
        int eurekaPort = Integer.parseInt(eurekaProperties.getString("eureka.port"));
        String contextPath = eurekaProperties.getString("server.contextPath");
        Boolean securePortEnabled = Boolean.valueOf(eurekaProperties.getString("eureka.securePortEnabled"));
        try {
            apiDocEndpoint = new URIBuilder()
                    .setScheme(securePortEnabled ? "https" : "http")
                    .setHost(eurekaHostName)
                    .setPort(eurekaPort)
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
