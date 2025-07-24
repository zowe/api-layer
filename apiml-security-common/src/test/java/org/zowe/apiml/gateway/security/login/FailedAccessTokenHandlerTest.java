/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.login;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.security.common.audit.Rauditx;
import org.zowe.apiml.security.common.audit.RauditxService;
import org.zowe.apiml.security.common.error.AuthExceptionHandler;
import org.zowe.apiml.security.common.handler.FailedAccessTokenHandler;

import java.io.IOException;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class FailedAccessTokenHandlerTest {

    AuthExceptionHandler authExceptionHandler;
    RauditxService rauditxService;
    RauditxService.RauditxBuilder rauditxBuilder;

    FailedAccessTokenHandler failedAccessTokenHandler;

    @BeforeEach
    void setUp() {
        authExceptionHandler = mock(AuthExceptionHandler.class);
        rauditxService = new RauditxService() {
            @Override
            public RauditxBuilder builder() {
                return rauditxBuilder;
            }
        };
        rauditxBuilder = spy(rauditxService.new RauditxBuilder(mock(Rauditx.class)));
        rauditxService = spy(rauditxService);
        failedAccessTokenHandler = new FailedAccessTokenHandler(authExceptionHandler, rauditxService);
    }

    @Nested
    class WhenCallingOnAuthentication {

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
        AuthenticationException authenticationException = mock(AuthenticationException.class);

        @Test
        void issueRauditx() throws ServletException, IOException {
            failedAccessTokenHandler.onAuthenticationFailure(mockHttpServletRequest, mockHttpServletResponse, authenticationException);
            verify(rauditxService).builder();
            verify(rauditxBuilder).failure();
            verify(rauditxBuilder).issue();
        }

        @Test
        void handleException() throws ServletException, IOException {
            failedAccessTokenHandler.onAuthenticationFailure(mockHttpServletRequest, mockHttpServletResponse, authenticationException);

            ArgumentCaptor<RuntimeException> exceptionCaptor = ArgumentCaptor.forClass(RuntimeException.class);
            ArgumentCaptor<BiConsumer<ApiMessageView, HttpStatus>> consumerCaptor = ArgumentCaptor.forClass(BiConsumer.class);
            ArgumentCaptor<BiConsumer<String, String>> headerCaptor = ArgumentCaptor.forClass(BiConsumer.class);
            verify(authExceptionHandler).handleException(
                eq(mockHttpServletRequest.getRequestURI()),
                consumerCaptor.capture(),
                headerCaptor.capture(),
                exceptionCaptor.capture()
            );

            assertEquals(authenticationException, exceptionCaptor.getValue());
        }

    }

}
