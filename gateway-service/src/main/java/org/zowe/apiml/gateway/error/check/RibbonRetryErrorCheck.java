/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.error.check;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.zowe.apiml.gateway.error.ErrorUtils;
import org.zowe.apiml.gateway.ribbon.http.RequestAbortException;
import org.zowe.apiml.gateway.ribbon.http.RequestContextNotPreparedException;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;

import javax.servlet.http.HttpServletRequest;
import java.net.ConnectException;
import java.util.stream.Collectors;

/**
 * Handler for exceptions that arise during the Ribbon retries
 */
@RequiredArgsConstructor
public class RibbonRetryErrorCheck implements ErrorCheck {
    private final MessageService messageService;
    @Override
    public ResponseEntity<ApiMessageView> checkError(HttpServletRequest request, Throwable exc) {

        int exceptionIndex = -1;

        if ( (exceptionIndex = ExceptionUtils.indexOfType(exc, RequestAbortException.class)) != -1) {
            Throwable t = ExceptionUtils.getThrowables(exc)[exceptionIndex];
            if (t instanceof RequestContextNotPreparedException) {
                ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.gateway.contextNotPrepared").mapToView();
                return getApiMessageViewResponseEntity(messageView);
            }

            if (t instanceof RequestAbortException) {
                ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.gateway.requestAborted",
                    ErrorUtils.getGatewayUri(request),
                    ExceptionUtils.getThrowableList(t).stream().map(Throwable::toString).collect(Collectors.joining(", Caused by: "))
                    ).mapToView();

                return getApiMessageViewResponseEntity(messageView);
            }

        }

        if ( (exceptionIndex = ExceptionUtils.indexOfType(exc, ConnectException.class)) != -1) {
            ConnectException t = (ConnectException) ExceptionUtils.getThrowables(exc)[exceptionIndex];
            ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.gateway.connectionRefused",
                ErrorUtils.getGatewayUri(request),
                t.getCause()).mapToView();
            return getApiMessageViewResponseEntity(messageView);
        }

        return null;
    }

    private ResponseEntity<ApiMessageView> getApiMessageViewResponseEntity(ApiMessageView messageView) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .body(messageView);
    }

}
