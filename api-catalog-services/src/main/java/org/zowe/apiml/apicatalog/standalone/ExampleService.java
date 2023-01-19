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

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

@Slf4j
@Service
@ConditionalOnProperty(value = "apiml.catalog.standalone.enabled", havingValue = "true")
public class ExampleService {

    private static final Example DEFAULT_EXAMPLE = Example.builder().responseCode(200).content("{}").build();

    private static final Map<String, List<Example>> examples = Collections.synchronizedMap(new HashMap<>());

    {
        SimpleModule simpleModule = new SimpleModule().addSerializer(new JsonNodeExampleSerializer());
        Json.mapper().registerModule(simpleModule);
    }

    static void addExample(String method, String path, int responseCode, String mediaType, String content) {
        Example example = Example.builder()
            .method(method).path(path)
            .responseCode(responseCode).content(content)
            .build();

        List<Example> byMethod = examples.computeIfAbsent(method, k -> Collections.synchronizedList(new ArrayList<>()));
        byMethod.add(example);

        log.debug("Generated response example for request {}, {}, {}, {}:\n{}", method, path, responseCode, mediaType, content);
    }

    static Map.Entry<String, ApiResponse> getFirstApiRespones(Operation operation) {
        Map.Entry<String, ApiResponse> first = null;
        for (Map.Entry<String, ApiResponse> responseEntry : operation.getResponses().entrySet()) {
            if (responseEntry.getKey().startsWith("2")) return responseEntry;
            if (first == null) first = responseEntry;
        }
        return first;
    }

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
        Map.Entry<String, ApiResponse> responseEntry = getFirstApiRespones(operation);
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

    public void generateExamples(String serviceId, String apiDoc) {
        SwaggerParseResult parseResult = new OpenAPIParser().readContents(apiDoc, null, null);

        OpenAPI swagger = parseResult.getOpenAPI();
        Paths paths = swagger.getPaths();

        for (Map.Entry<String, PathItem> pathItemEntry : paths.entrySet()) {
            for (Map.Entry<PathItem.HttpMethod, Operation> operationEntry : pathItemEntry.getValue().readOperationsMap().entrySet()) {
                generateExample(serviceId, swagger, operationEntry.getKey().name(), operationEntry.getValue(), pathItemEntry.getKey());
            }
        }
    }

    public Example getExample(String method, String path) {
        List<Example> byMethod = examples.get(method);
        if (byMethod == null) return DEFAULT_EXAMPLE;

        return byMethod.stream()
            .filter(e -> e.isMatching(path))
            .findFirst()
            .orElse(DEFAULT_EXAMPLE);
    }

    public void replyExample(HttpServletResponse httpServletResponse, String method, String path) throws IOException {
        Example example = getExample(method, path);
        try (PrintWriter pw = httpServletResponse.getWriter()) {
            pw.print(example.getContent());
        }
        httpServletResponse.setStatus(example.getResponseCode());
    }

    @Builder
    @Value
    static class Example {

        private final AntPathMatcher pathMatcher = new AntPathMatcher();
        private final String path;
        private final String method;
        private final int responseCode;
        private final String content;

        boolean isMatching(String uri) {
            return pathMatcher.match(this.path, uri);
        }

    }

}
