/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.apiml.security.error;

import com.ca.mfaas.error.ErrorService;
import com.ca.mfaas.rest.response.ApiMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
public abstract class AbstractExceptionHandler {
    protected static final String ERROR_MESSAGE_400 = "400 Status Code: {}";
    protected static final String ERROR_MESSAGE_500 = "500 Status Code: {}";
    private static final String CONTENT_TYPE = MediaType.APPLICATION_JSON_UTF8_VALUE;

    protected final ErrorService errorService;
    protected final ObjectMapper mapper;

    public AbstractExceptionHandler(ErrorService errorService, ObjectMapper mapper) {
        this.errorService = errorService;
        this.mapper = mapper;
    }

    public abstract void handleException(HttpServletRequest request, HttpServletResponse response, RuntimeException ex) throws ServletException;

    protected void writeErrorResponse(String messageKey, HttpStatus status, HttpServletRequest request, HttpServletResponse response) throws ServletException {
        final ApiMessage message = errorService.createApiMessage(messageKey, request.getRequestURI());
        writeErrorResponse(message, status, response);
    }

    protected void writeErrorResponse(ApiMessage message, HttpStatus status, HttpServletResponse response) throws ServletException {
        response.setStatus(status.value());
        response.setContentType(CONTENT_TYPE);
        try {
            mapper.writeValue(response.getWriter(), message);
        } catch (IOException e) {
            String errorMessage = "Couldn't write response";
            log.error(errorMessage, e);
            throw new ServletException(errorMessage, e);
        }
    }
}
