/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.service.response.enable.api;


import com.broadcom.apiml.library.service.response.enable.services.LocalApiDocService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.springframework.util.ResourceUtils.CLASSPATH_URL_PREFIX;

/**
 * Controller for handling retrieval of API doc via the gateway
 */
@RestController
@RequestMapping("/")
@ConditionalOnProperty(prefix = "eureka.instance.metadata-map.mfaas.discovery", value = "enableApiDoc", havingValue = "true", matchIfMissing = true)

public class ApiDocController {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ApiDocController.class);
    private final boolean apiDocEnabled;
    private String swaggerLocation;
    private LocalApiDocService localApiDocService;

    /**
     * API Doc retrieval controller
     * Autowire in dependencies to controller
     *
     * @param apiDocEnabled      does the service have API Documentation
     * @param swaggerLocation    optional parameter to tell the controller where to load a static swagger file
     * @param localApiDocService retrieve the API doc locally and not through the gateway
     */
    @Autowired
    public ApiDocController(
        @Value("${eureka.instance.metadata-map.mfaas.discovery.enableApiDoc:true}") boolean apiDocEnabled,
        @Value("${eureka.instance.metadata-map.mfaas.api-info.swagger.location:}") String swaggerLocation,
        LocalApiDocService localApiDocService) {
        this.apiDocEnabled = apiDocEnabled;
        this.swaggerLocation = swaggerLocation;
        this.localApiDocService = localApiDocService;
    }

    /**
     * Retrieve the API doc for the given group (or default to the first alphanumeric version tag)
     *
     * @param apiDocGroup the group to retrieve
     * @return the API doc for a group
     * @throws IOException when loading the swagger resource fails
     */
    @GetMapping(value = "/api-doc", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getApiDoc(@RequestParam(value = "group", required = false) String apiDocGroup) throws IOException {
        if (swaggerLocation == null || swaggerLocation.isEmpty()) {
            return this.localApiDocService.getApiDoc(apiDocGroup);
        } else {
            return loadApiDocumentationFromStaticResourceFileAsJson();
        }
    }

    /**
     * Load the swagger/api doc info from a local resource file
     *
     * @return the swagger as a String
     * @throws IOException when reading the file fails
     */
    private String loadApiDocumentationFromStaticResourceFileAsJson() throws IOException {
        log.debug("Loading Api Documentation from static resource: " + swaggerLocation);
        try {
            if (!swaggerLocation.startsWith(CLASSPATH_URL_PREFIX)) {
                swaggerLocation = CLASSPATH_URL_PREFIX + swaggerLocation.trim();
            }
            try {
                File file = ResourceUtils.getFile(swaggerLocation);
                if (!file.exists()) {
                    return loadDocumentationAsFileInJar();
                } else {
                    return new String(Files.readAllBytes(file.toPath()));
                }
            } catch (FileNotFoundException e) {
                return loadDocumentationAsFileInJar();
            }
        } catch (IOException e) {
            log.error("An exception occurred when attempting to retrieve swagger file: "
                + swaggerLocation + ". " + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Load the swagger as a file system file in a jar
     *
     * @return the swagger as a string
     * @throws IOException if something bad happened
     */
    private String loadDocumentationAsFileInJar() throws IOException {
        log.debug("Loading Api Documentation from jar resource: " + swaggerLocation);
        ClassPathResource cpr = new ClassPathResource(swaggerLocation);
        try {
            byte[] bytes = FileCopyUtils.copyToByteArray(cpr.getInputStream());
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IOException("Cannot find Api Documentation (swagger) file: " + swaggerLocation);
        }
    }

    /**
     * Is API Doc enabled for the implementing service
     *
     * @return true if enabled
     */
    @GetMapping(value = "/api-doc/enabled", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public boolean isApiDocEnabled() {
        return this.apiDocEnabled;
    }
}
