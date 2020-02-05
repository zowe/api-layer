/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.ticket;

import org.zowe.apiml.security.common.ticket.TicketRequest;
import org.zowe.apiml.security.common.ticket.TicketResponse;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Handles the successful /ticket request
 */
@Component
@RequiredArgsConstructor
public class SuccessfulTicketHandler implements AuthenticationSuccessHandler {
    private final ObjectMapper mapper;
    private final PassTicketService passTicketService;
    private final MessageService messageService;

    /**
     * Set the ticket response containing PassTicket
     *
     * @param request        the http request
     * @param response       the http response
     * @param authentication the successful authentication
     */
    @SneakyThrows
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);

        try {
            response.setStatus(HttpStatus.OK.value());
            mapper.writeValue(response.getWriter(), getTicketResponse(request, authentication));
        } catch (ApplicationNameNotFoundException e) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.security.ticket.invalidApplicationName").mapToView();
            mapper.writeValue(response.getWriter(), messageView);
        } catch (IRRPassTicketGenerationException e) {
            response.setStatus(e.getHttpStatus());
            ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.security.ticket.generateFailed",
                e.getErrorCode().getMessage()).mapToView();
            mapper.writeValue(response.getWriter(), messageView);
        }

        response.getWriter().flush();
        if (!response.isCommitted()) {
            throw new IOException("Authentication response has not been committed.");
        }
    }

    private TicketResponse getTicketResponse(HttpServletRequest request, Authentication authentication) throws ApplicationNameNotFoundException, IRRPassTicketGenerationException {
        TokenAuthentication tokenAuthentication = (TokenAuthentication) authentication;
        String userId = tokenAuthentication.getPrincipal();

        String applicationName;
        try {
            applicationName = mapper.readValue(request.getInputStream(), TicketRequest.class).getApplicationName();
        } catch (IOException e) {
            throw new ApplicationNameNotFoundException("Ticket object has wrong format.");
        }

        if (StringUtils.isBlank(applicationName)) {
            throw new ApplicationNameNotFoundException("ApplicationName not provided.");
        }

        String ticket = passTicketService.generate(userId, applicationName);

        return new TicketResponse(tokenAuthentication.getCredentials(), userId, applicationName, ticket);
    }
}
