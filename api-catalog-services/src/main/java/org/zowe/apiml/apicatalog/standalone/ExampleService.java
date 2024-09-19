/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.standalone;

import com.fasterxml.jackson.databind.module.SimpleModule;
import io.swagger.oas.inflector.examples.ExampleBuilder;
import io.swagger.oas.inflector.processors.JsonNodeExampleSerializer;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * This class is responsible for generating the examples for API Doc and can simulate the response for any endpoint. It
 * is helpful for the standalone mode that mocks the loaded schemas.
 *
 * This class tries to generate a response for each endpoint in the API documentation. It focuses on the successful
 * response with media-type JSON. If it is not possible to generate such content, it uses the default one (an empty
 * JSON object). Examples are mapping by ANT matcher and it is possible to respond by this class (see method
 * {@link ExampleService#replyExample(HttpServletResponse, String, String)}).
 */
@Slf4j
@Service
@ConditionalOnProperty(value = "apiml.catalog.standalone.enabled", havingValue = "true")
public class ExampleService {

    private static final Example DEFAULT_EXAMPLE = Example.builder()
        .responseCode(200).content("{}").mediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
        .build();

    /**
     * Collection of know responses. The key of the map represents method type. The list itself is all known
     * examples of responses for the specific type. It is not possible to use map or a different structure, because
     * the mapping uses Ant Patterns. Therefore, it is necessary to test mapping of each URL path.
     */
    private static final Map<String, List<Example>> examples = Collections.synchronizedMap(new HashMap<>());

    {
        // init JSON library to generate example
        SimpleModule simpleModule = new SimpleModule().addSerializer(new JsonNodeExampleSerializer());
        Json.mapper().registerModule(simpleModule);
    }

    /**
     * Construct class {@link ExampleService.Example} and put in {@link #examples} for the future using.
     * @param method method of the request
     * @param path ANT pattern to match URL path of request
     * @param responseCode response code of example
     * @param mediaType the media type of the example
     * @param content response's content of example
     */
    static void addExample(String method, String path, int responseCode, String mediaType, String content) {
        Example example = Example.builder()
            .method(method).path(path)
            .responseCode(responseCode).mediaType(mediaType).content(content)
            .build();

        List<Example> byMethod = examples.computeIfAbsent(method, k -> Collections.synchronizedList(new ArrayList<>()));
        byMethod.add(example);

        log.debug("Generated response example for request {}, {}, {}, {}:\n{}", method, path, responseCode, mediaType, content);
    }

    /**
     * Method returns first entry with response code 2xx if exists. Otherwise it return null (in case the operation
     * does not contain any record) or the first entry.
     * @param operation operation to analyze
     * @return entry with successful response if exists, otherwise first entry or null if no one exists
     */
    static Map.Entry<String, ApiResponse> getFirstApiResponses(Operation operation) {
        Map.Entry<String, ApiResponse> first = null;
        for (Map.Entry<String, ApiResponse> responseEntry : operation.getResponses().entrySet()) {
            if (responseEntry.getKey().startsWith("2")) return responseEntry;
            if (first == null) first = responseEntry;
        }
        return first;
    }

    /**
     * Method to translate response code in API doc to number.
     * @param input the representation of response code in API doc
     * @return translated response code or 0 if the input is unclear
     */
    static int getResponseCode(String input) {
        if (StringUtils.equals("default", input)) {
            return 200;
        }
        if (StringUtils.isNumeric(input)) {
            return Integer.parseInt(input);
        }
        return 0;
    }

    private void generateExample(String serviceId, OpenAPI swagger, String method, Operation operation, String path) {
        Map.Entry<String, ApiResponse> responseEntry = getFirstApiResponses(operation);
        if (responseEntry == null) return;
        Content content = responseEntry.getValue().getContent();
        if (content == null) return;

        for (Map.Entry<String, MediaType> mediaEntry : content.entrySet()) {
            if (!mediaEntry.getKey().contains("json")) continue;

            Object example = mediaEntry.getValue().getExample();
            if (example == null) {
                example = ExampleBuilder.fromSchema(mediaEntry.getValue().getSchema(), swagger.getComponents().getSchemas());
            }

            String uri = String.format("/%s%s", serviceId, path);
            String exampleJson = Json.pretty(example);

            addExample(method, uri, getResponseCode(responseEntry.getKey()), mediaEntry.getKey(), exampleJson);
        }
    }

    /**
     * It load, parse and analyze input file with API doc. The response is loaded examples in {@link #examples}.
     * @param serviceId ID of service of API doc file
     * @param apiDoc path of file with API doc to parse
     */
    public void generateExamples(String serviceId, String apiDoc) {
        try {
            SwaggerParseResult parseResult = new OpenAPIParser().readContents(apiDoc, null, null);
            OpenAPI swagger = parseResult != null ? parseResult.getOpenAPI() : null;
            Paths paths = swagger != null ? swagger.getPaths() : null ;
            if (paths != null) {
            for (Map.Entry<String, PathItem> pathItemEntry : paths.entrySet()) {
                for (Map.Entry<PathItem.HttpMethod, Operation> operationEntry : pathItemEntry.getValue().readOperationsMap().entrySet()) {
                    generateExample(serviceId, swagger, operationEntry.getKey().name(), operationEntry.getValue(), pathItemEntry.getKey());
                    }
                }
            }
        } catch (Exception e) {
         log.warn("Cannot generate example from API doc file {}", apiDoc, e);
        }
    }

    /**
     * To find a prepared example for specific endpoint defined by request method and URL path. If no example is found
     * it returns the default one (empty JSON object).
     * @param method HTTP method to find
     * @param path URL path to find
     * @return example generated by loaded API doc (see previous call of {@link #generateExamples(String, String)}) or
     *         the default one (empty JSON object)
     */
    public Example getExample(String method, String path) {
        List<Example> byMethod = examples.get(method);
        if (byMethod == null) return DEFAULT_EXAMPLE;

        return byMethod.stream()
            .filter(e -> e.isMatching(path))
            .findFirst()
            .orElse(DEFAULT_EXAMPLE);
    }

    /**
     * It writes a response with an example for specified endpoint.
     * @param httpServletResponse response to be handled
     * @param method request method of the endpoint
     * @param path URL path of the endpoint
     * @throws IOException
     */
    public void replyExample(HttpServletResponse httpServletResponse, String method, String path) throws IOException {
        Example example = getExample(method, path);
        httpServletResponse.setContentType(example.getMediaType());
        httpServletResponse.setStatus(example.getResponseCode());
        try (PrintWriter pw = httpServletResponse.getWriter()) {
            pw.print(example.getContent());
        }
    }

    /**
     * Data object to define example
     */
    @Builder
    @Value
    static class Example {

        private final AntPathMatcher pathMatcher = new AntPathMatcher();
        private final String path;
        private final String method;
        private final int responseCode;
        private final String content;
        private final String mediaType;

        /**
         * Checking if the ant pattern of the example is matching with the requested path
         * @param uri tested URI
         * @return true if example is matching, otherwise false
         */
        boolean isMatching(String uri) {
            return pathMatcher.match(this.path, uri);
        }

    }

}
