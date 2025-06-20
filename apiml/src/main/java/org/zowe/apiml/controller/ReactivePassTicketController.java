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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.ticket.TicketRequest;
import org.zowe.apiml.ticket.TicketResponse;
import reactor.core.publisher.Mono;

import java.util.Objects;

import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

@RestController
@RequestMapping("/gateway/api/v1/auth")
@Slf4j
@RequiredArgsConstructor
public class ReactivePassTicketController {

    private final PassTicketService passTicketService;

    @PostMapping(value = "/ticket", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Generate a passticket for the user associated with a token.",
        tags = {"Security"},
        operationId = "GenerateTicketUsingPOST",
        description = """
            Use the `/ticket` API to request a passticket for the user associated with a token.

            This endpoint is protect by a client certificate.

            **HTTP Headers:**

                The ticket request requires the token in one of the following formats:
                    * Cookie named `apimlAuthenticationToken`.
                    * Bearer authentication

                *Header example:* Authorization: Bearer *token*

            **Request payload:**
                The request takes one parameter, the name of the application for which the passticket should be generated. This parameter must be supplied.

            **Response Payload:**

                The response is a JSON object, which contains information associated with the ticket.
        """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = TicketResponse.class
                )
            )
        ),
        @ApiResponse(responseCode = "400", description = "Incorrect applicationName parameter. The parameter is not provided, is invalid or not defined to security."),
        @ApiResponse(responseCode = "401", description = "Zowe token is not provided, is invalid or is expired."),
        @ApiResponse(responseCode = "403", description = "A client certificate is not provided or is expired."),
        @ApiResponse(responseCode = "500", description = "The external security manager failed to generate a PassTicket for the user and application specified.")
    })
    public Mono<ResponseEntity<TicketResponse>> createPassTicket(@RequestBody(required = false) TicketRequest request) {
        if (request == null || StringUtils.isBlank(request.getApplicationName())) {
            throw new IncorrectPassTicketRequestBodyException();
        }

        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .filter(Objects::nonNull)
            .filter(Authentication::isAuthenticated)
            .filter(TokenAuthentication.class::isInstance)
            .map(TokenAuthentication.class::cast)
            .map(tokenAuthentication -> {
                var ticket = passTicketService.generate(tokenAuthentication.getPrincipal(), request.getApplicationName());
                var ticketResponse = new TicketResponse(tokenAuthentication.getCredentials(), tokenAuthentication.getPrincipal(), request.getApplicationName(), ticket);
                return ResponseEntity
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ticketResponse);
            })
            .switchIfEmpty(Mono.just(ResponseEntity.status(SC_UNAUTHORIZED).build()));
    }

}
