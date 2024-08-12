/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.product.version.VersionInfo;
import org.zowe.apiml.product.version.VersionService;
import reactor.core.publisher.Mono;

/**
 * API for providing information about Zowe and API ML versions
 */

@AllArgsConstructor
@Tag(name = "Diagnostic")
@RestController
@RequestMapping({"/gateway", "/application", "/gateway/api/v1"})
public class VersionController {

    private VersionService versionService;

    @GetMapping(value = "/version", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Return version information of API Mediation Layer and Zowe.",
        operationId = "VersionInfoUsingGET",
        description = "Use the `/version` API to get the version information of API Mediation Layer and Zowe.\n" +
            "The version information includes version, build number and commit hash.\n" +
            "In the response can be only API ML version information or API ML and Zowe version information, this depends on API ML installed as part of Zowe build or as standalone application.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK", content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = VersionInfo.class)
        ))
    })
    public Mono<ResponseEntity<VersionInfo>> getVersion() {
        return Mono.just(ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(versionService.getVersion()));
    }
}
