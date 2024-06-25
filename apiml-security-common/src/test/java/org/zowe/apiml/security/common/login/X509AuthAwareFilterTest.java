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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.zowe.apiml.security.common.utils.X509Utils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.security.cert.X509Certificate;

import static org.mockito.Mockito.*;

class X509AuthAwareFilterTest {

    private MockHttpServletRequest httpServletRequest;
    private MockHttpServletResponse httpServletResponse;

    private X509AuthAwareFilter x509AuthAwareFilter;
    private AuthenticationFailureHandler failureHandler;
    private AuthenticationProvider authenticationProvider;
    AuthenticationException authenticationException = mock(AuthenticationException.class);
    private X509Certificate[] x509Certificate = new X509Certificate[]{
        X509Utils.getCertificate(X509Utils.correctBase64("zowe"), "CN=user"),
    };

    @BeforeEach
    void setup() {
        httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod(HttpMethod.POST.name());
        httpServletRequest.setAttribute("client.auth.X509Certificate", x509Certificate);
        httpServletRequest.setServletPath("/gateway/api/v1/auth/login");

        httpServletResponse = new MockHttpServletResponse();
        failureHandler = mock(AuthenticationFailureHandler.class);
        authenticationProvider = mock(AuthenticationProvider.class);
        x509AuthAwareFilter = new X509AuthAwareFilter("/gateway/api/v1/auth/login", failureHandler, authenticationProvider);
    }

    @Nested
    class Givenx509AuthRequest {

        @Test
        void whenSuccessfulAuth_thenDoFilter() throws ServletException, IOException {
            FilterChain chain = mock(FilterChain.class);
            Authentication authentication = mock(Authentication.class);
            x509AuthAwareFilter.successfulAuthentication(httpServletRequest, httpServletResponse, chain, authentication);
            verify(chain, times(1)).doFilter(httpServletRequest, httpServletResponse);
        }

        @Test
        void whenUnsuccessfulAuth_thenFail() throws ServletException, IOException {
            x509AuthAwareFilter.unsuccessfulAuthentication(httpServletRequest, httpServletResponse, authenticationException);
            verify(failureHandler, times(1)).onAuthenticationFailure(httpServletRequest, httpServletResponse, authenticationException);
        }
    }
}
