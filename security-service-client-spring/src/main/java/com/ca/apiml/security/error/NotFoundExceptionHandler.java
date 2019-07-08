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
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@Component
public class NotFoundExceptionHandler extends AbstractExceptionHandler {

    public NotFoundExceptionHandler(ErrorService errorService, ObjectMapper mapper) {
        super(errorService, mapper);
    }

    @Override
    public void handleException(HttpServletRequest request, HttpServletResponse response, RuntimeException ex) throws ServletException {
        if (ex instanceof GatewayNotFoundException) {
            handleGatewayNotFound(request, response);
        } else {
            throw ex;
        }
    }

    private void handleGatewayNotFound(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        writeErrorResponse(ErrorType.GATEWAY_NOT_FOUND.getErrorMessageKey(), HttpStatus.NOT_FOUND, request, response);
    }
}
