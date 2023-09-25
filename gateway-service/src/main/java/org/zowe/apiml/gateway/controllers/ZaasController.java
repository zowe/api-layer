package org.zowe.apiml.gateway.controllers;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.zowe.apiml.gateway.security.ticket.ApplicationNameNotFoundException;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.ticket.TicketRequest;
import org.zowe.apiml.ticket.TicketResponse;

@RequiredArgsConstructor
@RestController
@RequestMapping(ZaasController.CONTROLLER_PATH)
@Slf4j
public class ZaasController {
    public static final String CONTROLLER_PATH = "gateway/zaas";

    private final PassTicketService passTicketService;

    @PostMapping(path = "ticket", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Provides PassTicket for authenticated user.")
    @ResponseBody
    public TicketResponse getPassTicket(@RequestBody TicketRequest ticketRequest, Authentication authentication) throws ApplicationNameNotFoundException, IRRPassTicketGenerationException {
        TokenAuthentication tokenAuthentication = (TokenAuthentication) authentication;
        final String userId = tokenAuthentication.getPrincipal();

        final String applicationName = ticketRequest.getApplicationName();
        if (StringUtils.isBlank(applicationName)) {
            throw new ApplicationNameNotFoundException("ApplicationName not provided.");
        }

        String ticket = passTicketService.generate(userId, applicationName);
        return new TicketResponse(tokenAuthentication.getCredentials(), userId, applicationName, ticket);
    }
}
