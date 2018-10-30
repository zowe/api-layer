/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.config.error;

import com.ca.mfaas.error.ErrorService;
import com.ca.mfaas.rest.response.ApiMessage;
import com.netflix.zuul.exception.ZuulException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

import java.net.SocketTimeoutException;

/**
 * Handles errors in REST API processing.
 */
@Slf4j
@Controller
@Order(Ordered.HIGHEST_PRECEDENCE)
@Primary
public class InternalServerErrorController implements ErrorController {

    @SuppressWarnings("squid:S1075")
    private static final String PATH = "/internal_error";
    private static final String ERROR_CAUSE_TIMEOUT = "TIMEOUT";

    private final ErrorService errorService;

    @Autowired
    public InternalServerErrorController(ErrorService errorService) {
        this.errorService = errorService;
    }

    @Override
    public String getErrorPath() {
        return PATH;
    }

    @RequestMapping(value = PATH, produces = "application/json")
    public @ResponseBody
    ResponseEntity<ApiMessage> error(HttpServletRequest request) {
        final int status = getErrorStatus(request);
        final String errorMessage = getErrorMessage(request);
        final Throwable exc = (Throwable) request.getAttribute("javax.servlet.error.exception");

        // Check for a specific error cause:
        ResponseEntity<ApiMessage> entity = checkTimeoutError(exc);
        if (entity != null) {
            return entity;
        }

        // Fallback for unexpected internal errors
        ApiMessage message = errorService.createApiMessage("com.ca.mfaas.common.endPointNotFound", getErrorURI(request));
        log.error(errorMessage, exc);
        return ResponseEntity.status(status).body(message);
    }

    private ResponseEntity<ApiMessage> gatewayTimeoutResponse(String message) {
        ApiMessage apiMessage = errorService.createApiMessage("com.ca.mfaas.common.serviceTimeout", message);
        log.error("MFSG0002 Timeout error: " + message);
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(apiMessage);
    }

    /**
     * Check whether the error was caused by timeout (service not responding).
     */
    private ResponseEntity<ApiMessage> checkTimeoutError(Throwable exc) {
        if (exc instanceof ZuulException) {
            ZuulException zuulException = (ZuulException) exc;
            Throwable rootCause = ExceptionUtils.getRootCause(zuulException);

            if ((zuulException.nStatusCode == HttpStatus.GATEWAY_TIMEOUT.value())
                    || zuulException.errorCause.equals(ERROR_CAUSE_TIMEOUT)) {
                Throwable cause = zuulException.getCause();
                String causeMessage;
                if (cause != null) {
                    causeMessage = cause.getMessage();
                } else {
                    causeMessage = "The service did not respond in time";
                }
                return gatewayTimeoutResponse(causeMessage);
            }

            else if (rootCause instanceof SocketTimeoutException) {
                return gatewayTimeoutResponse(rootCause.getMessage());
            }
        }

        return null;
    }

    private int getErrorStatus(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        return statusCode != null ? statusCode : HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    private String getErrorMessage(HttpServletRequest request) {
        final Throwable exc = (Throwable) request.getAttribute("javax.servlet.error.exception");
        return exc != null ? exc.getMessage() : "Unexpected error occurred";
    }

    private String getErrorURI(HttpServletRequest request) {
        return (String) request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
    }
}
