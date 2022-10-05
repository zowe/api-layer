/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.login;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.zowe.apiml.security.common.error.ResourceAccessExceptionHandler;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BasicAuthFilterTest {
    HttpServletRequest request;
    HttpServletResponse response;
    AuthenticationManager manager;
    BasicAuthFilter filter;
    Authentication authenticationToken = new UsernamePasswordAuthenticationToken("principal", "credential");

    @BeforeEach
    void setup() {
        AuthenticationFailureHandler failureHandler = mock(AuthenticationFailureHandler.class);
        ObjectMapper om = mock(ObjectMapper.class);
        manager = mock(AuthenticationManager.class);
        ResourceAccessExceptionHandler exceptionHandler = mock(ResourceAccessExceptionHandler.class);
        filter = new BasicAuthFilter("/login", failureHandler, om, manager, exceptionHandler);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
    }

    @Test
    void attemptAuthentication() throws ServletException {
        String creds = new String(Base64.getEncoder().encode("username:pass".getBytes()));
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic " + creds);
        when(manager.authenticate(any())).thenReturn(authenticationToken);
        Authentication authentication = filter.attemptAuthentication(request, response);
        assertEquals(authenticationToken.getPrincipal(), authentication.getPrincipal());
    }

    @Test
    void successfulAuthentication() throws ServletException, IOException {
        FilterChain chain = mock(FilterChain.class);
        filter.successfulAuthentication(request, response, chain, authenticationToken);
        verify(chain, times(1)).doFilter(request, response);
    }
}
