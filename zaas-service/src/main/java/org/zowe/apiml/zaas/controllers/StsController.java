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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for Security Token Service (STS) operations, similar to AuthController.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping(StsController.CONTROLLER_PATH)
@Slf4j
public class StsController {

    public static final String CONTROLLER_PATH = "/zaas/api/v1/auth/delegate";    
    public static final String ISSUE_PASSTICKET_PATH = "/ticket";

    /**
     * Public API: Issue a new passticket
     */
    @PostMapping(path = ISSUE_PASSTICKET_PATH)
    @ResponseBody
    @Operation(summary = "Issue a new passticket.",
        tags = {"Security"},
        operationId = "issueDelegatedPassticket",
        description = "Issues a new passticket for the authenticated user.",
        security = {
            @SecurityRequirement(name = "Bearer")
        }
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Passticket issued successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<String> issuePassticket(@RequestBody PassticketRequest request) {
        // Extract email_id and application_id from payload
        String emailId = request.getEmailId();
        String applicationId = request.getApplicationId();
        // ...passticket issuing logic using emailId and applicationId...
        String passticket = String.format("dummy-passticket-for-%s-%s", emailId, applicationId); // Replace with actual logic
        return new ResponseEntity<>(passticket, HttpStatus.OK);
    }

    @Data
    public static class PassticketRequest {
        private String emailId;
        private String applicationId;
    }

    @Data
    public static class ValidateRequestModel {
        private String token;
    }
}
