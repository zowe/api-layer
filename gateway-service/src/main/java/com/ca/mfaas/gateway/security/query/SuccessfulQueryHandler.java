/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.security.query;

import com.ca.mfaas.gateway.security.service.AuthenticationService;
import com.ca.apiml.security.token.TokenAuthentication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Handles the successful query
 */
@Component
public class SuccessfulQueryHandler implements AuthenticationSuccessHandler {
    private final ObjectMapper mapper;
    private final AuthenticationService authenticationService;

    /**
     * Constructor
     */
    public SuccessfulQueryHandler(ObjectMapper securityObjectMapper,
                                  AuthenticationService authenticationService) {
        this.mapper = securityObjectMapper;
        this.authenticationService = authenticationService;
    }

    /**
     * Set the query response with information about the JWT token
     *
     * @param request        the http request
     * @param response       the http response
     * @param authentication the successful authentication
     * @throws IOException when the response cannot be written
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
        throws IOException {
        TokenAuthentication tokenAuthentication = (TokenAuthentication) authentication;
        String token = tokenAuthentication.getCredentials();

        response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        response.setStatus(HttpStatus.OK.value());

        mapper.writeValue(response.getWriter(), authenticationService.parseJwtToken(token));

        response.getWriter().flush();
        if (!response.isCommitted()) {
            throw new IOException("Authentication response has not been committed.");
        }
    }
}
