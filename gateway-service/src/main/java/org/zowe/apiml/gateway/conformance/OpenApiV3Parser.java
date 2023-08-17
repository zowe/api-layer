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


import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.springframework.http.HttpMethod;
import org.zowe.apiml.product.gateway.GatewayConfigProperties;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class OpenApiV3Parser extends AbstractSwaggerParser {
    private final SwaggerParseResult swagger;


    public OpenApiV3Parser(String swaggerDoc, Map<String, String> metadata, GatewayConfigProperties gatewayConfigProperties, String serviceId) {
        super(metadata, gatewayConfigProperties, serviceId);
        swagger = new OpenAPIV3Parser().readContents(swaggerDoc);
    }

    public List<String> getMessages() {
        return swagger.getMessages();
    }


    public Set<Endpoint> getAllEndpoints() {
        HashSet<Endpoint> result = new HashSet<>();
        for (Map.Entry<String, PathItem> entry : swagger.getOpenAPI().getPaths().entrySet()) {
            HttpMethod method = getMethod(entry.getValue());
            String url = generateUrlForEndpoint(entry.getKey());
            Set<String> validResponses = getValidResponses(entry.getValue());
            Endpoint currentEndpoint = new Endpoint(url, serviceId, method, validResponses);
            result.add(currentEndpoint);
        }
        return result;
    }

    private Set<String> getValidResponses(PathItem value) {
        return value.readOperationsMap().get(convertSpringHttpToswagger(getMethod(value))).getResponses().keySet();
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

    private HttpMethod getMethod(PathItem value) {
        if (value.getGet() != null) {
            return HttpMethod.GET;
        } else if (value.getHead() != null) {
            return HttpMethod.HEAD;
        } else if (value.getOptions() != null) {
            return HttpMethod.OPTIONS;
        } else if (value.getPatch() != null) {
            return HttpMethod.PATCH;
        } else if (value.getPost() != null) {
            return HttpMethod.POST;
        } else if (value.getDelete() != null) {
            return HttpMethod.DELETE;
        } else if (value.getPut() != null) {
            return HttpMethod.PUT;
        }
        return null;
    }

    private PathItem.HttpMethod convertSpringHttpToswagger(HttpMethod input) {
        if (input == HttpMethod.GET) {
            return PathItem.HttpMethod.GET;
        } else if (input == HttpMethod.HEAD) {
            return PathItem.HttpMethod.HEAD;
        } else if (input == HttpMethod.OPTIONS) {
            return PathItem.HttpMethod.OPTIONS;
        } else if (input == HttpMethod.PATCH) {
            return PathItem.HttpMethod.PATCH;
        } else if (input == HttpMethod.POST) {
            return PathItem.HttpMethod.POST;
        } else if (input == HttpMethod.DELETE) {
            return PathItem.HttpMethod.DELETE;
        } else if (input == HttpMethod.PUT) {
            return PathItem.HttpMethod.PUT;
        }
        return null;
    }
}


