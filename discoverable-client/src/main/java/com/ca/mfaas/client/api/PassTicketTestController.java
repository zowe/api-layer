/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.client.api;

import com.ca.apiml.security.common.service.IRRPassTicketEvaluationException;
import com.ca.apiml.security.common.service.PassTicketService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Controller for testing PassTickets.
 */
@RestController
@Api(tags = { "Test Operations" }, description = "Operations for APIML Testing")
public class PassTicketTestController {

    @Value("${apiml.service.applId:ZOWEAPPL}")
    private String applId;

    private final PassTicketService passTicketService;

    public PassTicketTestController(PassTicketService passTicketService) {
        this.passTicketService = passTicketService;
    }

    /**
     * Validates the PassTicket in authorization header.
     */
    @GetMapping(value = "/api/v1/passticketTest")
    @ApiOperation(value = "Validate that the PassTicket in Authorization header is valid", tags = { "Test Operations" })
    public void passticketTest(@RequestHeader("authorization") String authorization) throws IRRPassTicketEvaluationException {
        if (authorization != null && authorization.toLowerCase().startsWith("basic")) {
            String base64Credentials = authorization.substring("Basic".length()).trim();
            String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
            String[] values = credentials.split(":", 2);
            String userId = values[0];
            String passTicket = values[1];
            passTicketService.evaluate(userId, applId, passTicket);
            return;
        }
        throw new IllegalArgumentException("Missing Basic authorization header");
    }
}
