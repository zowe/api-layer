/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.error;

import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.client.HttpServerErrorException;

import javax.servlet.ServletException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class ResourceAccessExceptionHandlerTest {

    private final MessageService messageService = new YamlMessageService("/security-service-messages.yml");

    private ResourceAccessExceptionHandler resourceAccessExceptionHandler;
    private MockHttpServletRequest httpServletRequest;
    private MockHttpServletResponse httpServletResponse;

    @Mock
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        resourceAccessExceptionHandler = new ResourceAccessExceptionHandler(messageService, objectMapper);

        httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setRequestURI("URI");

        httpServletResponse = new MockHttpServletResponse();
    }

    @Test
    void shouldRethrowException() throws ServletException {
        HttpServerErrorException response = assertDoesNotThrow(() -> new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThrows(HttpServerErrorException.class, () -> resourceAccessExceptionHandler.handleException(httpServletRequest, httpServletResponse, response));
    }

    @Test
    void shouldThrowServletExceptionOnIOException() throws Exception {
        Message message = messageService.createMessage(ErrorType.GATEWAY_NOT_AVAILABLE.getErrorMessageKey(), httpServletRequest.getRequestURI());
        doThrow(new IOException("Error in writing response")).when(objectMapper).writeValue(httpServletResponse.getWriter(), message.mapToView());

        assertThrows(ServletException.class, () -> {
            resourceAccessExceptionHandler.writeErrorResponse(message.mapToView(), HttpStatus.NOT_FOUND, httpServletResponse);
        });
    }
}
