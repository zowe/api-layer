/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package io.apiml.security.service.general;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.apiml.security.common.message.ApiMessage;
import io.apiml.security.common.message.MessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class UnauthorizedHandler implements AuthenticationEntryPoint {
    private final MessageService messageService;
    private final ObjectMapper mapper;

    public UnauthorizedHandler(MessageService messageService, ObjectMapper objectMapper) {
        this.messageService = messageService;
        this.mapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        log.debug("Unauthorized access to '{}' endpoint", request.getRequestURI());
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        String traceId = UUID.randomUUID().toString();
        ApiMessage message = messageService.createError(HttpStatus.UNAUTHORIZED.value(), "SEC0010", authException.getMessage(), traceId);

        mapper.writeValue(response.getWriter(), message);
    }
}
