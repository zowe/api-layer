/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.error.check;

import com.ca.apiml.security.common.token.TokenExpireException;
import com.ca.apiml.security.common.token.TokenNotValidException;
import com.ca.mfaas.message.api.ApiMessageView;
import com.ca.mfaas.message.core.MessageService;
import com.netflix.zuul.exception.ZuulException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;

import javax.servlet.http.HttpServletRequest;

/**
 * Checks whether the error was caused by an invalid token
 */
@RequiredArgsConstructor
public class SecurityTokenErrorCheck implements ErrorCheck {
    private final MessageService messageService;

    /**
     * Validate whether the exception is related to token and sets the proper response and status code
     *
     * @param request Http request
     * @param exc Exception thrown
     * @return Response entity with appropriate response and status code
     */
    @Override
    public ResponseEntity<ApiMessageView> checkError(HttpServletRequest request, Throwable exc) {
        if (exc instanceof ZuulException && (exc.getCause() instanceof AuthenticationException)) {
            ApiMessageView messageView = null;
            Throwable cause = exc.getCause();
            if (cause instanceof TokenExpireException) {
                messageView = messageService.createMessage("apiml.gateway.security.expiredToken").mapToView();
            } else if (cause instanceof TokenNotValidException) {
                messageView = messageService.createMessage("apiml.gateway.security.invalidToken").mapToView();
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON_UTF8).body(messageView);
        }

        return null;
    }
}
