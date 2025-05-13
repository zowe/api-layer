/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.security.common.error.AuthExceptionHandler;

import java.io.IOException;
import java.util.function.BiConsumer;

/**
 * Authentication error handler
 */
@Slf4j
@Component("failedAuthenticationHandler")
@Primary
@RequiredArgsConstructor
public class FailedAuthenticationHandler implements AuthenticationFailureHandler {
    private final AuthExceptionHandler handler;

    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();

    /**
     * Handles authentication failure by printing a debug message and passes control to {@link AuthExceptionHandler}
     *
     * @param request   the http request
     * @param response  the http response
     * @param exception to be checked
     * @throws ServletException when the response cannot be written
     */
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        var consumer = (BiConsumer<ApiMessageView, HttpStatus>) (apiMessageView, status) -> {
            response.setStatus(status.value());
            if(apiMessageView != null) {
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                try {
                    var mapper = new ObjectMapper();
                    mapper.writeValue(response.getWriter(), apiMessageView);
                } catch (IOException e) {
                    apimlLog.log("org.zowe.apiml.security.errorWritingResponse", e.getMessage());
                }
            }
        };

        var addHeader = (BiConsumer<String, String>) response::addHeader;
        handler.handleException(request.getRequestURI(), consumer, addHeader, exception);
    }
}
