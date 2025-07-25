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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.openapidiff.core.OpenApiCompare;
import org.openapitools.openapidiff.core.model.ChangedOpenApi;
import org.openapitools.openapidiff.core.output.HtmlRender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zowe.apiml.apicatalog.exceptions.ApiDiffNotAvailableException;
import org.zowe.apiml.apicatalog.exceptions.ApiDocNotFoundException;
import org.zowe.apiml.apicatalog.swagger.ApiDocService;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import static org.apache.hc.core5.http.HttpStatus.SC_OK;

/**
 * Main API for handling requests from the API Catalog UI, routed through the gateway
 */
@Slf4j
@Tag(name = "API Documentation")
@RequiredArgsConstructor
public class ApiDocController {

    private final ApiDocService apiDocService;

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
            @SecurityRequirement(name = "BasicAuthorization"), @SecurityRequirement(name = "CookieAuth")
        })
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "URI not found"),
        @ApiResponse(responseCode = "500", description = "An unexpected condition occurred"),
    })
    @ResponseBody
    public Mono<ResponseEntity<String>> getApiDocInfo(
        @Parameter(name = "serviceId", description = "The unique identifier of the registered service", required = true, example = "apicatalog")
        @PathVariable(value = "serviceId") String serviceId,
        @Parameter(name = "apiId", description = "The API ID and version, separated by a space, of the API documentation", required = true, example = "zowe.apiml.apicatalog v1.0.0")
        @PathVariable(value = "apiId") String apiId) {
        return apiDocService.retrieveApiDoc(serviceId, apiId)
            .map(apiDoc -> ResponseEntity
                .status(SC_OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(apiDoc)
            );
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
            @SecurityRequirement(name = "BasicAuthorization"), @SecurityRequirement(name = "CookieAuth")
        })
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "URI not found"),
        @ApiResponse(responseCode = "500", description = "An unexpected condition occurred"),
    })
    @ResponseBody
    public Mono<ResponseEntity<String>> getDefaultApiDocInfo(
        @Parameter(name = "serviceId", description = "The unique identifier of the registered service", required = true, example = "apicatalog")
        @PathVariable(value = "serviceId") String serviceId) {
        return apiDocService.retrieveDefaultApiDoc(serviceId)
            .map(apiDoc -> ResponseEntity
                .status(SC_OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(apiDoc)
            );
    }

    @GetMapping(value = "/{serviceId}/{apiId1}/{apiId2}", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Retrieve diff of two api versions for a specific service",
        description = "Returns an HTML document which details the difference between two versions of a API service",
        security = {
            @SecurityRequirement(name = "BasicAuthorization"), @SecurityRequirement(name = "CookieAuth")
        })
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "URI not found"),
        @ApiResponse(responseCode = "500", description = "An unexpected condition occurred")
    })
    @ResponseBody
    public Mono<ResponseEntity<String>> getApiDiff(
        @Parameter(name = "serviceId", description = "The unique identifier of the registered service", required = true, example = "apicatalog")
        @PathVariable(value = "serviceId") String serviceId,
        @Parameter(name = "apiId1", description = "The API ID and version, separated by a space, of the API documentation", required = true, example = "zowe.apiml.apicatalog v1.0.0")
        @PathVariable(value = "apiId1") String apiId1,
        @Parameter(name = "apiId2", description = "The API ID and version, separated by a space, of the API documentation", required = true, example = "zowe.apiml.apicatalog v2.0.0")
        @PathVariable(value = "apiId2") String apiId2) {

        return Mono.zip(
            apiDocService.retrieveApiDoc(serviceId, apiId1),
            apiDocService.retrieveApiDoc(serviceId, apiId2)
        ).flatMap(tuple -> Mono.fromCallable(() -> {
            ChangedOpenApi diff = OpenApiCompare.fromContents(tuple.getT1(), tuple.getT2());
            HtmlRender render = new HtmlRender();

            ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
            render.render(diff, new OutputStreamWriter(baos));
            String result = new String(baos.toByteArray(), StandardCharsets.UTF_8);

            // Remove external stylesheet
            result = result.replace("<link rel=\"stylesheet\" href=\"http://deepoove.com/swagger-diff/stylesheets/demo.css\">", "");
            return ResponseEntity
                .ok()
                .contentType(MediaType.TEXT_HTML)
                .body(result);
        }))
        .onErrorMap(e -> {
            if (e instanceof ApiDocNotFoundException) {
                return e;
            }
            return new ApiDiffNotAvailableException(
                String.format("Error retrieving API diff for '%s' with versions '%s' and '%s'", serviceId, apiId1, apiId2),
                e
            );
        });
    }

}

@RestController
@RequestMapping("/apicatalog/api/v1/apidoc")
@ConditionalOnBean(name = "modulithConfig")
class ApiDocControllerModulith extends ApiDocController {

    public ApiDocControllerModulith(ApiDocService apiDocService) {
        super(apiDocService);
    }

}

@RestController
@RequestMapping("/apicatalog/apidoc")
@ConditionalOnMissingBean(name = "modulithConfig")
class ApiDocControllerMicroservice extends ApiDocController {

    public ApiDocControllerMicroservice(ApiDocService apiDocService) {
        super(apiDocService);
    }

}
