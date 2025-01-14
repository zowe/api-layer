/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.error.controllers;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.zaas.error.ErrorUtils;

import java.util.Optional;

import static org.apache.hc.core5.http.HttpStatus.*;

/**
 * Handles errors in REST API processing.
 */
@RestController
@Order(Ordered.HIGHEST_PRECEDENCE)
@Primary
@RequiredArgsConstructor
public class ZaasErrorController implements ErrorController {

    private static final String ERROR_ENDPOINT = "/error";
    private static final String NOT_FOUND_ENDPOINT = "/not_found";
    public static final String INTERNAL_ERROR_ENDPOINT = "/internal_error";

    private final MessageService messageService;

    private Message getMessageByStatus(HttpServletRequest request, int status) {
        switch (status) {
            case SC_BAD_REQUEST:
                return messageService.createMessage("org.zowe.apiml.common.badRequest", request.getRequestURI());
            case SC_NOT_FOUND:
                return messageService.createMessage("org.zowe.apiml.common.endPointNotFound", ErrorUtils.getForwardUri(request));
            case SC_INTERNAL_SERVER_ERROR:
                final Throwable exc = (Throwable) request.getAttribute(ErrorUtils.ATTR_ERROR_EXCEPTION);
                return messageService.createMessage("org.zowe.apiml.common.internalRequestError",
                    ErrorUtils.getForwardUri(request),
                    ExceptionUtils.getMessage(exc),
                    ExceptionUtils.getRootCauseMessage(exc));
            default:
                return getMessageByStatus(request, SC_INTERNAL_SERVER_ERROR);
        }
    }

    private ApiMessageView getBodyByStatus(HttpServletRequest request, int status) {
        var message = getMessageByStatus(request, status);
        return message == null ? null : message.mapToView();
    }

    /**
     * Not found endpoint controller
     * Creates response and logs the error
     *
     * @param request Http request
     * @return Http response entity
     */
    @GetMapping(value = NOT_FOUND_ENDPOINT, produces = "application/json")
    public ResponseEntity<ApiMessageView> notFound404HttpResponse(HttpServletRequest request) {
        return ResponseEntity.status(SC_NOT_FOUND).body(getBodyByStatus(request, SC_NOT_FOUND));
    }
    /**
     * Error endpoint controller
     * Creates response and logs the error
     *
     * @param request Http request
     * @return Http response entity
     */
    @SuppressWarnings("squid:S3752")
    @RequestMapping(value = INTERNAL_ERROR_ENDPOINT, produces = "application/json")
    public ResponseEntity<ApiMessageView> internalError(HttpServletRequest request) {
        return ResponseEntity.status(SC_INTERNAL_SERVER_ERROR).body(getBodyByStatus(request, SC_INTERNAL_SERVER_ERROR));
    }

    private int getStatus(HttpServletRequest request) {
        return Optional
            .ofNullable((Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE))
            .orElse(SC_INTERNAL_SERVER_ERROR);
    }

    @SuppressWarnings("squid:S3752")
    @RequestMapping(value = ERROR_ENDPOINT, produces = "application/json")
    public ResponseEntity<ApiMessageView> error(HttpServletRequest request) {
        int status = getStatus(request);
        if (status == SC_NO_CONTENT) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return ResponseEntity.status(status).body(getBodyByStatus(request, status));
    }

}
