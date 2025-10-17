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

    public static final String CONTROLLER_PATH = "/zaas/api/v1/auth/delegations";
    public static final String ISSUE_PASSTICKET_PATH = "/ticket";

    /**
     * Public API: Issue a new passticket for the given emailId and applid.
     */
    @PostMapping(path = ISSUE_PASSTICKET_PATH)
    @ResponseBody
    @Operation(
        summary = "Generate a passticket for the given emailId and applId.",
        tags = {"Security"},
        operationId = "issueDelegatedPassticket",
        description = "Issues a new passticket for the given emailId, and api authenticated via X509 cert token.",
        security = {
            @SecurityRequirement(name = "Bearer")
        }
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "PassTicket issued successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<PassticketResponse> issuePassticket(@RequestBody PassticketRequest request) {
        String emailId = request.getEmailId();
        String applid = request.getApplid();
        // ...passticket issuing logic using emailId and applid...
        PassticketResponse response = new PassticketResponse();
        // Implement  actual logic
        response.setPassticket(String.format("havenkat-passticket-for-%s-%s", emailId, applid)); 
        response.setUserid("12345678"); 
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Data
    public static class PassticketRequest {
        private String emailId;
        private String applid;
    }

    @Data
    public static class PassticketResponse {
        private String passticket;
        private String userid;
    }

    @Data
    public static class ValidateRequestModel {
        private String token;
    }
}
