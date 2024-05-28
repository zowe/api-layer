/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.config;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;
import org.zowe.apiml.zaas.security.service.AuthenticationService;
import org.zowe.apiml.product.logging.LogMessageTracker;
import org.zowe.apiml.security.common.handler.FailedAuthenticationHandler;
import org.zowe.apiml.security.common.token.TokenFormatNotValidException;
import org.zowe.apiml.security.common.token.TokenNotProvidedException;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class JWTLogoutHandlerTest {
    private static final String TOKEN = "apimlToken";

    private AuthenticationService authenticationService;
    private FailedAuthenticationHandler failedAuthenticationHandler;

    private final LogMessageTracker logMessageTracker = new LogMessageTracker(JWTLogoutHandler.class);

    private HttpServletRequest request;
    private HttpServletResponse response;
    private Authentication authentication;

    private JWTLogoutHandler handler;

    @BeforeEach
    void setup() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        authentication = mock(Authentication.class);

        authenticationService = mock(AuthenticationService.class);
        failedAuthenticationHandler = mock(FailedAuthenticationHandler.class);
        handler = new JWTLogoutHandler(authenticationService, failedAuthenticationHandler);

        when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.of(TOKEN));
        when(authenticationService.isInvalidated(TOKEN)).thenReturn(false);
    }

    @Test
    void givenToken_whenLogout_thenTokenInvalidated() {
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
        when(authenticationService.invalidateJwtToken(TOKEN, true)).thenThrow(new TokenFormatNotValidException("msg"));
        handler.logout(request, response, authentication);
        verify(failedAuthenticationHandler, times(1)).onAuthenticationFailure(any(), any(), any());
    }

    @Test
    void givenInvalidToken_validityExceptionIsThrown_thenItsCorrectlyHandled() throws ServletException {
        when(authenticationService.invalidateJwtToken(TOKEN, true)).thenThrow(new TokenNotValidException("msg"));
        handler.logout(request, response, authentication);
        verify(failedAuthenticationHandler, times(1)).onAuthenticationFailure(any(), any(), any());
    }

    @Test
    void givenAlreadyInvalidatedToken_whenLogout_thenHandleFailure() throws ServletException {
        when(authenticationService.isInvalidated(TOKEN)).thenReturn(true);
        handler.logout(request, response, authentication);
        verify(failedAuthenticationHandler, times(1)).onAuthenticationFailure(any(), any(), any());
    }

    @Test
    void givenInvalidatedToken_whenTokenNotValidFailureThrowsError_thenErrorThrown() throws ServletException {
        when(authenticationService.invalidateJwtToken(TOKEN, true)).thenThrow(new TokenNotValidException("msg"));

        handler.logout(request, response, authentication);
        verify(failedAuthenticationHandler, times(1))
            .onAuthenticationFailure(any(), any(), any(TokenFormatNotValidException.class));
    }

    @Test
    void givenInvalidatedToken_whenGenericAuthenticationFailure_thenHandleFailure() throws ServletException {
        when(authenticationService.invalidateJwtToken(TOKEN, true)).thenThrow(new TokenNotProvidedException("msg"));

        handler.logout(request, response, authentication);
        verify(failedAuthenticationHandler, timeout(1)).onAuthenticationFailure(any(), any(), any(TokenNotProvidedException.class));
    }

    @Test
    void givenLogoutRequest_whenGenericFailure_thenHandleFailure() throws ServletException {
        when(authenticationService.invalidateJwtToken(TOKEN, true)).thenThrow(new RuntimeException("msg"));

        handler.logout(request, response, authentication);
        verify(failedAuthenticationHandler, times(1))
            .onAuthenticationFailure(any(), any(), any(TokenNotValidException.class));
    }

    @Test
    void givenLogoutRequest_whenFailureHandlingError_thenLogError() throws ServletException {
        logMessageTracker.startTracking();
        when(authenticationService.getJwtTokenFromRequest(request)).thenReturn(Optional.empty());
        Mockito.doThrow(new ServletException("msg")).when(failedAuthenticationHandler).onAuthenticationFailure(any(), any(), any());
        handler.logout(request, response, authentication);
        String expectedLogMessage = "The response cannot be written during the logout exception handler: msg";
        assertTrue(logMessageTracker.contains(expectedLogMessage, Level.ERROR));
        logMessageTracker.stopTracking();
    }
}
