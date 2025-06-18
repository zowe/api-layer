/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.product.gateway.GatewayNotAvailableException;

import java.util.function.BiConsumer;

/**
 * Exception handler that deals with exceptions related to accessing other services/resources
 */
@Slf4j
@Component
public class ResourceAccessExceptionHandler extends AbstractExceptionHandler {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public ResourceAccessExceptionHandler(MessageService messageService, ObjectMapper mapper) {
        super(messageService, mapper);
    }

    /**
     * Entry method that takes care of an exception passed to it
     *
     * @param requestUri Http request URI
     * @param addHeader Consumer to add response headers. This implementation does not use it
     * @param ex         Exception to be handled
     * @throws RuntimeException Fallback exception if exception cannot be handled
     */
    @Override
    public void handleException(String requestUri, BiConsumer<ApiMessageView, HttpStatus> function, BiConsumer<String, String> addHeader, RuntimeException ex) {
        ErrorType errorType;
        if (ex instanceof GatewayNotAvailableException) {
            errorType = ErrorType.GATEWAY_NOT_AVAILABLE;
        } else if (ex instanceof ServiceNotAccessibleException) {
            errorType = ErrorType.SERVICE_UNAVAILABLE;
        } else {
            throw ex;
        }

        log.debug(MESSAGE_FORMAT, HttpStatus.SERVICE_UNAVAILABLE.value(), ex.getMessage());
        writeErrorResponse(errorType.getErrorMessageKey(), HttpStatus.SERVICE_UNAVAILABLE, function, requestUri);
    }

}
