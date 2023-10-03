/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters.pre;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.OIDCAuthSourceService;
import org.zowe.apiml.gateway.security.service.schema.source.ParsedTokenAuthSource;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

class OIDCAuthenticationFilterTest {
    private HttpServletRequest request;
    private HttpServletResponse response;
    private OIDCAuthSourceService oidcAuthSourceService;
    private AuthenticationFailureHandler failureHandler;
    private OIDCAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        oidcAuthSourceService = mock(OIDCAuthSourceService.class);
        failureHandler = mock(AuthenticationFailureHandler.class);
        filter = new OIDCAuthenticationFilter("/**", failureHandler, oidcAuthSourceService);
    }

    @Nested
    class GivenAttemptAuthentication {

        private AuthSource.Parsed parsedToken;

        @BeforeEach
        void setUp() {
            Optional<String> accessToken = Optional.of("token_value");
            when(oidcAuthSourceService.getToken(request)).thenReturn(accessToken);
        }

        @Test
        void whenValidAccessToken_thenAuthenticated() {
            parsedToken = new ParsedTokenAuthSource("MF_userid", new Date(111), new Date(222), AuthSource.Origin.OIDC);
            when(oidcAuthSourceService.parse(any())).thenReturn(parsedToken);

            Authentication authentication = filter.attemptAuthentication(request, response);
            assertEquals(parsedToken.getUserId(), authentication.getPrincipal());
            assertTrue(authentication.isAuthenticated());
        }

        @Test
        void whenNoUserMapping_thenNotAuthenticated() {
            parsedToken = new ParsedTokenAuthSource("", new Date(111), new Date(222), AuthSource.Origin.OIDC);
            when(oidcAuthSourceService.parse(any())).thenReturn(parsedToken);

            Authentication authentication = filter.attemptAuthentication(request, response);
            assertNull(authentication);
        }

        @Test
        void whenInvalidAccessToken_thenNotAuthenticated() {
            when(oidcAuthSourceService.parse(any())).thenReturn(null);

            Authentication authentication = filter.attemptAuthentication(request, response);
            assertNull(authentication);
        }
    }

    @Nested
    class GivenSuccessfulAuthentication {

        private Authentication authenticationToken;

        @BeforeEach
        void setUp() {
            authenticationToken = new UsernamePasswordAuthenticationToken("principal", "credential");
        }

        @Test
        void thenContinueFilterChain() throws ServletException, IOException {
            FilterChain chain = mock(FilterChain.class);
            filter.successfulAuthentication(request, response, chain, authenticationToken);
            verify(chain, times(1)).doFilter(request, response);
        }
    }

    @Nested
    class GivenUnsuccessfulAuthentication {

        private AuthenticationException authenticationException;

        @BeforeEach
        void setUp() {
            authenticationException = mock(AuthenticationException.class);
        }
        @Test
        void thenFailureHandlerCalled() throws ServletException, IOException {
            filter.unsuccessfulAuthentication(request, response, authenticationException);
            verify(failureHandler, times(1)).onAuthenticationFailure(request, response, authenticationException);
        }
    }
}
