/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.swagger;

import com.ca.mfaas.apicatalog.services.cached.model.ApiDocInfo;
import com.ca.mfaas.apicatalog.swagger.api.ApiDocV2Service;
import com.ca.mfaas.apicatalog.swagger.api.ApiDocV3Service;
import com.ca.mfaas.product.gateway.GatewayClient;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.validation.UnexpectedTypeException;
import java.io.IOException;

/**
 * Transforms API documentation to documentation relative to Gateway, not the service instance
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransformApiDocService {
    private final GatewayClient gatewayClient;

    /**
     * Does transformation API documentation
     *
     * @param serviceId  the unique service id
     * @param apiDocInfo the API doc and additional information about transformation
     * @return the transformed API documentation relative to Gateway
     * @throws ApiDocTransformationException if could not convert Swagger/OpenAPI to JSON
     * @throws UnexpectedTypeException       if response is not a Swagger/OpenAPI type object
     */
    public String transformApiDoc(String serviceId, ApiDocInfo apiDocInfo) {
        //maybe null check of apidocinfo
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            ObjectNode objectNode = mapper.readValue(apiDocInfo.getApiDocContent(), ObjectNode.class);
            JsonNode openApiNode = objectNode.get("openapi");
            if (openApiNode != null) {
                log.debug("Open3 is working... {}", serviceId);
                ApiDocV3Service apiDocV3Service = new ApiDocV3Service(gatewayClient);
                return apiDocV3Service.transformApiDoc(serviceId, apiDocInfo);
                //do something with openapi class
            } else {
                JsonNode swaggerNode = objectNode.get("swagger");
                if (swaggerNode != null) {
                    log.debug("Open2 is working... {}", serviceId);
                    ApiDocV2Service apiDocV2Service = new ApiDocV2Service(gatewayClient);
                    return apiDocV2Service.transformApiDoc(serviceId, apiDocInfo);
                }
            }
        } catch (IOException e) {
            log.debug("Could not convert response body to a Swagger/OpenAPI object.", e);
            throw new UnexpectedTypeException("Response is not a Swagger or OpenAPI type object.");
        }
        return null;
    }
}
