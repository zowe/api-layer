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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * Controller for Security Token Service (STS) operations, similar to AuthController.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping(StsController.CONTROLLER_PATH)
@Slf4j
public class StsController {

    public static final String CONTROLLER_PATH = "/zaas/api/v1/sts";
    public static final String ISSUE_TOKEN_PATH = "/issue-token";
    public static final String ISSUE_PASSTICKET_PATH = "/issue-passticket";


    /**
     * Public API: Issue a new security token
     */
    @PostMapping(path = ISSUE_TOKEN_PATH)
    @ResponseBody
    @Operation(summary = "Issue a new security token.",
        tags = {"STS"},
        operationId = "issueSecurityToken",
        description = "Issues a new security token for the authenticated user.",
        security = {
            @SecurityRequirement(name = "Bearer")
        }
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token issued successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<String> issueToken(@RequestBody Map<String, String> request) {
        // ...token issuing logic...
        String token = "dummy-token"; // Replace with actual logic
        return new ResponseEntity<>(token, HttpStatus.OK);
    }

    /**
     * Public API: Issue a new passticket
     */
    @PostMapping(path = ISSUE_PASSTICKET_PATH)
    @ResponseBody
    @Operation(summary = "Issue a new passticket.",
        tags = {"STS"},
        operationId = "issuePassticket",
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
