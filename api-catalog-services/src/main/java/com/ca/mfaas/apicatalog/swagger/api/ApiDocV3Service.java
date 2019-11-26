/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.swagger.api;

import com.ca.mfaas.apicatalog.services.cached.model.ApiDocInfo;
import com.ca.mfaas.apicatalog.swagger.ApiDocTransformationException;
import com.ca.mfaas.product.gateway.GatewayClient;
import com.ca.mfaas.product.gateway.GatewayConfigProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.util.Json;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.util.*;


@Slf4j
public class ApiDocV3Service extends AbstractApiDocService<OpenAPI, PathItem> {

    public ApiDocV3Service(GatewayClient gatewayClient) {
        super(gatewayClient);
    }

    public String transformApiDoc(String serviceId, ApiDocInfo apiDocInfo) throws IOException {
        OpenAPI openAPI = Json.mapper().readValue(apiDocInfo.getApiDocContent(), OpenAPI.class);
        boolean hidden = isHidden(openAPI.getTags());

        updatePaths(openAPI, serviceId, apiDocInfo, hidden);
        updateServerAndLink(openAPI, serviceId, hidden);
        updateExternalDoc(openAPI, apiDocInfo);

        try {
            return Json.mapper().writeValueAsString(openAPI);
        } catch (JsonProcessingException e) {
            log.debug("Could not convert Swagger to JSON", e);
            throw new ApiDocTransformationException("Could not convert Swagger to JSON");
        }
    }

    private void updateServerAndLink(OpenAPI openAPI, String serviceId, boolean hidden) {
        GatewayConfigProperties gatewayConfigProperties = gatewayClient.getGatewayConfigProperties();
        String swaggerLink = OpenApiUtil.getOpenApiLink(serviceId, gatewayConfigProperties);

        if (openAPI.getServers() != null) {
            openAPI.getServers()
                .forEach(server -> server.setUrl(
                    String.format("%s://%s/%s", gatewayConfigProperties.getScheme(), gatewayConfigProperties.getHostname(), server.getUrl())));
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
        String basePath = getBasePath(openAPI.getServers());

        if (openAPI.getPaths() != null && !openAPI.getPaths().isEmpty()) {
            openAPI.getPaths()
                .forEach((originalEndpoint, path)
                    -> preparePath(path, apiDocPath, apiDocInfo, basePath, originalEndpoint, serviceId));
        }

        Map<String, PathItem> updatedPaths;
        if (apiDocPath.getPrefixes().size() == 1) {
            updateServerUrl(openAPI,apiDocPath.getPrefixes().iterator().next() + OpenApiUtil.SEPARATOR + serviceId);
            updatedPaths = apiDocPath.getShortPaths();
        } else {
            updateServerUrl(openAPI,"");
            updatedPaths = apiDocPath.getLongPaths();
        }

        if (!hidden) {
            Paths paths = new Paths();
            updatedPaths.keySet().forEach(pathName -> paths.addPathItem(pathName, updatedPaths.get(pathName)));
            openAPI.setPaths(paths);
        }
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

    private String getBasePath(List<Server> servers) {
        String basePath = "";
        if (servers != null && !servers.isEmpty()) {
            Server server = servers.get(0);
            try {
                URI uri = new URI(server.getUrl());
                basePath = uri.getPath();
            } catch (Exception e) {
                log.debug("serverUrl is not parsable");
            }
        }
        return basePath;
    }

    private void updateServerUrl(OpenAPI openAPI, String basePath) {
        if (openAPI.getServers() != null && !openAPI.getServers().isEmpty()) {
            //should we support several servers?
            Server server = openAPI.getServers().get(0);
            server.setUrl(basePath);
        } else {
            openAPI.addServersItem(new Server().url(basePath));
        }
    }

    private boolean isHidden(List<Tag> tags) {
        return tags.stream().anyMatch(tag -> tag.getName().equals(HIDDEN_TAG));
    }
}
