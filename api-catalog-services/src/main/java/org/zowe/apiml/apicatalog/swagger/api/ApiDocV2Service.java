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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.models.ExternalDocs;
import io.swagger.models.Path;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import io.swagger.util.Json;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.zowe.apiml.apicatalog.services.cached.model.ApiDocInfo;
import org.zowe.apiml.apicatalog.swagger.ApiDocTransformationException;
import org.zowe.apiml.config.ApiInfo;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.product.instance.ServiceAddress;

import jakarta.validation.UnexpectedTypeException;
import java.util.Collections;
import java.util.Map;

@Slf4j
public class ApiDocV2Service extends AbstractApiDocService<Swagger, Path> {

    @Value("${gateway.scheme.external:https}")
    private String scheme;

    public ApiDocV2Service(GatewayClient gatewayClient) {
        super(gatewayClient);
    }

    public String transformApiDoc(String serviceId, ApiDocInfo apiDocInfo) {

        Swagger swagger = new SwaggerParser().readWithInfo(apiDocInfo.getApiDocContent()).getSwagger();
        if (swagger == null) {
            log.debug("Could not convert response body to a Swagger object.");
            throw new UnexpectedTypeException("Response is not a Swagger type object.");
        }

        boolean hidden = swagger.getTag(HIDDEN_TAG) != null;

        if (!isDefinedOnlyBypassRoutes(apiDocInfo)) {
            updateSchemeHostAndLink(swagger, serviceId, apiDocInfo.getApiInfo(), hidden);
            updatePaths(swagger, serviceId, apiDocInfo, hidden);
        }
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
    private void updateSchemeHostAndLink(Swagger swagger, String serviceId, ApiInfo apiInfo, boolean hidden) {
        ServiceAddress gatewayConfigProperties = gatewayClient.getGatewayConfigProperties();
        String swaggerLink = OpenApiUtil.getOpenApiLink(serviceId, apiInfo, gatewayConfigProperties, scheme);
        log.debug("Updating host for service with id: " + serviceId + " to: " + getHostname(serviceId));
        swagger.setSchemes(Collections.singletonList(Scheme.forValue(scheme)));
        swagger.setHost(getHostname(serviceId));
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
            swagger.setBasePath(OpenApiUtil.getBasePath(serviceId, apiDocPath));
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
