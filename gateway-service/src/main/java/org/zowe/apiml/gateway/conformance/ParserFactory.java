/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.conformance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.zowe.apiml.product.gateway.GatewayConfigProperties;

import java.util.Map;


public class ParserFactory {
    private ParserFactory() {
    }


    public static AbstractSwaggerParser parseSwagger(String swaggerDoc, Map<String, String> metadata, GatewayConfigProperties gatewayConfigProperties, String serviceId) {
        JsonNode root;
        try {
            root = new ObjectMapper().readTree(swaggerDoc);
        } catch (JsonProcessingException e) {
            throw new SwaggerParsingException("Could not parse Swagger documentation");
        }
        if (root.findValue("openapi") != null && root.findValue("openapi").asText().split("\\.")[0].equals("3")) {
            return new OpenApiV3Parser(swaggerDoc, metadata, gatewayConfigProperties, serviceId);
        } else if (root.findValue("swagger") != null && root.findValue("swagger").asText().equals("2.0")) {
            return new OpenApiV2Parser(swaggerDoc, metadata, gatewayConfigProperties, serviceId);
        } else
            throw new SwaggerParsingException("Swagger documentation is not conformant to either OpenAPI V2 nor V3 - can't " +
                "find the version (that is cant find field named 'swagger' with value '2.0' or 'openapi' with version starting with '3' )");
    }

}
