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

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.parser.OpenAPIV3Parser;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zowe.apiml.security.common.error.ServiceNotAccessibleException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Optional;

import static org.zowe.apiml.product.constants.CoreService.ZAAS;

@Slf4j
@Configuration
@RequiredArgsConstructor
@OpenAPIDefinition(
    security = @SecurityRequirement(name = "LoginBasicAuth"),
    info = @Info(title = "API Gateway", description = "REST API for the API Gateway, which is a component of the API\nMediation Layer. Use this API to perform tasks such as logging in with the\nmainframe credentials and checking authorization to mainframe resources.")
)
@SecurityScheme(
    name = "LoginBasicAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "basic"
)
@SecurityScheme(
    name = "Bearer",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
@SecurityScheme(
    name = "CookieAuth",
    type = SecuritySchemeType.APIKEY,
    in = SecuritySchemeIn.COOKIE,
    paramName = "apimlAuthenticationToken"
)
@SecurityScheme(
    type = SecuritySchemeType.HTTP,
    name = "ClientCert",
    description = "Client certificate X509"
)
public class SwaggerConfig {

    @Value("${server.attls.enabled:false}")
    private boolean isAttlsEnabled;

    private final EurekaClient eurekaClient;

    private URI zaasUri;

    @PostConstruct
    void initEurekaListener() {
        eurekaClient.registerEventListener(event -> {
            Optional.ofNullable(eurekaClient.getApplication(ZAAS.getServiceId()))
                .map(apps -> apps.getInstances())
                .filter(apps -> !apps.isEmpty())
                .map(apps -> apps.get(0))
                .ifPresent(app -> {
                    try {
                        zaasUri = new URIBuilder()
                            .setScheme(isAttlsEnabled || app.isPortEnabled(InstanceInfo.PortType.SECURE) ? "https" : "http")
                            .setHost(app.getHostName())
                            .setPort(app.isPortEnabled(InstanceInfo.PortType.SECURE) ? app.getSecurePort() : app.getPort())
                            .setPath("/v3/api-docs/auth")
                            .build();
                    } catch (URISyntaxException e) {
                        log.error("Cannot construct Swagger URL on ZAAS", e);
                    }
                });
        });
    }

    private String updateUrlFromZaas(String zaasUrl) {
        return zaasUrl.replaceFirst("/zaas/", "/gateway/");
    }

    void updatePaths(OpenAPI openApi, String pathToMatch) {
        String basePath = pathToMatch.replaceAll("[*]", "");

        if (openApi.getServers() == null) {
            openApi.setServers(new LinkedList<>());
        }
        openApi.getServers().forEach(server -> {
            String url = server.getUrl();
            if (!url.endsWith("/")) {
                url += '/';
            }
            url += basePath.substring(1);
            server.setUrl(url);
        });

        if (openApi.getPaths() == null) {
            openApi.setPaths(new Paths());
        }
        Paths paths = new Paths();
        openApi.getPaths().forEach((url, schema) ->
            paths.addPathItem(url.replace(basePath, basePath.endsWith("/") ? "/" : ""), schema)
        );
        openApi.setPaths(paths);
    }

    @Bean
    public OpenApiCustomizer servletEndpoints(@Value("${springdoc.pathsToMatch:/}") String pathToMatch) {
        return (openApi) -> {
            if (zaasUri == null) {
                throw new ServiceNotAccessibleException("ZAAS is not available yet");
            }

            OpenAPI servletEndpoints = new OpenAPIV3Parser().read(zaasUri.toString());

            for (var entry : servletEndpoints.getPaths().entrySet()) {
                if (openApi.getPaths() == null) {
                    openApi.setPaths(new Paths());
                }
                openApi.getPaths().addPathItem(updateUrlFromZaas(entry.getKey()), entry.getValue());
            }

            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            if (openApi.getComponents().getSchemas() == null) {
                openApi.getComponents().setSchemas(new HashMap<>());
            }
            openApi.getComponents().getSchemas().putAll(
                servletEndpoints.getComponents().getSchemas()
            );

            if (openApi.getTags() == null) {
                openApi.setTags(new ArrayList<>());
            }
            openApi.getTags().addAll(servletEndpoints.getTags());

            updatePaths(openApi, pathToMatch);
        };
    }

}
