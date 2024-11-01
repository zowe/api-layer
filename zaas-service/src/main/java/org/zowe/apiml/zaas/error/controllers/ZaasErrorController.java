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

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.zaas.error.ErrorUtils;

import static org.apache.hc.core5.http.HttpStatus.*;

/**
 * Handles errors in REST API processing.
 */
@RestController
@Order(Ordered.HIGHEST_PRECEDENCE)
@Primary
@RequiredArgsConstructor
public class ZaasErrorController implements ErrorController {

    private static final String NOT_FOUND_ENDPOINT = "/not_found";
    public static final String ERROR_ENDPOINT = "/internal_error";

    private final MessageService messageService;

    /**
     * Not found endpoint controller
     * Creates response and logs the error
     *
     * @param request Http request
     * @return Http response entity
     */
    @GetMapping(value = NOT_FOUND_ENDPOINT, produces = "application/json")
    public ResponseEntity<ApiMessageView> notFound404HttpResponse(HttpServletRequest request) {
        Message message = messageService.createMessage("org.zowe.apiml.common.endPointNotFound",
            ErrorUtils.getForwardUri(request));
        return ResponseEntity.status(SC_NOT_FOUND).body(message.mapToView());
    }
    /**
     * Error endpoint controller
     * Creates response and logs the error
     *
     * @param request Http request
     * @return Http response entity
     */
    @SuppressWarnings("squid:S3752")
    @RequestMapping(value = ERROR_ENDPOINT, produces = "application/json")
    public ResponseEntity<ApiMessageView> error(HttpServletRequest request) {
        final Throwable exc = (Throwable) request.getAttribute(ErrorUtils.ATTR_ERROR_EXCEPTION);
        Message message = messageService.createMessage("org.zowe.apiml.common.internalRequestError",
            ErrorUtils.getForwardUri(request),
            ExceptionUtils.getMessage(exc),
            ExceptionUtils.getRootCauseMessage(exc));

        return ResponseEntity.status(SC_INTERNAL_SERVER_ERROR).body(message.mapToView());
    }

}
