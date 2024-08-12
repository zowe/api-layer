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
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.gateway.service.CertificateChainService;
import reactor.core.publisher.Mono;

/**
 * This simple controller provides a public endpoint with the client certificate chain.
 */
@RequiredArgsConstructor
@Tag(name = "Certificates")
@RestController
@RequestMapping({ CertificatesRestController.CONTROLLER_PATH, CertificatesRestController.CONTROLLER_FULL_PATH })
public class CertificatesRestController {
    public static final String CONTROLLER_PATH = "/gateway/certificates";
    public static final String CONTROLLER_FULL_PATH = "/gateway/api/v1/certificates";

    private final CertificateChainService certificateChainService;

    @GetMapping
    @Operation(summary = "Returns the certificate chain that is used by Gateway", operationId = "getCertificates")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successful responding of certificates", content = @Content(
            mediaType = MediaType.TEXT_PLAIN_VALUE,
            schema = @Schema(implementation = String.class)
        ))
    })
    public Mono<String> getCertificates() {
        return Mono.just(certificateChainService.getCertificatesInPEMFormat());
    }
}
