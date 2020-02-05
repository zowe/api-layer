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

import org.zowe.apiml.gateway.error.ErrorUtils;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;
import com.netflix.zuul.exception.ZuulException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.net.ssl.SSLException;
import javax.servlet.http.HttpServletRequest;

/**
 * Checks whether the error was caused by timeout (service not responding).
 */
@Slf4j
@RequiredArgsConstructor
public class TlsErrorCheck implements ErrorCheck {
    private final MessageService messageService;

    public ResponseEntity<ApiMessageView> checkError(HttpServletRequest request, Throwable exc) {
        if (exc instanceof ZuulException) {
            int exceptionIndex = ExceptionUtils.indexOfType(exc, SSLException.class);
            if (exceptionIndex != -1) {
                Throwable sslException = ExceptionUtils.getThrowables(exc)[exceptionIndex];
                log.debug("TLS request error: {}", sslException.getMessage(), sslException);
                return tlsErrorResponse(request, sslException.getMessage());
            }
        }

        return null;
    }

    private ResponseEntity<ApiMessageView> tlsErrorResponse(HttpServletRequest request, String message) {
        ApiMessageView apiMessage = messageService.createMessage("org.zowe.apiml.common.tlsError", ErrorUtils.getGatewayUri(request),
                message).mapToView();
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(apiMessage);
    }
}
