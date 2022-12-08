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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.zowe.apiml.security.common.audit.Rauditx;
import org.zowe.apiml.security.common.audit.RauditxService;
import org.zowe.apiml.security.common.error.AuthExceptionHandler;

import javax.servlet.ServletException;

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
        void issueRauditx() throws ServletException {
            failedAccessTokenHandler.onAuthenticationFailure(mockHttpServletRequest, mockHttpServletResponse, authenticationException);
            verify(rauditxService).builder();
            verify(rauditxBuilder).failure();
            verify(rauditxBuilder).issue();
        }

        @Test
        void handleException() throws ServletException {
            failedAccessTokenHandler.onAuthenticationFailure(mockHttpServletRequest, mockHttpServletResponse, authenticationException);
            verify(authExceptionHandler).handleException(mockHttpServletRequest, mockHttpServletResponse, authenticationException);
        }

    }

}