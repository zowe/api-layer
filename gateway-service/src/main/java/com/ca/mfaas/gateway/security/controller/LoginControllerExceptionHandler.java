package com.ca.mfaas.gateway.security.controller;

import com.ca.mfaas.gateway.security.controller.exception.GatewayLoginRequestFormatException;
import com.ca.mfaas.gateway.security.controller.exception.IncorrectUsernameOrPasswordException;
import io.apiml.security.common.message.ApiMessage;
import io.apiml.security.common.message.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.UUID;

@Slf4j
@ControllerAdvice
public class LoginControllerExceptionHandler extends ResponseEntityExceptionHandler {
    private final MessageService messageService;

    public LoginControllerExceptionHandler(MessageService messageService) {
        this.messageService = messageService;
    }

    @ExceptionHandler(GatewayLoginRequestFormatException.class)
    public ResponseEntity<ApiMessage> handleWrongLoginFormat(GatewayLoginRequestFormatException exception) {
        String traceId = UUID.randomUUID().toString();
        log.debug("Authentication: Request format is wrong, trace id is {}", traceId);
        ApiMessage message = messageService.createError(HttpStatus.BAD_REQUEST.value(), "SEC0001", exception.getMessage(), traceId);
        HttpHeaders headers = new HttpHeaders();
        headers.add("WWW-Authenticate", "Basic realm=\"Gateway Authentication\"");

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .headers(headers)
            .body(message);
    }

    @ExceptionHandler(IncorrectUsernameOrPasswordException.class)
    public ResponseEntity<ApiMessage> handleIncorrectCredentials(IncorrectUsernameOrPasswordException exception) {
        String traceId = UUID.randomUUID().toString();
        log.debug("Authentication: Username or password is incorrect, trace id is {}", traceId);
        ApiMessage message = messageService.createError(HttpStatus.UNAUTHORIZED.value(), "SEC0002", exception.getMessage(), traceId);
        HttpHeaders headers = new HttpHeaders();
        headers.add("WWW-Authenticate", "Basic realm=\"Gateway Authentication\"");

        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .headers(headers)
            .body(message);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        String traceId = UUID.randomUUID().toString();
        log.debug("Authentication: HTTP method for login is incorrect, trace id is {}", traceId);
        ApiMessage message = messageService.createError(HttpStatus.METHOD_NOT_ALLOWED.value(), "SEC0003", ex.getMessage(), traceId);

        return ResponseEntity
            .status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(message);
    }
}
