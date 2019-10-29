/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.apiml.security.common.error;

import com.ca.mfaas.message.api.ApiMessageView;
import com.ca.mfaas.message.core.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Base class for exception handlers
 * Aggregates boilerplate and constants, which are reused by concrete classes
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractExceptionHandler {
    protected static final String ERROR_MESSAGE_400 = "400 Status Code: {}";
    protected static final String ERROR_MESSAGE_500 = "500 Status Code: {}";
    private static final String CONTENT_TYPE = MediaType.APPLICATION_JSON_UTF8_VALUE;

    protected final MessageService messageService;
    protected final ObjectMapper mapper;

    /**
     * Entry method that takes care of an exception passed to it
     *
     * @param request  Http request
     * @param response Http response
     * @param ex       Exception to be handled
     * @throws ServletException Fallback exception if exception cannot be handled
     */
    public abstract void handleException(HttpServletRequest request, HttpServletResponse response, RuntimeException ex) throws ServletException;

    /**
     * Write message (by message key) to http response
     * Error service resolves the message, see {@link MessageService}
     *
     * @param messageKey Message key
     * @param status     Http response status
     * @param request    Http request
     * @param response   Http response
     * @throws ServletException throws when a message cannot be written to response
     */
    protected void writeErrorResponse(String messageKey, HttpStatus status, HttpServletRequest request, HttpServletResponse response) throws ServletException {
        final ApiMessageView message = messageService.createMessage(messageKey, request.getRequestURI()).mapToView();
        writeErrorResponse(message, status, response);
    }

    /**
     * Write a message to http response
     *
     * @param message  Message object, which contains message key, type, number and text
     * @param status   Http response status
     * @param response Http response
     * @throws ServletException thrown when message cannot be written to response
     */
    protected void writeErrorResponse(ApiMessageView message, HttpStatus status, HttpServletResponse response) throws ServletException {
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
