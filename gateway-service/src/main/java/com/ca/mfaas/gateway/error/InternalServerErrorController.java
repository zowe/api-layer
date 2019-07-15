/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.error;

import com.ca.mfaas.error.ErrorService;
import com.ca.mfaas.gateway.error.check.ErrorCheck;
import com.ca.mfaas.gateway.error.check.SecurityTokenErrorCheck;
import com.ca.mfaas.gateway.error.check.TimeoutErrorCheck;
import com.ca.mfaas.gateway.error.check.TlsErrorCheck;
import com.ca.mfaas.rest.response.ApiMessage;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

/**
 * Handles errors in REST API processing.
 */
@Slf4j
@Controller
@Order(Ordered.HIGHEST_PRECEDENCE)
@Primary
public class InternalServerErrorController implements ErrorController {
    private static final String ERROR_ENDPOINT = "/internal_error";

    private final ErrorService errorService;
    private final List<ErrorCheck> errorChecks = new ArrayList<>();

    @Autowired
    public InternalServerErrorController(ErrorService errorService) {
        this.errorService = errorService;
        errorChecks.add(new TlsErrorCheck(errorService));
        errorChecks.add(new TimeoutErrorCheck(errorService));
        errorChecks.add(new SecurityTokenErrorCheck(errorService));
    }

    @Override
    public String getErrorPath() {
        return ERROR_ENDPOINT;
    }

    @RequestMapping(value = ERROR_ENDPOINT, produces = "application/json")
    @ResponseBody
    public ResponseEntity<ApiMessage> error(HttpServletRequest request) {
        final Throwable exc = (Throwable) request.getAttribute(ErrorUtils.ATTR_ERROR_EXCEPTION);

        ResponseEntity<ApiMessage> entity = checkForSpecificErrors(request, exc);
        if (entity != null) {
            return entity;
        }

        return logAndCreateResponseForInternalError(request, exc);
    }

    private ResponseEntity<ApiMessage> logAndCreateResponseForInternalError(HttpServletRequest request, Throwable exc) {
        final int status = ErrorUtils.getErrorStatus(request);
        final String errorMessage = ErrorUtils.getErrorMessage(request);
        ApiMessage message = errorService.createApiMessage("apiml.common.internalRequestError", ErrorUtils.getGatewayUri(request),
            ExceptionUtils.getMessage(exc), ExceptionUtils.getRootCauseMessage(exc));
        log.error("Unresolved request error: {}", errorMessage, exc);
        return ResponseEntity.status(status).body(message);
    }

    private ResponseEntity<ApiMessage> checkForSpecificErrors(HttpServletRequest request, Throwable exc) {
        for (ErrorCheck check : errorChecks) {
            ResponseEntity<ApiMessage> entity = check.checkError(request, exc);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }
}
