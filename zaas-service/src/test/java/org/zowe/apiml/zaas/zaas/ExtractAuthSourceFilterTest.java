/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.zaas;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.security.common.error.AuthExceptionHandler;
import org.zowe.apiml.security.common.token.TokenNotValidException;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSource;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.zaas.security.service.schema.source.JwtAuthSource;
import org.zowe.apiml.zaas.security.service.schema.source.ParsedTokenAuthSource;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExtractAuthSourceFilterTest {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;
    private AuthSourceService authSourceService;
    private AuthExceptionHandler authExceptionHandler;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
        authSourceService = mock(AuthSourceService.class);
        authExceptionHandler = mock(AuthExceptionHandler.class);
    }

    @Nested
    class GivenAuthSourceInRequest {
        AuthSource authSource;

        @BeforeEach
        void setUp() {
            authSource = new JwtAuthSource("token");
            when(authSourceService.getAuthSourceFromRequest(request)).thenReturn(Optional.of(authSource));
        }

        @Nested
        class WhenAuthSourceIsValid {
            ParsedTokenAuthSource parseAuthSource;

            @BeforeEach
            void setUp() {
                parseAuthSource = new ParsedTokenAuthSource("userid", new Date(111), new Date(222), AuthSource.Origin.ZOSMF);
                when(authSourceService.parse(authSource)).thenReturn(parseAuthSource);
            }

            @Test
            void thenParsedAuthSourceInRequestAttribute() throws ServletException, IOException {
                ExtractAuthSourceFilter filter = new ExtractAuthSourceFilter(authSourceService, authExceptionHandler);
                filter.doFilterInternal(request, response, filterChain);
                verify(request, times(1)).setAttribute(ExtractAuthSourceFilter.AUTH_SOURCE_PARSED_ATTR, parseAuthSource);
            }
        }

        @Nested
        class WhenAuthSourceInvalid {

            RuntimeException tokenNotValidException;

            @BeforeEach
            void setUp() {
                tokenNotValidException = new TokenNotValidException("Token is not valid");
                when(authSourceService.parse(authSource)).thenThrow(tokenNotValidException);
            }

            @Test
            void thenExceptionHandlerIsCalled() throws ServletException, IOException {
                tokenNotValidException = new TokenNotValidException("Token is not valid");
                when(authSourceService.getAuthSourceFromRequest(request)).thenThrow(tokenNotValidException);
                when(request.getRequestURI()).thenReturn(null);

                ExtractAuthSourceFilter filter = new ExtractAuthSourceFilter(authSourceService, authExceptionHandler);

                filter.doFilterInternal(request, response, filterChain);

                ArgumentCaptor<RuntimeException> exceptionCaptor = ArgumentCaptor.forClass(RuntimeException.class);
                verify(authExceptionHandler).handleException(
                    any(),                   // URI
                    any(),                   // BiConsumer<ApiMessageView, HttpStatus>
                    any(),                   // BiConsumer<String, String>
                    exceptionCaptor.capture()
                );

                assertEquals(tokenNotValidException, exceptionCaptor.getValue());
            }
        }
    }

    @Nested
    class GivenNoAuthSourceInRequest {

        @BeforeEach
        void setUp() {
            when(authSourceService.getAuthSourceFromRequest(request)).thenReturn(Optional.empty());
        }

        @Test
        void thenExceptionIsHandled() throws ServletException, IOException {
            authSourceService = mock(AuthSourceService.class);
            authExceptionHandler = mock(AuthExceptionHandler.class);
            request = mock(HttpServletRequest.class);
            response = mock(HttpServletResponse.class);
            filterChain = mock(FilterChain.class);

            when(request.getRequestURI()).thenReturn("/some-uri");
            when(authSourceService.getAuthSourceFromRequest(request)).thenReturn(Optional.empty());

            ExtractAuthSourceFilter filter = new ExtractAuthSourceFilter(authSourceService, authExceptionHandler);

            filter.doFilterInternal(request, response, filterChain);

            ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<BiConsumer<ApiMessageView, HttpStatus>> consumerCaptor = ArgumentCaptor.forClass(BiConsumer.class);
            ArgumentCaptor<BiConsumer<String, String>> headerCaptor = ArgumentCaptor.forClass(BiConsumer.class);
            ArgumentCaptor<RuntimeException> exceptionCaptor = ArgumentCaptor.forClass(RuntimeException.class);

            verify(authExceptionHandler).handleException(
                uriCaptor.capture(),
                consumerCaptor.capture(),
                headerCaptor.capture(),
                exceptionCaptor.capture()
            );

            assertEquals("/some-uri", uriCaptor.getValue());
            assertInstanceOf(InsufficientAuthenticationException.class, exceptionCaptor.getValue());
        }
    }
}
