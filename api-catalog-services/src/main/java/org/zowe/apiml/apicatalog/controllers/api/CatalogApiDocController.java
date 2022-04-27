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

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.zowe.apiml.apicatalog.services.status.APIServiceStatusService;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.apicatalog.services.status.model.ApiDocNotFoundException;

/**
 * Main API for handling requests from the API Catalog UI, routed through the gateway
 */
@RestController
@RequestMapping("/apidoc")
@Api(tags = {"API Documentation"})
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
     * @param apiId the version of the api
     * @return api-doc info (as JSON)
     */
    @GetMapping(value = "/{serviceId}/{apiId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Retrieves the API documentation for a specific service version",
        notes = "Returns the API documentation for a specific service {serviceId} and version {apiId}. When " +
            " the API documentation for the specified version is not found, the first discovered version will be used.",
        authorizations = {
            @Authorization("LoginBasicAuth"), @Authorization("CookieAuth")
        },
        response = String.class)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 401, message = "Unauthorized"),
        @ApiResponse(code = 403, message = "Forbidden"),
        @ApiResponse(code = 404, message = "URI not found"),
        @ApiResponse(code = 500, message = "An unexpected condition occurred"),
    })
    @HystrixCommand
    public ResponseEntity<String> getApiDocInfo(
        @ApiParam(name = "serviceId", value = "The unique identifier of the registered service", required = true, example = "apicatalog")
        @PathVariable(value = "serviceId") String serviceId,
        @ApiParam(name = "apiId", value = "The API ID and version, separated by a space, of the API documentation", required = true, example = "zowe.apiml.apicatalog v1.0.0")
        @PathVariable(value = "apiId") String apiId) {
        try {
            return this.apiServiceStatusService.getServiceCachedApiDocInfo(serviceId, apiId);
        } catch (ApiDocNotFoundException e) {
            return this.apiServiceStatusService.getServiceCachedApiDocInfo(serviceId, null);
        }
    }
    /**
     * Retrieve the api-doc info for this service's default API
     *
     * @param serviceId  the eureka id
     * @return api-doc info (as JSON)
     */
    @GetMapping(value = "/{serviceId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Retrieves the API documentation for the default service version",
        notes = "Returns the API documentation for a specific service {serviceId} and it's default version.",
        authorizations = {
            @Authorization("LoginBasicAuth"), @Authorization("CookieAuth")
        },
        response = String.class)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 401, message = "Unauthorized"),
        @ApiResponse(code = 403, message = "Forbidden"),
        @ApiResponse(code = 404, message = "URI not found"),
        @ApiResponse(code = 500, message = "An unexpected condition occurred"),
    })
    @HystrixCommand
    public ResponseEntity<String> getDefaultApiDocInfo(
        @ApiParam(name = "serviceId", value = "The unique identifier of the registered service", required = true, example = "apicatalog")
        @PathVariable(value = "serviceId") String serviceId) {
        return this.apiServiceStatusService.getServiceCachedDefaultApiDocInfo(serviceId);
    }

    @GetMapping(value = "/{serviceId}/{apiId1}/{apiId2}", produces = MediaType.TEXT_HTML_VALUE)
    @ApiOperation(value = "Retrieve diff of two api versions for a specific service",
        notes = "Returns an HTML document which details the difference between two versions of a API service",
        authorizations = {
            @Authorization("LoginBasicAuth"), @Authorization("CookieAuth")
        },
        response = String.class)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 401, message = "Unauthorized"),
        @ApiResponse(code = 403, message = "Forbidden"),
        @ApiResponse(code = 404, message = "URI not found"),
        @ApiResponse(code = 500, message = "An unexpected condition occurred"),
    })
    @HystrixCommand
    public ResponseEntity<String> getApiDiff(
        @ApiParam(name = "serviceId", value = "The unique identifier of the registered service", required = true, example = "apicatalog")
        @PathVariable(value = "serviceId") String serviceId,
        @ApiParam(name = "apiId1", value = "The API ID and version, separated by a space, of the API documentation", required = true, example = "zowe.apiml.apicatalog v1.0.0")
        @PathVariable(value = "apiId1") String apiId1,
        @ApiParam(name = "apiId2", value = "The API ID and version, separated by a space, of the API documentation", required = true, example = "zowe.apiml.apicatalog v2.0.0")
        @PathVariable(value = "apiId2") String apiId2) {
        return this.apiServiceStatusService.getApiDiffInfo(serviceId, apiId1, apiId2);
    }
}
