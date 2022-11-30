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
import org.zowe.apiml.security.common.audit.Rauditx;
import org.zowe.apiml.security.common.audit.RauditxService;
import org.zowe.apiml.security.common.token.AccessTokenProvider;
import org.zowe.apiml.security.common.token.TokenAuthentication;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.security.common.filter.StoreAccessTokenInfoFilter.TOKEN_REQUEST;

class SuccessfulAccessTokenHandlerTest {

    private static final String USERNAME = "user";
    private final TokenAuthentication dummyAuth = new TokenAuthentication(USERNAME, "TEST_TOKEN_STRING");
    private SuccessfulAccessTokenHandler underTest;
    private AccessTokenProvider accessTokenProvider;
    private MockHttpServletRequest httpServletRequest;
    private MockHttpServletResponse httpServletResponse;

    RauditxService rauditxService;
    RauditxService.RauditxBuilder rauditBuilder;

    static Set<String> scopes = new HashSet<>();
    static SuccessfulAccessTokenHandler.AccessTokenRequest accessTokenRequest = new SuccessfulAccessTokenHandler.AccessTokenRequest(80, scopes);

    static {
        scopes.add("gateway");
    }

    @BeforeEach
    void setup() {
        httpServletRequest = new MockHttpServletRequest();
        httpServletResponse = new MockHttpServletResponse();
        accessTokenProvider = mock(AccessTokenProvider.class);

        rauditxService = new RauditxService();
        rauditBuilder = spy(rauditxService.new RauditxBuilder(mock(Rauditx.class)));
        rauditxService = spy(rauditxService);
        doReturn(rauditBuilder).when(rauditxService).builder();

        underTest = new SuccessfulAccessTokenHandler(accessTokenProvider, rauditxService);
        httpServletRequest.setAttribute(TOKEN_REQUEST, accessTokenRequest);
    }

    @Nested
    class WhenCallingOnAuthentication {
        @Test
        void thenReturn200() throws IOException {
            when(accessTokenProvider.getToken(any(), anyInt(), any())).thenReturn("jwtToken");
            executeLoginHandler();

            assertEquals(HttpStatus.OK.value(), httpServletResponse.getStatus());
        }

        @Test
        void givenNullExpiration_thenReturn200() throws IOException {
            when(accessTokenProvider.getToken(any(), anyInt(), any())).thenReturn("jwtToken");
            executeLoginHandler();

            assertEquals(HttpStatus.OK.value(), httpServletResponse.getStatus());
        }

        @Test
        void givenResponseNotCommitted_thenThrowIOException() throws IOException {
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

    @Nested
    class RauditxIntegration {

        private static final String MESSAGE = "An attempt to generate PAT";

        void verifyCommons() {
            verify(rauditBuilder).userId(USERNAME);
            verify(rauditBuilder).messageSegment(MESSAGE);
            verify(rauditBuilder).alwaysLogSuccesses();
            verify(rauditBuilder).alwaysLogFailures();
        }

        @Test
        void whenProperInputs_thenRauditxIsGenerated() throws IOException {
            doReturn("token").when(accessTokenProvider).getToken(anyString(), anyInt(), any());

            underTest.onAuthenticationSuccess(httpServletRequest, httpServletResponse, dummyAuth);

            verifyCommons();
            verify(rauditBuilder).success();
            verify(rauditBuilder).issue();
        }

        @Test
        void whenImproperInputs_thenRauditxIsGenerated() {
            Exception e = new IllegalStateException("Cannot generate");
            doThrow(e).when(accessTokenProvider).getToken(anyString(), anyInt(), any());

            assertSame(e, assertThrows(IllegalStateException.class, () -> underTest.onAuthenticationSuccess(httpServletRequest, httpServletResponse, dummyAuth)));

            verifyCommons();
            verify(rauditBuilder).failure();
            verify(rauditBuilder).issue();
        }

    }

    private void executeLoginHandler() throws IOException {
        underTest.onAuthenticationSuccess(httpServletRequest, httpServletResponse, dummyAuth);
    }
}
