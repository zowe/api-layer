/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.zaas;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zowe.apiml.gateway.security.service.saf.SafIdtAuthException;
import org.zowe.apiml.gateway.security.service.saf.SafIdtException;
import org.zowe.apiml.gateway.security.service.saf.SafIdtProvider;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.gateway.security.service.zosmf.ZosmfService;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.ticket.TicketRequest;
import org.zowe.apiml.ticket.TicketResponse;
import org.zowe.apiml.zaas.ZaasSafIdtResponse;
import org.zowe.apiml.zaas.ZaasTokenResponse;

import java.util.Arrays;

import static org.zowe.apiml.gateway.filters.pre.ExtractAuthSourceFilter.AUTH_SOURCE_ATTR;
import static org.zowe.apiml.gateway.filters.pre.ExtractAuthSourceFilter.AUTH_SOURCE_PARSED_ATTR;
import static org.zowe.apiml.security.SecurityUtils.COOKIE_AUTH_NAME;

@RequiredArgsConstructor
@RestController
@RequestMapping(ZaasController.CONTROLLER_PATH)
@Slf4j
public class ZaasController {
    public static final String CONTROLLER_PATH = "gateway/zaas";

    private final AuthSourceService authSourceService;
    private final MessageService messageService;
    private final PassTicketService passTicketService;
    private final ZosmfService zosmfService;
    private final SafIdtProvider safIdtProvider;

    @PostMapping(path = "ticket", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Provides PassTicket for authenticated user.")
    public ResponseEntity<Object> getPassTicket(@RequestBody TicketRequest ticketRequest, @RequestAttribute(AUTH_SOURCE_PARSED_ATTR) AuthSource.Parsed authSourceParsed) {

        if (StringUtils.isEmpty(authSourceParsed.getUserId())) {
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
            ticket = passTicketService.generate(authSourceParsed.getUserId(), applicationName);
        } catch (IRRPassTicketGenerationException e) {
            ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.security.ticket.generateFailed",
                e.getErrorCode().getMessage()).mapToView();
            return ResponseEntity
                .status(e.getHttpStatus())
                .body(messageView);
        }
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(new TicketResponse(null, authSourceParsed.getUserId(), applicationName, ticket));
    }

    @PostMapping(path = "zosmf", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Provides z/OSMF JWT or LTPA token for authenticated user.")
    public ResponseEntity<Object> getZosmfToken(@RequestAttribute(AUTH_SOURCE_ATTR) AuthSource authSource,
                                                @RequestAttribute(AUTH_SOURCE_PARSED_ATTR) AuthSource.Parsed authSourceParsed) {
        try {
            ZaasTokenResponse zaasTokenResponse = zosmfService.exchangeAuthenticationForZosmfToken(authSource.getRawSource().toString(), authSourceParsed);

            return ResponseEntity
                .status(HttpStatus.OK)
                .body(zaasTokenResponse);

        } catch (Exception e) {
            ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.zaas.zosmf.noZosmfTokenReceived", e.getMessage()).mapToView();
            return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(messageView);
        }
    }


    @PostMapping(path = "zoweJwt", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Provides zoweJwt for authenticated user.")
    public ResponseEntity<Object> getZoweJwt(@RequestAttribute(AUTH_SOURCE_ATTR) AuthSource authSource) {
        try {
            String token = authSourceService.getJWT(authSource);

            return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ZaasTokenResponse(COOKIE_AUTH_NAME, token));

        } catch (Exception e) {
            ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.zaas.zoweJwt.noToken", e.getMessage()).mapToView();
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(messageView);
        }
    }

    @PostMapping(path = "safIdt", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Provides SAF Identity Token for authenticated user.")
    public ResponseEntity<Object> getSafIdToken(@RequestBody TicketRequest ticketRequest, @RequestAttribute(AUTH_SOURCE_PARSED_ATTR) AuthSource.Parsed authSourceParsed) {

        final String userId = authSourceParsed.getUserId();
        if (StringUtils.isEmpty(userId)) {
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

        char[] passTicket = "".toCharArray();
        try {
            passTicket = passTicketService.generate(userId, applicationName).toCharArray();
            String safIdToken = safIdtProvider.generate(userId, passTicket, applicationName);

            return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ZaasSafIdtResponse(safIdToken));

        } catch (IRRPassTicketGenerationException e) {
            ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.security.ticket.generateFailed",
                e.getErrorCode().getMessage()).mapToView();
            return ResponseEntity
                .status(e.getHttpStatus())
                .body(messageView);
        } catch (SafIdtException | SafIdtAuthException e) {
            ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.security.idt.failed", e.getMessage()).mapToView();
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(messageView);
        } finally {
            Arrays.fill(passTicket, (char) 0);
        }
    }

}
