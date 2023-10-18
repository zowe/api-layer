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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.security.service.TokenCreationService;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.gateway.zaas.ZosmfTokensResponse;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.ticket.TicketRequest;
import org.zowe.apiml.ticket.TicketResponse;

@RequiredArgsConstructor
@RestController
@RequestMapping(ZaasController.CONTROLLER_PATH)
@Slf4j
public class ZaasController {
    public static final String CONTROLLER_PATH = "gateway/zaas";
    static final String AUTH_SOURCE_ATTR = "zaas.auth.source";

    private final PassTicketService passTicketService;
    private final TokenCreationService tokenCreationService;
    private final AuthenticationService authenticationService;
    private final MessageService messageService;

    @PostMapping(path = "ticket", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Provides PassTicket for authenticated user.")
    @ResponseBody
    public ResponseEntity<Object> getPassTicket(@RequestBody TicketRequest ticketRequest, @RequestAttribute(AUTH_SOURCE_ATTR) AuthSource.Parsed authSource) {

        if (StringUtils.isEmpty(authSource.getUserId())) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .build();
        }

        final String applicationName = ticketRequest.getApplicationName();
        if (StringUtils.isBlank(applicationName)) {
            ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.security.ticket.invalidApplicationName").mapToView();
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(messageView);
        }

        String ticket = null;
        try {
            ticket = passTicketService.generate(authSource.getUserId(), applicationName);
        } catch (IRRPassTicketGenerationException e) {
            ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.security.ticket.generateFailed",
                e.getErrorCode().getMessage()).mapToView();
            return ResponseEntity
                .status(e.getHttpStatus())
                .body(messageView);
        }
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(new TicketResponse(null, authSource.getUserId(), applicationName, ticket));
    }

    @PostMapping(path = "zosmf", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Provides z/OSMF LTPA and JWT token for authenticated user.")
    @ResponseBody
    public ResponseEntity<Object> getZosmfTokens(@RequestAttribute(AUTH_SOURCE_ATTR) AuthSource.Parsed authSource) {

        if (StringUtils.isEmpty(authSource.getUserId())) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .build();
        }

        String jwtToken = null;
        String ltpaToken = null;
        try {
            jwtToken = tokenCreationService.createJwtTokenWithoutCredentials(authSource.getUserId());
            ltpaToken = authenticationService.getLtpaToken(jwtToken);
        } catch (Exception e) {
            ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.gateway.security.token.authenticationFailed").mapToView();
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(messageView);
        }
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(new ZosmfTokensResponse(jwtToken, ltpaToken));
    }
}
