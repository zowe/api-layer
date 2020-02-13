/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.apicatalog.apidoc.controllers;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.apicatalog.apidoc.services.LocalApiDocService;

import java.io.IOException;


/**
 * Controller for handling retrieval of API doc via the gateway
 */
@Slf4j
@RestController
@RequestMapping("/")
public class ApiDocController {

    private final boolean apiDocEnabled;
    private String swaggerLocation;
    private LocalApiDocService localApiDocService;

    /**
     * API Doc retrieval controller
     * Autowire in dependencies to controller
     * @param apiDocEnabled does the service have API Documentation
     * @param localApiDocService retrieve the API doc locally and not through the gateway
     */
    @Autowired
    public ApiDocController(
        @Value("${eureka.instance.metadata-map.apiml.service.catalog.enableApiDoc:true}") boolean apiDocEnabled,
        LocalApiDocService localApiDocService)  {
        this.apiDocEnabled = apiDocEnabled;
        this.localApiDocService = localApiDocService;
    }

    /**
     * Retrieve the API doc for the given group (or default to the first alphanumeric version tag)
     * @param apiDocGroup the group to retrieve
     * @return the API doc for a group
     * @throws IOException when loading the swagger resource fails
     */
    @GetMapping(value = "/api-doc", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getApiDoc(@RequestParam(value = "group", required = false) String apiDocGroup) {
            return this.localApiDocService.getApiDoc(apiDocGroup);
    }

    /**
     * Is API Doc enabled for the implementing service
     * @return true if enabled
     */
    @GetMapping(value = "/api-doc/enabled", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public boolean isApiDocEnabled()  {
        return this.apiDocEnabled;
    }
}
