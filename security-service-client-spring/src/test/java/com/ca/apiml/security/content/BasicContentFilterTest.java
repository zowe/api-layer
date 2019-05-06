/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.apiml.security.content;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BasicContentFilterTest {

    private BasicContentFilter basicContentFilter;
    private AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private AuthenticationFailureHandler authenticationFailureHandler = mock(AuthenticationFailureHandler.class);
    private HttpServletRequest request = mock(HttpServletRequest.class);

    @Before
    public void setUp() {
        basicContentFilter = new BasicContentFilter(authenticationManager, authenticationFailureHandler);
    }

    @Test
    public void extractContentFromRequestWithValidBasicAuth() {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic dXNlcjpwYXNzd29yZA==");
        Optional<AbstractAuthenticationToken> token = basicContentFilter.extractContent(request);

        assertTrue(token.isPresent());
        assertEquals("user",token.get().getPrincipal());
        assertEquals("password",token.get().getCredentials().toString());
    }

    @Test
    public void extractContentFromRequestWithNonsenseBasicAuth() {
        when(request.getHeader(anyString())).thenReturn("Basic dXNlG4m3oFthR0n3syZA==");
        Optional<AbstractAuthenticationToken> token = basicContentFilter.extractContent(request);

        assertTrue(token.isPresent());
        assertNull(token.get().getPrincipal());
        assertNull(token.get().getCredentials());
    }

    @Test
    public void extractContentFromRequestWithNonsenseBasicAuth2() {
        when(request.getHeader(anyString())).thenReturn("Duck");
        Optional<AbstractAuthenticationToken> token = basicContentFilter.extractContent(request);

        assertFalse(token.isPresent());
    }

    @Test
    public void extractContentFromRequestWithoutAuthHeader() {
        when(request.getHeader(anyString())).thenReturn(null);
        Optional<AbstractAuthenticationToken> token = basicContentFilter.extractContent(request);

        assertFalse(token.isPresent());
    }

}

