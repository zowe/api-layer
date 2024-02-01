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

import com.netflix.zuul.exception.ZuulException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.zowe.apiml.gateway.error.ErrorUtils;
import org.zowe.apiml.gateway.security.service.PassTicketException;
import org.zowe.apiml.gateway.security.service.saf.SafIdtAuthException;
import org.zowe.apiml.gateway.security.service.saf.SafIdtException;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.security.common.token.TokenNotValidException;

/**
 * Checks whether the error was caused by an invalid token or credentials.
 */
@RequiredArgsConstructor
public class SecurityErrorCheck implements ErrorCheck {
    private final MessageService messageService;

    /**
     * Validate whether the exception is related to token and sets the proper response and status code
     *
     * @param request Http request
     * @param exc     Exception thrown
     * @return Response entity with appropriate response and status code
     */
    @Override
    public ResponseEntity<ApiMessageView> checkError(HttpServletRequest request, Throwable exc) {
        if (!(exc instanceof ZuulException)) return null;

        Throwable cause = exc.getCause();
        ApiMessageView messageView = null;

        if (cause instanceof AuthenticationException) {
            HttpStatus status = HttpStatus.UNAUTHORIZED;

            if (cause instanceof TokenExpireException) {
                messageView = messageService.createMessage("org.zowe.apiml.gateway.security.expiredToken").mapToView();
            } else if (cause instanceof TokenNotValidException) {
                messageView = messageService.createMessage("org.zowe.apiml.gateway.security.invalidToken").mapToView();
            } else if (cause instanceof BadCredentialsException) {
                messageView = messageService.createMessage("org.zowe.apiml.security.login.invalidCredentials",
                        ErrorUtils.getGatewayUri(request)
                ).mapToView();
            } else if (cause instanceof SafIdtAuthException) {
                messageView = messageService.createMessage("org.zowe.apiml.security.idt.auth.failed",
                        cause.getLocalizedMessage() + ". " + getPreviousCause(cause)
                ).mapToView();
            }

            return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(messageView);
        }

        if (cause instanceof AccessDeniedException) {
            HttpStatus status = HttpStatus.FORBIDDEN;

            if (cause instanceof SafIdtException) {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
                messageView = messageService.createMessage("org.zowe.apiml.security.idt.failed",
                        cause.getLocalizedMessage() + ". " + getPreviousCause(cause)
                ).mapToView();
            } else if (cause instanceof PassTicketException) {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
                messageView = messageService.createMessage("org.zowe.apiml.security.ticket.generateFailed",
                    cause.getLocalizedMessage() + ". " + getPreviousCause(cause)
                ).mapToView();
            }

            return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(messageView);
        }

        return null;
    }

    private static String getPreviousCause(Throwable cause) {
        Throwable t = cause.getCause();
        return t == null ? "" : t.getMessage();
    }

}
