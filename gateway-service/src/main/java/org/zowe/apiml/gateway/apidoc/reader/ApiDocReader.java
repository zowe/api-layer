/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.apidoc.reader;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;


/**
 * Service class for loading Gateway API doc from a resource file
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiDocReader {

    /**
     * Load the swagger/api doc info from a local resource file
     * @param location the location of the local resource file
     * @return the OpenApi as a String object
     * @throws ApiDocReaderException when the conversion to an OpenAPI object fails
     */
    public String load(String location) {
        if (location == null || location.isEmpty()) {
            throw new ApiDocReaderException("API doc location can't be null or empty");
        }


        String openAPIJsonContent = getOpenAPIJsonContent(location.trim());
        boolean isValidJson = isValidOpenApi3Content(openAPIJsonContent);
        if (!isValidJson) {
            log.debug("Could not convert response body to an OpenAPI object");
            throw new ApiDocReaderException("OpenAPI content is not valid");
        }

        return openAPIJsonContent;
    }

    public OpenAPI load2(String location) {
        if (location == null || location.isEmpty()) {
            throw new ApiDocReaderException("API doc location can't be null or empty");
        }


        String openAPIJsonContent = getOpenAPIJsonContent(location.trim());

        SwaggerParseResult parseResult = new OpenAPIV3Parser().readContents(openAPIJsonContent);
        OpenAPI openAPI = parseResult.getOpenAPI();
        if (openAPI == null) {
            log.debug("Could not convert response body to an OpenAPI object");
            throw new ApiDocReaderException("OpenAPI content is not valid");
        }

        return openAPI;
    }

    private boolean isValidOpenApi3Content(String openAPIJsonContent) {
        SwaggerParseResult parseResult = new OpenAPIV3Parser().readContents(openAPIJsonContent);
        return parseResult.getOpenAPI() != null;
    }

    private String getOpenAPIJsonContent(String location) {
        String openAPIJsonContent;
        try {
            File file = ResourceUtils.getFile(location);
            if (!file.exists()) {
                openAPIJsonContent =  getDocumentationAsFileInJar(location);
            } else {
                openAPIJsonContent = getDocumentationFromPath(file);
            }
        } catch (FileNotFoundException e) {
            openAPIJsonContent = getDocumentationAsFileInJar(location);
        }
        return openAPIJsonContent;
    }

    private String getDocumentationAsFileInJar(String swaggerLocation) {
        log.debug("Loading Api Documentation from jar resource: " + swaggerLocation);
        ClassPathResource cpr = new ClassPathResource(swaggerLocation);
        try {
            byte[] bytes = FileCopyUtils.copyToByteArray(cpr.getInputStream());
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ApiDocReaderException("OpenAPI file does not exist: " + swaggerLocation);
        }
    }

    private String getDocumentationFromPath(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            throw new ApiDocReaderException("OpenAPI file does not exist");
        }
    }
}
