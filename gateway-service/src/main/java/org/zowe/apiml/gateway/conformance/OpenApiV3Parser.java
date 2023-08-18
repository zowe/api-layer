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

import java.util.*;


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
            Set<HttpMethod> methods = getMethod(entry.getValue());
            String url = generateUrlForEndpoint(entry.getKey());
            HashMap<String, Set<String>> validResponses = getValidResponses(entry.getValue());
            Endpoint currentEndpoint = new Endpoint(url, serviceId, methods, validResponses);
            result.add(currentEndpoint);
        }
        return result;
    }

    private HashMap<String, Set<String>> getValidResponses(PathItem value) {
        HashMap<String, Set<String>> result = new HashMap<>();
        for (HttpMethod i : getMethod(value)) {
            result.put(i.name(), value.readOperationsMap().get(convertSpringHttpToswagger(i)).getResponses().keySet());
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
        if (value.getGet() != null) {
            result.add(HttpMethod.GET);
        }
        if (value.getHead() != null) {
            result.add(HttpMethod.HEAD);
        }
        if (value.getOptions() != null) {
            result.add(HttpMethod.OPTIONS);
        }
        if (value.getPatch() != null) {
            result.add(HttpMethod.PATCH);
        }
        if (value.getPost() != null) {
            result.add(HttpMethod.POST);
        }
        if (value.getDelete() != null) {
            result.add(HttpMethod.DELETE);
        }
        if (value.getPut() != null) {
            result.add(HttpMethod.PUT);
        }
        return result;
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


