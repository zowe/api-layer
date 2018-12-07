package io.apiml.security.service.login.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.apiml.security.common.message.ApiMessage;
import io.apiml.security.common.message.MessageService;
import io.apiml.security.service.login.exception.ServiceLoginRequestFormatException;
import io.apiml.security.service.login.exception.WrongLoginMethodException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.UUID;

@Component
public class ServiceLoginFailureHandler implements AuthenticationFailureHandler {
    private final MessageService messageService;
    private final ObjectMapper mapper;

    public ServiceLoginFailureHandler(MessageService messageService, ObjectMapper objectMapper) {
        this.messageService = messageService;
        this.mapper = objectMapper;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        ApiMessage message = null;
        if (exception instanceof WrongLoginMethodException) {
            String traceId = UUID.randomUUID().toString();
            message = messageService.createError(HttpStatus.UNAUTHORIZED.value(), "SEC0005", exception.getMessage(), traceId);
        } else if (exception instanceof ServiceLoginRequestFormatException) {
            String traceId = UUID.randomUUID().toString();
            message = messageService.createError(HttpStatus.UNAUTHORIZED.value(), "SEC0006", exception.getMessage(), traceId);
        } else if (exception instanceof BadCredentialsException) {
            String traceId = UUID.randomUUID().toString();
            message = messageService.createError(HttpStatus.UNAUTHORIZED.value(), "SEC0007", exception.getMessage(), traceId);
        } else {
            String traceId = UUID.randomUUID().toString();
            message = messageService.createError(HttpStatus.UNAUTHORIZED.value(), "SEC0008", exception.getMessage(), traceId);
        }
        mapper.writeValue(response.getWriter(), message);
    }
}
