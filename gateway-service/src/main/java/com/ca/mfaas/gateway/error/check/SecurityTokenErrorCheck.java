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

import com.ca.mfaas.error.ErrorService;
import com.ca.apiml.security.token.TokenNotValidException;
import com.ca.apiml.security.token.TokenExpireException;
import com.ca.mfaas.rest.response.ApiMessage;
import com.netflix.zuul.exception.ZuulException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;

import javax.servlet.http.HttpServletRequest;

/**
 * Checks whether the error was caused by an invalid token
 */
@Slf4j
@RequiredArgsConstructor
public class SecurityTokenErrorCheck implements ErrorCheck {
    private final ErrorService errorService;

    @Override
    public ResponseEntity<ApiMessage> checkError(HttpServletRequest request, Throwable exc) {
        if (exc instanceof ZuulException && (exc.getCause() instanceof AuthenticationException)) {
            ApiMessage message = null;
            Throwable cause = exc.getCause();
            if (cause instanceof TokenExpireException) {
                message = errorService.createApiMessage("apiml.gateway.security.expiredToken");
            } else if (cause instanceof TokenNotValidException) {
                message = errorService.createApiMessage("apiml.gateway.security.invalidToken");
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON_UTF8).body(message);
        }

        return null;
    }
}
