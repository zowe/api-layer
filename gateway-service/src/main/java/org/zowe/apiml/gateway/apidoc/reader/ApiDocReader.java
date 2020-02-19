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

import static org.springframework.util.ResourceUtils.CLASSPATH_URL_PREFIX;

@Slf4j
@Service
public class ApiDocReader {

    public OpenAPI load(String location) {
        if (location == null || location.isEmpty()) {
            throw new ApiDocReaderException("API doc location can't be null or empty");
        }

        if (!location.startsWith(CLASSPATH_URL_PREFIX)) {
            location = CLASSPATH_URL_PREFIX + location.trim();
        }

        String openAPIJsonContent = getOpenAPIJsonContent(location);

        SwaggerParseResult parseResult = new OpenAPIV3Parser().readContents(openAPIJsonContent);
        OpenAPI openAPI = parseResult.getOpenAPI();
        if (openAPI == null) {
            log.debug("Could not convert response body to an OpenAPI object");
            throw new ApiDocReaderException("OpenAPI content is not valid");
        }

        return openAPI;
    }

    private String getOpenAPIJsonContent(String location) {
        String openAPIJsonContent;
        try {
            File file = ResourceUtils.getFile(location);
            if (!file.exists()) {
                openAPIJsonContent =  getDocumentationAsFileInJar(location);
            } else {
                openAPIJsonContent = getOpenAPIJsonContent2(file);
            }
        } catch (FileNotFoundException e) {
            openAPIJsonContent = getDocumentationAsFileInJar(location);
        }
        return openAPIJsonContent;
    }

    /**
     * Load the swagger as a file system file in a jar
     * @return the swagger as a string
     * @throws ApiDocReaderException if something bad happened
     */
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

    private String getOpenAPIJsonContent2(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            throw new ApiDocReaderException("OpenAPI file does not exist");
        }
    }
}
