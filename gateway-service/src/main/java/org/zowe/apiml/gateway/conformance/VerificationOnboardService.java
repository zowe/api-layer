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
import io.swagger.parser.SwaggerParser;
import io.swagger.parser.util.SwaggerDeserializationResult;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Service class that offers methods for checking onboarding information and also checks availability metadata from
 * a provided serviceId.
 */
@Service
@RequiredArgsConstructor
public class VerificationOnboardService {

    private final DiscoveryClient discoveryClient;

    /**
     * Accepts serviceId and checks if the service is onboarded to the API Mediation Layer
     *
     * @param serviceId serviceId to check
     * @return true if the service is known by Eureka otherwise false.
     */
    public boolean checkOnboarding(String serviceId) {

        List<String> serviceLists = discoveryClient.getServices();

        return serviceLists.contains(serviceId);

    }


    /**
     * Accepts metadata and retrieves the Swagger url if it exists
     *
     * @param metadata to grab swagger from
     * @return SwaggerUrl when able, empty string otherwise
     */
    public String findSwaggerUrl(Map<String, String> metadata) {


        String swaggerKey = null;
        for (String key : metadata.keySet()) {
            if (key.contains("swaggerUrl")) {        // Find the correct key for swagger docs, can be both apiml.apiInfo.0.swaggerUrl or apiml.apiInfo.api-v1.swaggerUrl for example
                swaggerKey = key;
                break;
            }
        }
        if (swaggerKey == null) {
            return "";
        }
        String swaggerUrl = metadata.get(swaggerKey);
        if (swaggerUrl != null) {
            return swaggerUrl;
        }
        return "";
    }


    public String getSwagger(String swaggerUrl) {
        RestTemplate restTemplate = new RestTemplate();

        String response;
        response = restTemplate.getForEntity(swaggerUrl, String.class).getBody();

        return response;
    }


    public List<String> verifySwaggerDocumentation(String swaggerDoc, String serviceId) {
        return new ArrayList<>(validateConformanceToSwaggerSpecification(swaggerDoc, serviceId));
    }


    public List<String> validateConformanceToSwaggerSpecification(String swaggerDoc, String serviceId) {
        JsonNode root;
        try {
            root = new ObjectMapper().readTree(swaggerDoc);
        } catch (JsonProcessingException e) {
            return new ArrayList<>(Collections.singleton("Could not parse Swagger documentation"));
        }
        if (root.findValue("openapi") != null && root.findValue("openapi").asText().split("\\.")[0].equals("3")) {
            return validateOpenApi3(swaggerDoc, serviceId);
        } else if (root.findValue("swagger") != null && root.findValue("swagger").asText().equals("2.0")) {
            return validateOpenApi2(swaggerDoc, serviceId);
        } else
            return new ArrayList<>(Collections.singleton("Swagger documentation is not conformant to either OpenAPI V2 nor V3 - can't " +
                "find the version (that is cant find field named 'swagger' with value '2.0' or 'openapi' with version starting with '3' )"));
    }


    private List<String> validateOpenApi2(String swaggerAsString, String serviceId) {

        assert (serviceId != null);    // will be changed

        SwaggerDeserializationResult parseResult = new SwaggerParser().readWithInfo(swaggerAsString);
        if (parseResult.getMessages() != null) return parseResult.getMessages();

        return new ArrayList<>();
    }

    private List<String> validateOpenApi3(String swaggerAsString, String serviceId) {

        assert (serviceId != null); // will be changed

        SwaggerParseResult parseResult = new OpenAPIV3Parser().readContents(swaggerAsString);
        if (parseResult.getMessages() != null) return parseResult.getMessages();

        return new ArrayList<>();
    }


}

