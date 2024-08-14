/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.swagger.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import jakarta.validation.UnexpectedTypeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.zowe.apiml.apicatalog.services.cached.model.ApiDocInfo;
import org.zowe.apiml.apicatalog.swagger.ApiDocTransformationException;
import org.zowe.apiml.apicatalog.swagger.SecuritySchemeSerializer;
import org.zowe.apiml.config.ApiInfo;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.product.instance.ServiceAddress;
import org.zowe.apiml.product.routing.RoutedService;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;


@Slf4j
public class ApiDocV3Service extends AbstractApiDocService<OpenAPI, PathItem> {
    @Value("${gateway.scheme.external:https}")
    private String scheme;

    private final ObjectMapper mapper;

    public ApiDocV3Service(GatewayClient gatewayClient) {
        super(gatewayClient);
        mapper = initializeObjectMapper();
    }

    public String transformApiDoc(String serviceId, ApiDocInfo apiDocInfo) {
        SwaggerParseResult parseResult = new OpenAPIV3Parser().readContents(apiDocInfo.getApiDocContent());
        OpenAPI openAPI = parseResult.getOpenAPI();

        if (openAPI == null) {
            log.debug("Could not convert response body to an OpenAPI object for service {}. {}", serviceId, parseResult.getMessages());

            if (parseResult.getMessages() == null) {
                throw new UnexpectedTypeException("Response is not an OpenAPI type object.");
            } else {
                throw new UnexpectedTypeException(parseResult.getMessages().toString());
            }
        }

        boolean hidden = isHidden(openAPI.getTags());

        if (!isDefinedOnlyBypassRoutes(apiDocInfo)) {
            updatePaths(openAPI, serviceId, apiDocInfo, hidden);
            updateServerAndLink(openAPI, serviceId, apiDocInfo.getApiInfo(), hidden);
        }
        updateExternalDoc(openAPI, apiDocInfo);

        try {
            return mapper.writeValueAsString(openAPI);
        } catch (JsonProcessingException e) {
            log.debug("Could not convert OpenAPI to JSON", e);
            throw new ApiDocTransformationException("Could not convert Swagger to JSON");
        }
    }

    private void updateServerAndLink(OpenAPI openAPI, String serviceId, ApiInfo apiInfo, boolean hidden) {
        ServiceAddress gatewayConfigProperties = gatewayClient.getGatewayConfigProperties();
        String swaggerLink = OpenApiUtil.getOpenApiLink(serviceId, apiInfo, gatewayConfigProperties, scheme);

        if (openAPI.getServers() != null) {
            openAPI.getServers()
                .forEach(server -> server.setUrl(
                    String.format("%s://%s/%s", scheme, getHostname(serviceId), server.getUrl())));
        }
        if (!hidden) {
            openAPI.getInfo().setDescription(openAPI.getInfo().getDescription() + swaggerLink);
        }
    }

    /**
     * Updates Servers and Paths in OpenAPI
     *
     * @param openAPI    the API doc
     * @param serviceId  the unique service id
     * @param apiDocInfo the service information
     * @param hidden     do not set Paths for automatically generated API doc
     */
    protected void updatePaths(OpenAPI openAPI, String serviceId, ApiDocInfo apiDocInfo, boolean hidden) {
        ApiDocPath<PathItem> apiDocPath = new ApiDocPath<>();
        Server server = getBestMatchingServer(openAPI.getServers(), apiDocInfo);
        String basePath = server != null ? getBasePath(server.getUrl()) : "";

        if (openAPI.getPaths() != null && !openAPI.getPaths().isEmpty()) {
            openAPI.getPaths()
                .forEach((originalEndpoint, path)
                    -> preparePath(path, apiDocPath, apiDocInfo, basePath, originalEndpoint, serviceId));
        }

        Map<String, PathItem> updatedPaths;
        if (apiDocPath.getPrefixes().size() == 1) {
            updateServerUrl(openAPI, server, OpenApiUtil.getBasePath(serviceId, apiDocPath));
            updatedPaths = apiDocPath.getShortPaths();
        } else {
            updateServerUrl(openAPI, server, "/");
            updatedPaths = apiDocPath.getLongPaths();
        }

        if (!hidden) {
            Paths paths = new Paths();
            updatedPaths.keySet().forEach(pathName -> paths.addPathItem(pathName, updatedPaths.get(pathName)));
            openAPI.setPaths(paths);
        }
    }

    private Server getBestMatchingServer(List<Server> servers, ApiDocInfo apiDocInfo) {
        if (servers != null && !servers.isEmpty()) {
            for (Server server : servers) {
                String basePath = getBasePath(server.getUrl());
                RoutedService route = getRoutedServiceByApiInfo(apiDocInfo, basePath);
                if (route != null) {
                    return server;
                }
            }
            return servers.get(0);
        }
        return null;
    }

    private String getBasePath(String serverUrl) {
        String basePath = "";
        try {
            URI uri = new URI(serverUrl);
            basePath = uri.getPath();
        } catch (Exception e) {
            log.debug("serverUrl is not parse-able");
        }
        return basePath;
    }

    /**
     * Updates External documentation in OpenAPI
     *
     * @param openAPI    the API doc
     * @param apiDocInfo the service information
     */
    protected void updateExternalDoc(OpenAPI openAPI, ApiDocInfo apiDocInfo) {
        if (apiDocInfo.getApiInfo() == null)
            return;

        String externalDocUrl = apiDocInfo.getApiInfo().getDocumentationUrl();

        if (externalDocUrl != null) {
            ExternalDocumentation externalDoc = new ExternalDocumentation();
            externalDoc.setDescription(EXTERNAL_DOCUMENTATION);
            externalDoc.setUrl(externalDocUrl);
            openAPI.setExternalDocs(externalDoc);
        }
    }

    private void updateServerUrl(OpenAPI openAPI, Server server, String basePath) {
        if (server != null) {
            server.setUrl(basePath.startsWith("/") ? basePath.substring(1) : basePath); // server expects no / at start of url
            openAPI.setServers(Collections.singletonList(server));
        } else {
            openAPI.addServersItem(new Server().url(basePath));
        }
    }

    private boolean isHidden(List<Tag> tags) {
        return tags != null && tags.stream().anyMatch(tag -> tag.getName().equals(HIDDEN_TAG));
    }

    private ObjectMapper initializeObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(SecurityScheme.class, new SecuritySchemeSerializer());

        objectMapper.registerModule(simpleModule);
        objectMapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);

        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }
}
