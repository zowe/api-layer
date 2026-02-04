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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.passticket.PassTicketException;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.zaas.security.mapping.NativeMapperWrapper;
import org.zowe.commons.usermap.MapperResponse;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller offer method to control security. It can contain method for user
 * and also method for calling services
 * by gateway to distribute state of authentication between nodes.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping(SecurityTokenServiceController.CONTROLLER_PATH)
@Slf4j
public class SecurityTokenServiceController {

    @Value("${apiml.security.oidc.registry:}")
    protected String registry;

    private final PassTicketService passTicketService;
    private final NativeMapperWrapper nativeMapper;

    public static final String CONTROLLER_PATH = "/zaas/api/v1/auth/delegate";
    public static final String PASSTICKET_PATH = "/passticket";

    @PostMapping(value = SecurityTokenServiceController.PASSTICKET_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ConditionalOnProperty(value = "apiml.security.delegatePassticket.enabled", havingValue = "true", matchIfMissing = false)
    @PreAuthorize("@safMethodSecurityExpressionRoot.hasSafServiceResourceAccess('DELEGATE.PASSTICKET', 'READ',#root)")
    @Hidden
    public ResponseEntity<PassTicketResponse> getPassTicket(@RequestBody PassTicketRequest passticketRequest)
            throws Exception {
        String applID = passticketRequest.getApplId();
        String emailID = passticketRequest.getEmailId();
        String zosUserId = "";

        if (Strings.isBlank(emailID) || Strings.isBlank(applID)) {
            log.debug("getPassTicket: Invalid applId or EmailId");
            return ResponseEntity.badRequest().build();
        }
        try {
            MapperResponse response = nativeMapper.getUserIDForDN(emailID, registry);
            if (response.getRc() == 0 && StringUtils.isNotEmpty(response.getUserId())) {
                zosUserId = response.getUserId();
            }
            log.debug("getPassTicket: Processing request for ZOS userId: {}", zosUserId);
            var ticket = passTicketService.generate(zosUserId, applID);
            log.debug("getPassTicket: Request processed with emailId: {} and ZOS userId: {}", emailID, zosUserId);
            return ResponseEntity.ok(new PassTicketResponse(ticket, zosUserId));
        } catch (PassTicketException ex) {
            log.error("getPassTicket: Failed to generate passticket", ex);
            return ResponseEntity.internalServerError().build();
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