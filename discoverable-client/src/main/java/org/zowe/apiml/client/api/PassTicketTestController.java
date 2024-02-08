/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.client.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.passticket.IRRPassTicketEvaluationException;
import org.zowe.apiml.passticket.PassTicketService;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Controller for testing PassTickets.
 */
@RestController
@Tag(name = "Test Operations")
public class PassTicketTestController {

    @Value("${apiml.service.applId:ZOWEAPPL}")
    private String defaultApplId;

    private final PassTicketService passTicketService;

    public PassTicketTestController(PassTicketService passTicketService) {
        this.passTicketService = passTicketService;
    }

    /**
     * Validates the PassTicket in authorization header.
     */
    @GetMapping(value = "/api/v1/passticketTest")
    @Operation(summary = "Validate that the PassTicket in Authorization header is valid", tags = {"Test Operations"})
    public void passticketTest(@RequestHeader("authorization") String authorization,
                               @RequestHeader(value = "X-Zowe-Auth-Failure", required = false) String zoweAuthFailure,
                               @RequestParam(value = "applId", defaultValue = "", required = false) String applId)
        throws IRRPassTicketEvaluationException {
        if (authorization != null && authorization.toLowerCase().startsWith("basic")) {
            if (zoweAuthFailure != null) {
                throw new IllegalArgumentException("Scheme transformation happened and error was set in X-Zowe-Auth-Failure header");
            } else {
                String base64Credentials = authorization.substring("Basic".length()).trim();
                String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
                String[] values = credentials.split(":", 2);
                String userId = values[0];
                String passTicket = values[1];
                if (applId.isEmpty()) {
                    applId = defaultApplId;
                }
                passTicketService.evaluate(userId, applId, passTicket);
            }
        } else if (authorization != null && !authorization.toLowerCase().startsWith("basic") && zoweAuthFailure == null) {
            throw new IllegalArgumentException("Neither scheme transformation happened not error was set in X-Zowe-Auth-Failure header");
        }
    }
}
