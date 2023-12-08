/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.zaas;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.zowe.apiml.gateway.security.service.saf.SafIdtAuthException;
import org.zowe.apiml.gateway.security.service.saf.SafIdtException;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSchemeException;
import org.zowe.apiml.gateway.security.ticket.ApplicationNameNotFoundException;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import javax.management.ServiceNotFoundException;

@ControllerAdvice
@RequiredArgsConstructor
public class ZaasExceptionHandler {
    private final MessageService messageService;

    @ExceptionHandler(value = {IRRPassTicketGenerationException.class})
    public ResponseEntity<ApiMessageView> handlePassTicketException(IRRPassTicketGenerationException ex) {
        ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.security.ticket.generateFailed",
            ex.getErrorCode().getMessage()).mapToView();
        return ResponseEntity
            .status(ex.getHttpStatus())
            .contentType(MediaType.APPLICATION_JSON)
            .body(messageView);
    }

    @ExceptionHandler(value = {SafIdtException.class, SafIdtAuthException.class})
    public ResponseEntity<ApiMessageView> handleSafIdtExceptions(RuntimeException ex) {
        ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.security.idt.failed", ex.getMessage()).mapToView();
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .body(messageView);
    }

    @ExceptionHandler(value = {ApplicationNameNotFoundException.class})
    public ResponseEntity<ApiMessageView> handleApplIdNotFoundException(ApplicationNameNotFoundException ex) {
        ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.security.ticket.invalidApplicationName").mapToView();
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON)
            .body(messageView);
    }

    @ExceptionHandler(value = {ServiceNotFoundException.class})
    public ResponseEntity<ApiMessageView> handleServiceNotFoundException(ServiceNotFoundException ex) {
        ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.zaas.zosmf.noZosmfTokenReceived", ex.getMessage()).mapToView();
        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .contentType(MediaType.APPLICATION_JSON)
            .body(messageView);
    }

    @ExceptionHandler(value = {IllegalStateException.class})
    public ResponseEntity<ApiMessageView> handleZoweJwtCreationErrors(IllegalStateException ex) {
        ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.zaas.zoweJwt.noToken", ex.getMessage()).mapToView();
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .body(messageView);
    }

    @ExceptionHandler(value = {TokenNotValidException.class, AuthSchemeException.class})
    public ResponseEntity<ApiMessageView> handleTokenNotValidException(RuntimeException ex) {
        ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.gateway.security.invalidToken").mapToView();
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .contentType(MediaType.APPLICATION_JSON)
            .body(messageView);
    }

    @ExceptionHandler(value = {TokenExpireException.class})
    public ResponseEntity<ApiMessageView> handleTokenExpiredException(TokenExpireException ex) {
        ApiMessageView messageView = messageService.createMessage("org.zowe.apiml.gateway.security.expiredToken").mapToView();
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .contentType(MediaType.APPLICATION_JSON)
            .body(messageView);
    }
}
