/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.content;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.zowe.apiml.security.common.error.ResourceAccessExceptionHandler;
import org.zowe.apiml.security.common.token.TokenAuthentication;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BearerContentFilterTest {
    private BearerContentFilter bearerContentFilter;
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final FilterChain filterChain = mock(FilterChain.class);
    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final AuthenticationFailureHandler authenticationFailureHandler = mock(AuthenticationFailureHandler.class);
    private final ResourceAccessExceptionHandler resourceAccessExceptionHandler = mock(ResourceAccessExceptionHandler.class);
    private final static String BEARER_AUTH = "Bearer token";

    @BeforeEach
    void setUp() {
        bearerContentFilter = new BearerContentFilter(
            authenticationManager,
            authenticationFailureHandler,
            resourceAccessExceptionHandler);
    }

    @Nested
    class GivenValidBearerHeader {
        @Nested
        class WhenAuthenticate {
            @Test
            void thenSuccess() throws ServletException, IOException {
                String token = "token";
                TokenAuthentication tokenAuthentication = new TokenAuthentication(token);
                request.addHeader(HttpHeaders.AUTHORIZATION, BEARER_AUTH);

                bearerContentFilter.doFilter(request, response, filterChain);

                verify(authenticationManager).authenticate(tokenAuthentication);
                verify(filterChain).doFilter(request, response);
                verify(authenticationFailureHandler, never()).onAuthenticationFailure(any(), any(), any());
                verify(resourceAccessExceptionHandler, never()).handleException(any(), any(), any());
            }
        }

        @Nested
        class whenAuthenticateWithNoGateway {
            @Test
            void thenAuthenticationFails() throws ServletException, IOException {
                String token = "token";
                RuntimeException exception = new RuntimeException("No Gateway");

                TokenAuthentication tokenAuthentication = new TokenAuthentication(token);
                request.addHeader(HttpHeaders.AUTHORIZATION, BEARER_AUTH);
                when(authenticationManager.authenticate(tokenAuthentication)).thenThrow(exception);

                bearerContentFilter.doFilter(request, response, filterChain);

                verify(authenticationManager).authenticate(tokenAuthentication);
                verify(filterChain, never()).doFilter(any(), any());
                verify(authenticationFailureHandler, never()).onAuthenticationFailure(any(), any(), any());
                verify(resourceAccessExceptionHandler).handleException(request, response, exception);
            }
        }
    }

    @Nested
    class WhenGatewayEndpoint {
        @Test
        void thenSkipFilter() throws ServletException, IOException {
            String[] endpoints = {"/gateway"};

            request.setContextPath(endpoints[0]);

            BearerContentFilter bearerContentFilter = new BearerContentFilter(authenticationManager,
                authenticationFailureHandler,
                resourceAccessExceptionHandler,
                endpoints);

            bearerContentFilter.doFilter(request, response, filterChain);

            verify(authenticationManager, never()).authenticate(any());
            verify(authenticationFailureHandler, never()).onAuthenticationFailure(any(), any(), any());
            verify(resourceAccessExceptionHandler, never()).handleException(any(), any(), any());
        }
    }

    @Nested
    class GivenInValidToken {
        @Nested
        class WhenAuthenticate {
            @Test
            void thenAuthenticationFails() throws ServletException, IOException {
                String token = "token";
                AuthenticationException exception = new BadCredentialsException("Token not valid");

                TokenAuthentication tokenAuthentication = new TokenAuthentication(token);
                request.addHeader(HttpHeaders.AUTHORIZATION, BEARER_AUTH);
                when(authenticationManager.authenticate(tokenAuthentication)).thenThrow(exception);

                bearerContentFilter.doFilter(request, response, filterChain);

                verify(authenticationManager).authenticate(tokenAuthentication);
                verify(filterChain, never()).doFilter(any(), any());
                verify(authenticationFailureHandler).onAuthenticationFailure(request, response, exception);
                verify(resourceAccessExceptionHandler, never()).handleException(any(), any(), any());
            }
        }
    }

    @Nested
    class WhenNoBearerHeader {
        @Test
        void thenNotFilter() throws ServletException, IOException {
            bearerContentFilter.doFilter(request, response, filterChain);

            verify(authenticationManager, never()).authenticate(any());
            verify(filterChain).doFilter(request, response);
            verify(authenticationFailureHandler, never()).onAuthenticationFailure(any(), any(), any());
            verify(resourceAccessExceptionHandler, never()).handleException(any(), any(), any());
        }

        @Test
        void thenReturnEmpty() {
            Optional<AbstractAuthenticationToken> content = bearerContentFilter.extractContent(request);

            assertEquals(Optional.empty(), content);
        }
    }

    @Nested
    class WhenBearerHeader {
        @Test
        void thenExtractContent() {
            request.addHeader(HttpHeaders.AUTHORIZATION, BEARER_AUTH);
            Optional<AbstractAuthenticationToken> content = bearerContentFilter.extractContent(request);

            TokenAuthentication actualToken = new TokenAuthentication("token");

            assertTrue(content.isPresent());
            assertEquals(actualToken, content.get());
        }
    }
}
