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
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.utils.X509Utils;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.security.common.token.X509AuthenticationToken;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Optional;

import static org.mockito.Mockito.*;

class X509LoginFilterTest {

    private MockHttpServletRequest httpServletRequest;
    private MockHttpServletResponse httpServletResponse;

    private X509Filter x509Filter;

    private AuthenticationSuccessHandler successHandler;

    private AuthenticationService authenticationService;
    private AuthenticationProvider authenticationProvider;
    private FilterChain filterChain;

    private X509Certificate[] x509Certificate = new X509Certificate[]{
        X509Utils.getCertificate(X509Utils.correctBase64("zowe"), "CN=user"),
    };

    @BeforeEach
    void setup() {
        successHandler = mock(AuthenticationSuccessHandler.class);
        authenticationProvider = mock(AuthenticationProvider.class);
        filterChain = mock(FilterChain.class);
        authenticationService = mock(AuthenticationService.class);
        x509Filter = new X509Filter("/api/v1/gateway/auth/login", successHandler, authenticationProvider);

        when(authenticationService.getJwtTokenFromRequest(httpServletRequest)).thenReturn(Optional.of("jwt"));
    }

    @Test
    void shouldCallAuthProviderWithCertificate() {
        httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod(HttpMethod.POST.name());
        httpServletRequest.setAttribute("client.auth.X509Certificate", x509Certificate);
        httpServletResponse = new MockHttpServletResponse();

        x509Filter.attemptAuthentication(httpServletRequest, httpServletResponse);

        verify(authenticationProvider).authenticate(new X509AuthenticationToken(x509Certificate));
    }

    @Test
    void shouldAuthenticateWithCertificate() throws ServletException, IOException {
        httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod(HttpMethod.POST.name());
        httpServletRequest.setAttribute("client.auth.X509Certificate", x509Certificate);
        httpServletRequest.setServletPath("/api/v1/gateway/auth/login");

        httpServletResponse = new MockHttpServletResponse();
        when(authenticationProvider.authenticate(new X509AuthenticationToken(x509Certificate)))
            .thenReturn(new TokenAuthentication("user", "jwt"));
        x509Filter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        verify(authenticationProvider).authenticate(new X509AuthenticationToken(x509Certificate));
    }

    @Test
    void shouldNotAuthenticate() {
        httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod(HttpMethod.POST.name());
        httpServletResponse = new MockHttpServletResponse();

        x509Filter.attemptAuthentication(httpServletRequest, httpServletResponse);

        verify(authenticationProvider, times(0)).authenticate(new X509AuthenticationToken(x509Certificate));
    }


}
