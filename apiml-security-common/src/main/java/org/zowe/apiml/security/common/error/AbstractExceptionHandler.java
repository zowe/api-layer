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

import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.function.BiConsumer;

/**
 * Base class for exception handlers
 * Aggregates boilerplate and constants, which are reused by concrete classes
 */

@RequiredArgsConstructor
public abstract class AbstractExceptionHandler {
    protected static final String MESSAGE_FORMAT = "Status Code {}, error message: {}";

    protected final MessageService messageService;
    protected final ObjectMapper mapper;


    /**
     * Entry method that takes care of an exception passed to it
     *
     * @param requestUri Http request URI
     * @param addHeader  add response header
     * @param ex         Exception to be handled
     * @throws ServletException Fallback exception if exception cannot be handled
     */
    public abstract void handleException(String requestUri, BiConsumer<ApiMessageView, HttpStatus> function, BiConsumer<String, String> addHeader, RuntimeException ex);

    /**
     * Write message (by message key) to http response
     * Error service resolves the message, see {@link MessageService}
     *
     * @param messageKey Message key
     * @param status     Http response status
     * @param response   Update response with message and status
     */
    protected void writeErrorResponse(String messageKey, HttpStatus status, String requestUri,  BiConsumer<ApiMessageView, HttpStatus> response)  {
        final ApiMessageView message = messageService.createMessage(messageKey, requestUri, status.getReasonPhrase()).mapToView();
        response.accept(message, status);

    }

}
