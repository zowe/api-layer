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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.ticket.TicketRequest;
import org.zowe.apiml.ticket.TicketResponse;
import reactor.core.publisher.Mono;

import java.util.Objects;

@RestController
@RequestMapping("/gateway/api/v1/auth")
@Slf4j
@RequiredArgsConstructor
public class ReactivePassTicketController {

    private final PassTicketService passTicketService;

    @PostMapping(value = "/ticket", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
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
            });
    }

}
