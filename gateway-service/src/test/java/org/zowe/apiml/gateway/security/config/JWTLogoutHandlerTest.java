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
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.security.common.handler.FailedAuthenticationHandler;
import org.zowe.apiml.security.common.token.TokenFormatNotValidException;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

import static org.mockito.Mockito.*;

class JWTLogoutHandlerTest {
    private static final String TOKEN = "apimlToken";

    private final ExpectedException expectedException = ExpectedException.none();

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

        when(authenticationService.isInvalidated(TOKEN)).thenReturn(false);
    }

    @Test
    void givenToken_whenLogout_thenTokenInvalidated() {
        when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of(TOKEN));
        handler.logout(request, response, authentication);
        verify(authenticationService, times(1)).invalidateJwtToken(TOKEN, true);
    }

    @Test
    void givenNoToken_whenLogout_thenHandleFailure() throws ServletException {
        when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.empty());
        handler.logout(request, response, authentication);
        verify(failedAuthenticationHandler, times(1)).onAuthenticationFailure(any(), any(), any());
    }

    @Test
    void givenInvalidToken_formatExceptionIsThrown_thenItsCorrectlyHandled() throws ServletException {
        when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of(TOKEN));
        when(authenticationService.invalidateJwtToken(TOKEN, true)).thenThrow(new TokenFormatNotValidException("msg"));
        handler.logout(request, response, authentication);
        verify(failedAuthenticationHandler, times(1)).onAuthenticationFailure(any(), any(), any());
    }

    @Test
    void givenInvalidToken_validityExceptionIsThrown_thenItsCorrectlyHandled() throws ServletException {
        when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of(TOKEN));
        when(authenticationService.invalidateJwtToken(TOKEN, true)).thenThrow(new TokenNotValidException("msg"));
        handler.logout(request, response, authentication);
        verify(failedAuthenticationHandler, times(1)).onAuthenticationFailure(any(), any(), any());
    }

    @Test
    void givenAlreadyInvalidatedToken_whenLogout_thenHandleFailure() throws ServletException {
        when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of(TOKEN));
        when(authenticationService.isInvalidated(TOKEN)).thenReturn(true);
        handler.logout(request, response, authentication);
        verify(failedAuthenticationHandler, times(1)).onAuthenticationFailure(any(), any(), any());
    }

    @Test
    void givenInvalidatedToken_whenLogoutFailureThrowsError_thenErrorThrown() throws ServletException {
        Mockito.doThrow(new ServletException("msg")).when(failedAuthenticationHandler).onAuthenticationFailure(any(), any(), any());
        when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.empty());

        expectedException.expect(ServletException.class);
        expectedException.expectMessage("The response cannot be written during the logout exception handler: msg");

        handler.logout(request, response, authentication);
    }

    @Test
    void givenLogoutRequest_whenUnknownError_thenHandleFailure() throws ServletException {
        when(authenticationService.invalidateJwtToken(TOKEN, true)).thenThrow(new RuntimeException("msg"));

        handler.logout(request, response, authentication);

        verify(failedAuthenticationHandler, times(1)).onAuthenticationFailure(any(), any(), any());
    }
}
