/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.conformance;

import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.springframework.http.HttpMethod;
import org.zowe.apiml.product.gateway.GatewayConfigProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OpenApiV3Validator extends AbstractSwaggerValidator {
    private final SwaggerParseResult swagger;


    public OpenApiV3Validator(String swaggerDoc, Map<String, String> metadata, GatewayConfigProperties gatewayConfigProperties, String serviceId) {
        super(metadata, gatewayConfigProperties, serviceId);
        swagger = new OpenAPIV3Parser().readContents(swaggerDoc);
    }

    public List<String> getMessages() {
        ArrayList<String> result = new ArrayList<>();
        for (String message : swagger.getMessages()) {
            result.add("Problem with swagger documentation: " + message);
        }
        return result;
    }


    public Set<Endpoint> getAllEndpoints() {
        HashSet<Endpoint> result = new HashSet<>();
        for (Map.Entry<String, PathItem> pathEntry : swagger.getOpenAPI().getPaths().entrySet()) {
            Set<HttpMethod> methods = getMethod(pathEntry.getValue());
            String url = generateUrlForEndpoint(pathEntry.getKey());
            HashMap<String, Set<String>> validResponses = getValidResponses(pathEntry.getValue());
            Endpoint currentEndpoint = new Endpoint(url, serviceId, methods, validResponses);
            result.add(currentEndpoint);
        }
        return result;
    }

    private HashMap<String, Set<String>> getValidResponses(PathItem value) {
        HashMap<String, Set<String>> result = new HashMap<>();
        for (HttpMethod httpMethod : getMethod(value)) {
            result.put(httpMethod.name(), value.readOperationsMap().get(convertSpringHttpToSwagger(httpMethod)).getResponses().keySet());
        }
        return result;
    }

    private String generateUrlForEndpoint(String endpoint) {

        String baseUrl = gatewayConfigProperties.getScheme() + "://" + gatewayConfigProperties.getHostname();

        String version = searchMetadata(metadata, "apiml", "routes", "gatewayUrl");
        String serviceUrl = searchMetadata(metadata, "apiml", "routes", "serviceUrl");

        String endOfUrl;
        if (endpoint.contains("/api/")) {
            endOfUrl = serviceUrl + endpoint;
        } else {
            endOfUrl = serviceUrl + version + endpoint;
        }
        return baseUrl + endOfUrl.replace("//", "/");
    }

    private Set<HttpMethod> getMethod(PathItem value) {
        Set<HttpMethod> result = new HashSet<>();
        if (value.getPost() != null) {
            result.add(HttpMethod.POST);
        }
        if (value.getGet() != null) {
            result.add(HttpMethod.GET);
        }
        if (value.getPatch() != null) {
            result.add(HttpMethod.PATCH);
        }
        if (value.getHead() != null) {
            result.add(HttpMethod.HEAD);
        }
        if (value.getOptions() != null) {
            result.add(HttpMethod.OPTIONS);
        }
        if (value.getDelete() != null) {
            result.add(HttpMethod.DELETE);
        }
        if (value.getPut() != null) {
            result.add(HttpMethod.PUT);
        }
        return result;
    }

    private PathItem.HttpMethod convertSpringHttpToSwagger(HttpMethod input) {
        switch (input.name()) {
            case "GET":
                return PathItem.HttpMethod.GET;
            case "HEAD":
                return PathItem.HttpMethod.HEAD;
            case "OPTIONS":
                return PathItem.HttpMethod.OPTIONS;
            case "PATCH":
                return PathItem.HttpMethod.PATCH;
            case "POST":
                return PathItem.HttpMethod.POST;
            case "DELETE":
                return PathItem.HttpMethod.DELETE;
            case "PUT":
                return PathItem.HttpMethod.PUT;
            default:
                return null;
        }
    }
}


