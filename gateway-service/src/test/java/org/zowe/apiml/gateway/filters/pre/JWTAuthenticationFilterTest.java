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

import com.netflix.zuul.context.RequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.JwtAuthSourceService;
import org.zowe.apiml.gateway.security.service.schema.source.ParsedTokenAuthSource;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

public class JWTAuthenticationFilterTest {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private JwtAuthSourceService jwtAuthSourceService;
    private AuthenticationFailureHandler failureHandler;
    private JWTAuthenticationFilter filter;

    private RequestContext context;
    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        context = mock(RequestContext.class);
        jwtAuthSourceService = mock(JwtAuthSourceService.class);
        failureHandler = mock(AuthenticationFailureHandler.class);
        filter = new JWTAuthenticationFilter("/**", failureHandler, jwtAuthSourceService);
    }

    @Nested
    class GivenAttemptAuthentication {

        private AuthSource.Parsed parsedToken;

        @BeforeEach
        void setUp() {
            Optional<String> accessToken = Optional.of("token_value");
            when(jwtAuthSourceService.getToken(context)).thenReturn(accessToken);
        }

        @Test
        void whenValidAccessToken_thenAuthenticated() {
            parsedToken = new ParsedTokenAuthSource("user", new Date(111), new Date(222), AuthSource.Origin.ZOSMF);
            when(jwtAuthSourceService.parse(any())).thenReturn(parsedToken);

            Authentication authentication = filter.attemptAuthentication(context.getRequest(), response);
            assertEquals(parsedToken.getUserId(), authentication.getPrincipal());
            assertTrue(authentication.isAuthenticated());
        }

        @Test
        void whenNoUserMapping_thenNotAuthenticated() {
            parsedToken = new ParsedTokenAuthSource("", new Date(111), new Date(222), AuthSource.Origin.ZOSMF);
            when(jwtAuthSourceService.parse(any())).thenReturn(parsedToken);

            Authentication authentication = filter.attemptAuthentication(context.getRequest(), response);
            assertNull(authentication);
        }

        @Test
        void whenInvalidAccessToken_thenNotAuthenticated() {
            when(jwtAuthSourceService.parse(any())).thenReturn(null);

            Authentication authentication = filter.attemptAuthentication(context.getRequest(), response);
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

