/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.security.handler;

import com.broadcom.apiml.library.response.ApiMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.UUID;

@Component
public class UnauthorizedHandler implements AuthenticationEntryPoint {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(UnauthorizedHandler.class);
    private final ObjectMapper mapper;

    public UnauthorizedHandler(@Qualifier("securityObjectMapper") ObjectMapper objectMapper) {
        this.mapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        log.debug("Unauthorized access to '{}' endpoint", request.getRequestURI());
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        ApiMessage message =
            new ApiMessage(401, "No credentials were provided",
                "com.broadcom.apiml.security.authenticationRequired", UUID.randomUUID());
        mapper.writeValue(response.getWriter(), message);
    }
}
