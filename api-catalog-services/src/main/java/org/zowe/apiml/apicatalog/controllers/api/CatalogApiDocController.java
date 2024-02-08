/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.controllers.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.apicatalog.services.status.APIServiceStatusService;

/**
 * Main API for handling requests from the API Catalog UI, routed through the gateway
 */
@RestController
@RequestMapping("/apidoc")
@Tag(name = "API Documentation")
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
     * @param serviceId the eureka id
     * @param apiId     the version of the api
     * @return api-doc info (as JSON)
     */
    @GetMapping(value = "/{serviceId}/{apiId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Retrieves the API documentation for a specific service version",
        description = "Returns the API documentation for a specific service {serviceId} and version {apiId}. When " +
            " the API documentation for the specified version is not found, the first discovered version will be used.",
        security = {
            @SecurityRequirement(name = "Basic authorization"), @SecurityRequirement(name = "CookieAuth")
        })
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "URI not found"),
        @ApiResponse(responseCode = "500", description = "An unexpected condition occurred"),
    })
    public ResponseEntity<String> getApiDocInfo(
        @Parameter(name = "serviceId", description = "The unique identifier of the registered service", required = true, example = "apicatalog")
        @PathVariable(value = "serviceId") String serviceId,
        @Parameter(name = "apiId", description = "The API ID and version, separated by a space, of the API documentation", required = true, example = "zowe.apiml.apicatalog v1.0.0")
        @PathVariable(value = "apiId") String apiId) {
        return this.apiServiceStatusService.getServiceCachedApiDocInfo(serviceId, apiId);
    }

    /**
     * Retrieve the api-doc info for this service's default API
     *
     * @param serviceId the eureka id
     * @return api-doc info (as JSON)
     */
    @GetMapping(value = "/{serviceId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Retrieves the API documentation for the default service version",
        description = "Returns the API documentation for a specific service {serviceId} and its default version.",
        security = {
            @SecurityRequirement(name = "Basic authorization"), @SecurityRequirement(name = "CookieAuth")
        })
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "URI not found"),
        @ApiResponse(responseCode = "500", description = "An unexpected condition occurred"),
    })
    public ResponseEntity<String> getDefaultApiDocInfo(
        @Parameter(name = "serviceId", description = "The unique identifier of the registered service", required = true, example = "apicatalog")
        @PathVariable(value = "serviceId") String serviceId) {
        return this.apiServiceStatusService.getServiceCachedDefaultApiDocInfo(serviceId);
    }

    @GetMapping(value = "/{serviceId}/{apiId1}/{apiId2}", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Retrieve diff of two api versions for a specific service",
        description = "Returns an HTML document which details the difference between two versions of a API service",
        security = {
            @SecurityRequirement(name = "Basic authorization"), @SecurityRequirement(name = "CookieAuth")
        })
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "URI not found"),
        @ApiResponse(responseCode = "500", description = "An unexpected condition occurred")
    })
    public ResponseEntity<String> getApiDiff(
        @Parameter(name = "serviceId", description = "The unique identifier of the registered service", required = true, example = "apicatalog")
        @PathVariable(value = "serviceId") String serviceId,
        @Parameter(name = "apiId1", description = "The API ID and version, separated by a space, of the API documentation", required = true, example = "zowe.apiml.apicatalog v1.0.0")
        @PathVariable(value = "apiId1") String apiId1,
        @Parameter(name = "apiId2", description = "The API ID and version, separated by a space, of the API documentation", required = true, example = "zowe.apiml.apicatalog v2.0.0")
        @PathVariable(value = "apiId2") String apiId2) {
        return this.apiServiceStatusService.getApiDiffInfo(serviceId, apiId1, apiId2);
    }
}
