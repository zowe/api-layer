/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.security.common.token.OIDCProvider;
import org.zowe.apiml.zaas.controllers.AuthController;
import org.zowe.apiml.zaas.security.webfinger.WebFingerProvider;
import org.zowe.apiml.zaas.security.webfinger.WebFingerResponse;
import reactor.core.publisher.Mono;

import java.io.IOException;

import static org.zowe.apiml.zaas.controllers.AuthController.OIDC_TOKEN_VALIDATE;
import static org.zowe.apiml.zaas.controllers.AuthController.OIDC_WEBFINGER_PATH;

@RestController
@RequestMapping("/gateway/api/v1/auth")
@Slf4j
@RequiredArgsConstructor
public class ReactiveOIDCController {

    private final WebFingerProvider webFingerProvider;
    @Nullable private final OIDCProvider oidcProvider;

    @Data
    public static class RulesRequestModel {
        private String serviceId;
        private String userId;
        private long timestamp;
    }

    @PostMapping(path = OIDC_TOKEN_VALIDATE)
    @Operation(summary = "Validate OIDC token",
        tags = {"OIDC"},
        operationId = "validateOIDCToken",
        description = "Use the `/oidc-token/validate` API to validate token against configured OIDC provider. " +
            "The Gateway can verify token locally or remotely depends on API Mediation Layer configuration.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                schema = @Schema(implementation = AuthController.ValidateRequestModel.class)
            ),
            description = "Specifies the OIDC token for validation without scopes (serviceId will be ignored)."
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Valid token"),
        @ApiResponse(responseCode = "401", description = "Invalid token or OIDC provider is not defined")
    })
    public Mono<ResponseEntity<Void>> validateOIDCToken(@RequestBody AuthController.ValidateRequestModel validateRequestModel) {
        return Mono.fromSupplier(() -> {
            log.debug("Validating OIDC token using provider {}", oidcProvider);
            var token = validateRequestModel.getToken();
            if (oidcProvider != null && oidcProvider.isValid(token)) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        });
    }

    /**
     * Proof of concept of WebFinger provider for OIDC clients.
     *
     * @return List of link's relation type and the target URI for provided clientID
     */
    @GetMapping(path = OIDC_WEBFINGER_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List of link's relation type and the target URI for provided clientID",
        tags = {"OIDC"},
        operationId = "getWebFinger",
        description = "[EXPERIMENTAL] The endpoint can be used to obtain links to authenticate against OIDC provider based on clientID provided in the request. " +
            "The links are defined in the configuration of the API Mediation Layer.",
        security = {
            @SecurityRequirement(name = "Bearer"),
            @SecurityRequirement(name = "CookieAuth"),
            @SecurityRequirement(name = "LoginBasicAuth")
        })
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "WebFinger is disabled"),
    })
    public Mono<ResponseEntity<Object>> getWebFinger(@RequestParam(name = "resource") String clientId) {
        return Mono.fromSupplier(() -> {
            if (webFingerProvider.isEnabled()) {
                try {
                    WebFingerResponse response = webFingerProvider.getWebFingerConfig(clientId);
                    return ResponseEntity.ok(response);
                } catch (IOException e) {
                    throw new InvalidWebFingerConfigurationException(e);
                }

            }
            return ResponseEntity.notFound().build();
        });

    }

}
