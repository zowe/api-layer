package io.apiml.security.service.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.apiml.security.common.message.ApiMessage;
import io.apiml.security.common.message.MessageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.UUID;

@Component
public class ServiceTokenQueryFailureHandler implements AuthenticationFailureHandler {
    private final MessageService messageService;
    private final ObjectMapper mapper;

    public ServiceTokenQueryFailureHandler(MessageService messageService, ObjectMapper objectMapper) {
        this.messageService = messageService;
        this.mapper = objectMapper;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        String traceId = UUID.randomUUID().toString();
        ApiMessage message = messageService.createError(HttpStatus.UNAUTHORIZED.value(), "SEC0009", exception.getMessage(), traceId);
        mapper.writeValue(response.getWriter(), message);
    }
}
