package com.ca.mfaas.gateway.security.controller;

import com.ca.mfaas.gateway.security.controller.exception.QueryRequestException;
import com.ca.mfaas.gateway.security.token.TokenExpireException;
import com.ca.mfaas.gateway.security.token.TokenNotValidException;
import io.apiml.security.common.message.ApiMessage;
import io.apiml.security.common.message.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.UUID;

@Slf4j
@ControllerAdvice
public class QueryControllerExceptionHandler {
    private final MessageService messageService;

    public QueryControllerExceptionHandler(MessageService messageService) {
        this.messageService = messageService;
    }

    @ExceptionHandler(QueryRequestException.class)
    public ResponseEntity<ApiMessage> handleQueryRequestException(QueryRequestException exception) {
        String traceId = UUID.randomUUID().toString();
        log.debug("Authentication: Query request is not valid, trace id is {}", traceId);
        ApiMessage message = messageService.createError(HttpStatus.BAD_REQUEST.value(), "SEC0004", exception.getMessage(), traceId);

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(message);
    }

    @ExceptionHandler(value = {TokenExpireException.class, TokenNotValidException.class})
    public ResponseEntity<ApiMessage> handleQueryRequestException(RuntimeException exception) {
        String traceId = UUID.randomUUID().toString();
        log.debug("Authentication: Token is not valid, trace id is {}", traceId);
        ApiMessage message = messageService.createError(HttpStatus.UNAUTHORIZED.value(), "SEC0004", exception.getMessage(), traceId);

        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(message);
    }
}
