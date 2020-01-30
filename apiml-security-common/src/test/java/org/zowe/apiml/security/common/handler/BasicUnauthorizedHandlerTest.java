/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.security.common.handler;

import org.zowe.apiml.security.common.error.AuthExceptionHandler;
import org.zowe.apiml.security.common.error.ErrorType;
import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.SpringRunner;

import javax.servlet.ServletException;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
public class BasicUnauthorizedHandlerTest {

    @Autowired
    private MessageService messageService;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    public void testCommence() throws IOException, ServletException {
        BasicAuthUnauthorizedHandler basicAuthUnauthorizedHandler = new BasicAuthUnauthorizedHandler(
            new AuthExceptionHandler(messageService, objectMapper));

        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setRequestURI("URI");

        MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();

        basicAuthUnauthorizedHandler.commence(httpServletRequest, httpServletResponse, new TokenExpireException("ERROR"));

        assertEquals(HttpStatus.UNAUTHORIZED.value(), httpServletResponse.getStatus());
        assertEquals(ApimlConstants.BASIC_AUTHENTICATION_PREFIX, httpServletResponse.getHeader(HttpHeaders.WWW_AUTHENTICATE));

        Message message = messageService.createMessage(
            ErrorType.TOKEN_EXPIRED.getErrorMessageKey(),
            httpServletRequest.getRequestURI());
        verify(objectMapper).writeValue(httpServletResponse.getWriter(), message.mapToView());
    }

    @Configuration
    static class ContextConfiguration {
        @Bean
        public MessageService messageService() {
            return new YamlMessageService("/security-service-messages.yml");
        }
    }
}
