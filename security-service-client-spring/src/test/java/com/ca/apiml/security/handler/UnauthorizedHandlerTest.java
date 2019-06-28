/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.apiml.security.handler;

import com.ca.apiml.security.error.ErrorType;
import com.ca.apiml.security.error.AuthExceptionHandler;
import com.ca.apiml.security.token.TokenExpireException;
import com.ca.mfaas.error.ErrorService;
import com.ca.mfaas.error.impl.ErrorServiceImpl;
import com.ca.mfaas.rest.response.ApiMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.SpringRunner;

import javax.servlet.ServletException;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
public class UnauthorizedHandlerTest {

    @Autowired
    private ErrorService errorService;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    public void testCommence() throws IOException, ServletException {
        UnauthorizedHandler unauthorizedHandler = new UnauthorizedHandler(new AuthExceptionHandler(errorService, objectMapper));

        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setRequestURI("URI");

        MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();

        unauthorizedHandler.commence(httpServletRequest, httpServletResponse, new TokenExpireException("ERROR"));

        assertEquals(HttpStatus.UNAUTHORIZED.value(), httpServletResponse.getStatus());

        ApiMessage message = errorService.createApiMessage(ErrorType.TOKEN_EXPIRED.getErrorMessageKey(), httpServletRequest.getRequestURI());
        verify(objectMapper).writeValue(httpServletResponse.getWriter(), message);
    }

    @Configuration
    static class ContextConfiguration {
        @Bean
        public ErrorService errorService() {
            return new ErrorServiceImpl("/security-service-messages.yml");
        }
    }
}
