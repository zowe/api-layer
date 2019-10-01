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

import com.ca.mfaas.gateway.error.check.ErrorCheck;
import com.ca.mfaas.gateway.error.check.SecurityTokenErrorCheck;
import com.ca.mfaas.gateway.error.check.TimeoutErrorCheck;
import com.ca.mfaas.gateway.error.check.TlsErrorCheck;
import com.ca.mfaas.message.api.ApiMessageView;
import com.ca.mfaas.message.core.Message;
import com.ca.mfaas.message.core.MessageService;
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

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles errors in REST API processing.
 */
@Slf4j
@Controller
@Order(Ordered.HIGHEST_PRECEDENCE)
@Primary
public class InternalServerErrorController implements ErrorController {
    private static final String ERROR_ENDPOINT = "/internal_error";

    private final MessageService messageService;
    private final List<ErrorCheck> errorChecks = new ArrayList<>();

    @Autowired
    public InternalServerErrorController(MessageService messageService) {
        this.messageService = messageService;
        errorChecks.add(new TlsErrorCheck(messageService));
        errorChecks.add(new TimeoutErrorCheck(messageService));
        errorChecks.add(new SecurityTokenErrorCheck(messageService));
    }

    @Override
    public String getErrorPath() {
        return ERROR_ENDPOINT;
    }

    /**
     * Error endpoint controller
     * Creates response and logs the error
     *
     * @param request Http request
     * @return Http response entity
     */
    @RequestMapping(value = ERROR_ENDPOINT, produces = "application/json")
    @ResponseBody
    public ResponseEntity<ApiMessageView> error(HttpServletRequest request) {
        final Throwable exc = (Throwable) request.getAttribute(ErrorUtils.ATTR_ERROR_EXCEPTION);

        ResponseEntity<ApiMessageView> entity = checkForSpecificErrors(request, exc);
        if (entity != null) {
            return entity;
        }

        return logAndCreateResponseForInternalError(request, exc);
    }

    private ResponseEntity<ApiMessageView> logAndCreateResponseForInternalError(HttpServletRequest request, Throwable exc) {
        final int status = ErrorUtils.getErrorStatus(request);
        final String errorMessage = ErrorUtils.getErrorMessage(request);
        Message message = messageService.createMessage("apiml.common.internalRequestError", ErrorUtils.getGatewayUri(request),
            ExceptionUtils.getMessage(exc), ExceptionUtils.getRootCauseMessage(exc));
        log.error("Unresolved request error: {}", errorMessage, exc);
        return ResponseEntity.status(status).body(message.mapToView());
    }

    private ResponseEntity<ApiMessageView> checkForSpecificErrors(HttpServletRequest request, Throwable exc) {
        for (ErrorCheck check : errorChecks) {
            ResponseEntity<ApiMessageView> entity = check.checkError(request, exc);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }
}
