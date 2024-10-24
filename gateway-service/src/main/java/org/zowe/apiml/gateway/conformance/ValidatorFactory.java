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
import lombok.experimental.UtilityClass;
import org.zowe.apiml.product.instance.ServiceAddress;

import java.util.Map;

@UtilityClass
public class ValidatorFactory {
    private static final String NON_CONFORMANT_KEY = "org.zowe.apiml.gateway.verifier.nonConformant";
    public AbstractSwaggerValidator parseSwagger(String swaggerDoc, Map<String, String> metadata, ServiceAddress gatewayServiceAddress, String serviceId) {
        JsonNode root;
        try {
            root = new ObjectMapper().readTree(swaggerDoc);
        } catch (JsonProcessingException e) {
            throw new ValidationException("Could not parse Swagger documentation", NON_CONFORMANT_KEY);
        }
        if (root.findValue("openapi") != null && root.findValue("openapi").asText().split("\\.")[0].equals("3")) {
            return new OpenApiV3Validator(swaggerDoc, metadata, gatewayServiceAddress, serviceId);
        }
        if (root.findValue("swagger") != null && root.findValue("swagger").asText().equals("2.0")) {
            return new OpenApiV2Validator(swaggerDoc, metadata, gatewayServiceAddress, serviceId);
        }
        throw new ValidationException("Swagger documentation is not conformant to either OpenAPI V2 nor V3 - cannot " +
            "find the version (that is cannot find field named 'swagger' with value '2.0' or 'openapi' with version starting with '3' )", NON_CONFORMANT_KEY);
    }
}
