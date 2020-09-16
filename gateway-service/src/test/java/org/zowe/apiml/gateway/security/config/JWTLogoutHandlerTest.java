/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.security.core.Authentication;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.security.common.handler.FailedAuthenticationHandler;
import org.zowe.apiml.security.common.token.TokenFormatNotValidException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

import static org.mockito.Mockito.*;

class JWTLogoutHandlerTest {


    private AuthenticationService authenticationService;

    private FailedAuthenticationHandler failedAuthenticationHandler;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private Authentication authentication;
    private JWTLogoutHandler handler;

    @BeforeEach
    void setup() {
        authenticationService = mock(AuthenticationService.class);
        failedAuthenticationHandler = mock(FailedAuthenticationHandler.class);
        handler = new JWTLogoutHandler(authenticationService, failedAuthenticationHandler);
    }

    @Test
    void testLogout() {
        when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of("apimlToken"));
        handler.logout(request, response, authentication);
        verify(authenticationService, times(1)).invalidateJwtToken("apimlToken", true);
    }

    @Test
    void givenInvalidToken() throws ServletException {
        when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.empty());
        handler.logout(request, response, authentication);
        verify(failedAuthenticationHandler, times(1)).onAuthenticationFailure(any(), any(), any());
    }

    @Test
    void givenInvalidToken_exceptionIsThrown_thenItsCorrectlyHandled() throws ServletException {
        when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.empty());
        when(authenticationService.invalidateJwtToken("apimlToken", true)).thenThrow(new TokenFormatNotValidException("msg"));
        handler.logout(request, response, authentication);
        verify(failedAuthenticationHandler, times(1)).onAuthenticationFailure(any(), any(), any());
    }
}
