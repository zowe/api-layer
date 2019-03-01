/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.controllers.api;

import com.ca.mfaas.apicatalog.services.status.APIServiceStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Main API for handling requests from the API Catalog UI, routed through the gateway
 */
@Slf4j
@RestController
@RequestMapping("/apidoc")
@Api(tags = {"API Documentation"},
    description = "Service documentation")
public class CatalogApiDocController {

    private final APIServiceStatusService apiServiceStatusService;

    /**
     * Create the controller and autowire in the repository services
     *
     * @param apiServiceStatusService repo service for registered services
     */
    @Autowired
    public CatalogApiDocController(APIServiceStatusService apiServiceStatusService) {
        this.apiServiceStatusService = apiServiceStatusService;
    }


    /**
     * Retrieve the api-doc info for this service
     *
     * @param serviceId  the eureka id
     * @param apiVersion the version of the api
     * @return api-doc info (as JSON)
     */
    @GetMapping(value = "/{service-id}/{api-version}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ApiOperation(value = "Retrieves the API documentation for a specific service version",
        notes = "Returns the API documentation for a specific service {service-id} and version {api-version}",
        response = String.class)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 401, message = "Unauthorized"),
        @ApiResponse(code = 403, message = "Forbidden"),
        @ApiResponse(code = 404, message = "URI not found"),
        @ApiResponse(code = 500, message = "An unexpected condition occurred"),
    })
    public Mono<ResponseEntity<String>> getApiDocInfo(
        @ApiParam(name = "service-id", value = "The unique identifier of the registered service", required = true, example = "apicatalog")
        @PathVariable(value = "service-id") String serviceId,
        @ApiParam(name = "api-version", value = "The major version of the API documentation (v1, v2, etc.)", required = true, example = "v1")
        @PathVariable(value = "api-version") String apiVersion) {
        return Mono.just(this.apiServiceStatusService.getServiceCachedApiDocInfo(serviceId, apiVersion));
    }
}
