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
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.zowe.apiml.security.common.token.AccessTokenProvider;
import org.zowe.apiml.security.common.token.TokenAuthentication;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class SuccessfulAccessTokenHandlerTest {
    private final TokenAuthentication dummyAuth = new TokenAuthentication("user", "TEST_TOKEN_STRING");
    private SuccessfulAccessTokenHandler underTest;
    private AccessTokenProvider accessTokenProvider;
    private MockHttpServletRequest httpServletRequest;
    private MockHttpServletResponse httpServletResponse;

    @BeforeEach
    void setup() {
        httpServletRequest = new MockHttpServletRequest();
        httpServletResponse = new MockHttpServletResponse();
        accessTokenProvider = mock(AccessTokenProvider.class);

        underTest = new SuccessfulAccessTokenHandler(accessTokenProvider);
    }

    @Nested
    class WhenCallingOnAuthentication {
        @Test
        void thenReturn200() throws ServletException, IOException {
            httpServletRequest.setAttribute("expirationTime", 90);
            when(accessTokenProvider.getToken(any(), anyInt(),any())).thenReturn("jwtToken");
            executeLoginHandler();

            assertEquals(HttpStatus.OK.value(), httpServletResponse.getStatus());
        }

        @Test
        void givenNullExpiration_thenReturn200() throws ServletException, IOException {
            httpServletRequest.setAttribute("expirationTime", "");
            when(accessTokenProvider.getToken(any(), anyInt(), any())).thenReturn("jwtToken");
            executeLoginHandler();

            assertEquals(HttpStatus.OK.value(), httpServletResponse.getStatus());
        }

        @Test
        void givenResponseNotCommitted_thenThrowIOException() throws IOException {
            httpServletRequest.setAttribute("expirationTime", 90);
            when(accessTokenProvider.getToken(any(), anyInt(), any())).thenReturn("jwtToken");
            HttpServletResponse servletResponse = mock(HttpServletResponse.class);
            PrintWriter mockWriter = mock(PrintWriter.class);
            when(servletResponse.getWriter()).thenReturn(mockWriter);
            when(servletResponse.isCommitted()).thenReturn(false);
            assertThrows(IOException.class, () -> {
                underTest.onAuthenticationSuccess(httpServletRequest, servletResponse, dummyAuth);
            });
        }
    }

    private void executeLoginHandler() throws ServletException, IOException {
        underTest.onAuthenticationSuccess(httpServletRequest, httpServletResponse, dummyAuth);
    }
}
