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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zowe.commons.usermap.MapperResponse;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.zaas.security.mapping.NativeMapperWrapper;


/**
 * Controller offer method to control security. It can contain method for user
 * and also method for calling services
 * by gateway to distribute state of authentication between nodes.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping(StsController.CONTROLLER_PATH)
@Slf4j
public class StsController {

    @Value("${apiml.security.oidc.registry:}")
    protected String registry;

    private final PassTicketService passTicketService;
    private final NativeMapperWrapper nativeMapper;

    public static final String CONTROLLER_PATH = "/zaas/api/v1/auth/delegate";
    public static final String PASSTICKET_PATH = "/passticket";

    @PostMapping(value = StsController.PASSTICKET_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(description = "The authenticated service uses this endpoint to request a PassTicket for a target user (identified by emailId) for a specific z/OS application (applid). The incoming Bearer token is validated to ensure the requester is authorized to perform delegation before the ticket is generated.", tags = {
            "Security" }, security = {
                    @SecurityRequirement(name = "Bearer"),
                    @SecurityRequirement(name = "LoginBasicAuth"),
                    @SecurityRequirement(name = "ClientCert")
            })
    public ResponseEntity<PassTicketResponse> getPassTicket(@RequestBody PassTicketRequest passticketRequest)
            throws Exception {
        String applID = passticketRequest.getApplId();
        String emailID = passticketRequest.getEmailId();
        String zosUserId = "";

        if (Strings.isBlank(emailID) || Strings.isBlank(applID)) {
            return ResponseEntity.badRequest().build();
        }
        try {
            MapperResponse response = nativeMapper.getUserIDForDN(emailID, registry);
            if (response.getRc() == 0 && StringUtils.isNotEmpty(response.getUserId())) {
                zosUserId = response.getUserId();
            }
            log.info("Getting ZOS_User_id: {} ", zosUserId);
            var ticket = passTicketService.generate(zosUserId, applID);
            log.info("Getting request email id: {} and ZOS_Userid: {}", emailID, zosUserId);
            return ResponseEntity.ok(new PassTicketResponse(ticket, zosUserId));
        } catch (Exception ex) {
            log.error("Error calling delegate passticket api", ex);
            throw ex;
        }
    }

    @Data
    public static class PassTicketRequest {
        private String emailId;
        private String applId;
    }

    @Data
    @Builder
    public static class PassTicketResponse {
        private String passticket;
        private String tsoUserid;
    }

}
