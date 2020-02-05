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

import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import com.netflix.zuul.exception.ZuulException;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.SocketTimeoutException;

import javax.servlet.http.HttpServletRequest;

/**
 * Checks whether the error was caused by timeout (service not responding).
 */
@RequiredArgsConstructor
public class TimeoutErrorCheck implements ErrorCheck {
    public static final String DEFAULT_MESSAGE = "The service did not respond in time";
    private static final String ERROR_CAUSE_TIMEOUT = "TIMEOUT";

    private final MessageService messageService;

    public ResponseEntity<ApiMessageView> checkError(HttpServletRequest request, Throwable exc) {
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
                    causeMessage = DEFAULT_MESSAGE;
                }
                return gatewayTimeoutResponse(causeMessage);
            }

            else if (rootCause instanceof SocketTimeoutException) {
                return gatewayTimeoutResponse(rootCause.getMessage());
            }
        }

        return null;
    }

    private ResponseEntity<ApiMessageView> gatewayTimeoutResponse(String messageText) {
        Message message = messageService.createMessage("org.zowe.apiml.common.serviceTimeout", messageText);
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(message.mapToView());
    }
}
