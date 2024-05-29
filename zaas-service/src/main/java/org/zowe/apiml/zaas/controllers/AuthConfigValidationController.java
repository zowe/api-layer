/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.controllers;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.zaas.security.login.Providers;
import org.zowe.apiml.zaas.security.service.TokenCreationService;

@RequiredArgsConstructor
@RestController
@RequestMapping(AuthConfigValidationController.CONTROLLER_PATH)
public class AuthConfigValidationController {

    public static final String CONTROLLER_PATH = "zaas/validate";

    private final Providers providers;
    private final TokenCreationService tokenCreationService;

    @GetMapping(path = "")
    @Operation
    public ResponseEntity<String> validateAuth() {
        if (providers.isZosfmUsed()) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body("apimlAuthenticationToken cookie was not provided");
        }
        try {
            tokenCreationService.createJwtTokenWithoutCredentials("validate");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body("PassTicket cannot be generated with the z/OSMF provider");
        }
    }
}
