/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.security.handler;

import com.broadcom.apiml.library.response.ApiMessage;
import com.broadcom.apiml.library.security.login.InvalidUserException;
import com.broadcom.apiml.library.security.token.TokenExpireException;
import com.broadcom.apiml.library.security.token.TokenNotValidException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.UUID;

@Component
public class FailedAuthenticationHandler implements AuthenticationFailureHandler {
    private final ObjectMapper mapper;

    public FailedAuthenticationHandler(@Qualifier("securityObjectMapper") ObjectMapper objectMapper) {
        this.mapper = objectMapper;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        ApiMessage message = null;
        if (exception instanceof TokenExpireException) {
            message = new ApiMessage(401, exception.getMessage(), "com.broadcom.apiml.security.tokenIsExpired", UUID.randomUUID());
        } else if (exception instanceof TokenNotValidException) {
            message = new ApiMessage(401, exception.getMessage(), "com.broadcom.apiml.security.tokenIsNotValid", UUID.randomUUID());
        } else if (exception instanceof InvalidUserException) {
            message = new ApiMessage(401, exception.getMessage(), "com.broadcom.apiml.security.invalidUsername", UUID.randomUUID());
        } else {
            message = new ApiMessage(401, exception.getMessage(), "com.broadcom.apiml.security.authenticationException", UUID.randomUUID());
        }
        mapper.writeValue(response.getWriter(), message);
    }
}
