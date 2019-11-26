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
import io.swagger.models.ExternalDocs;
import io.swagger.models.Path;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;
import io.swagger.util.Json;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;

@Slf4j
public class ApiDocV2Service extends AbstractApiDocService<Swagger, Path> {

    public ApiDocV2Service(GatewayClient gatewayClient) {
        super(gatewayClient);
    }

    public String transformApiDoc(String serviceId, ApiDocInfo apiDocInfo) throws IOException {
        Swagger swagger = Json.mapper().readValue(apiDocInfo.getApiDocContent(), Swagger.class);
        boolean hidden = swagger.getTag(HIDDEN_TAG) != null;

        updateSchemeHostAndLink(swagger, serviceId, hidden);
        updatePaths(swagger, serviceId, apiDocInfo, hidden);
        updateExternalDoc(swagger, apiDocInfo);

        try {
            return Json.mapper().writeValueAsString(swagger);
        } catch (JsonProcessingException e) {
            log.debug("Could not convert Swagger to JSON", e);
            throw new ApiDocTransformationException("Could not convert Swagger to JSON");
        }
    }

    /**
     * Updates scheme and hostname, and adds API doc link to Swagger
     *
     * @param swagger   the API doc
     * @param serviceId the unique service id
     * @param hidden    do not add link for automatically generated API doc
     */
    private void updateSchemeHostAndLink(Swagger swagger, String serviceId, boolean hidden) {
        GatewayConfigProperties gatewayConfigProperties = gatewayClient.getGatewayConfigProperties();
        String swaggerLink = OpenApiUtil.getOpenApiLink(serviceId, gatewayConfigProperties);

        swagger.setSchemes(Collections.singletonList(Scheme.forValue(gatewayConfigProperties.getScheme())));
        swagger.setHost(gatewayConfigProperties.getHostname());
        if (!hidden) {
            swagger.getInfo().setDescription(swagger.getInfo().getDescription() + swaggerLink);
        }
    }

    /**
     * Updates BasePath and Paths in Swagger
     *
     * @param swagger    the API doc
     * @param serviceId  the unique service id
     * @param apiDocInfo the service information
     * @param hidden     do not set Paths for automatically generated API doc
     */
    protected void updatePaths(Swagger swagger, String serviceId, ApiDocInfo apiDocInfo, boolean hidden) {
        ApiDocPath<Path> apiDocPath = new ApiDocPath<>();
        String basePath = swagger.getBasePath();

        if (swagger.getPaths() != null && !swagger.getPaths().isEmpty()) {
            swagger.getPaths()
                .forEach((originalEndpoint, path)
                    -> preparePath(path, apiDocPath, apiDocInfo, basePath, originalEndpoint, serviceId));
        }

        Map<String, Path> updatedPaths;
        if (apiDocPath.getPrefixes().size() == 1) {
            swagger.setBasePath(OpenApiUtil.SEPARATOR + apiDocPath.getPrefixes().iterator().next() + OpenApiUtil.SEPARATOR + serviceId);
            updatedPaths = apiDocPath.getShortPaths();
        } else {
            swagger.setBasePath("");
            updatedPaths = apiDocPath.getLongPaths();
        }

        if (!hidden) {
            swagger.setPaths(updatedPaths);
        }
    }

    /**
     * Updates External documentation in Swagger
     *
     * @param swagger    the API doc
     * @param apiDocInfo the service information
     */
    protected void updateExternalDoc(Swagger swagger, ApiDocInfo apiDocInfo) {
        if (apiDocInfo.getApiInfo() == null)
            return;

        String externalDoc = apiDocInfo.getApiInfo().getDocumentationUrl();

        if (externalDoc != null) {
            swagger.setExternalDocs(new ExternalDocs(EXTERNAL_DOCUMENTATION, externalDoc));
        }
    }
}
