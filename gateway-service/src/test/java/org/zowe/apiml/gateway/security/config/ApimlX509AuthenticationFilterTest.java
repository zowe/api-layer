package org.zowe.apiml.gateway.security.config;/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.zowe.apiml.gateway.utils.X509Utils;

import javax.security.auth.x500.X500Principal;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApimlX509AuthenticationFilterTest {

    private ServletRequest request;
    private ServletResponse response;
    private FilterChain chain;

    @BeforeEach
    public void setUp() {
        request = new MockHttpServletRequest();
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
    }

    @Test
    void givenNoCertificates_thenDontUpdate_whenCallFilter() throws IOException, ServletException {
        ApimlX509AuthenticationFilter filter = new ApimlX509AuthenticationFilter(Collections.emptySet());

        filter.doFilter(request, response, chain);

        assertNull(request.getAttribute("javax.servlet.request.X509Certificate"));
        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    void giveCertificates_thenRemoveForeign_whenCallFilter() throws IOException, ServletException {
        ApimlX509AuthenticationFilter filter = new ApimlX509AuthenticationFilter(new HashSet<>(Arrays.asList(
            X509Utils.correctBase64("apimlCert1"),
            X509Utils.correctBase64("apimlCert2")
        )));
        filter.setAuthenticationDetailsSource(mock(AuthenticationDetailsSource.class));
        filter.setAuthenticationManager(mock(AuthenticationManager.class));

        X509Certificate[] certificates = new X509Certificate[] {
            X509Utils.getCertificate(X509Utils.correctBase64("foreignCert1")),
            X509Utils.getCertificate(X509Utils.correctBase64("apimlCert1")),
            X509Utils.getCertificate(X509Utils.correctBase64("foreignCert2")),
            X509Utils.getCertificate(X509Utils.correctBase64("apimlCert2"))
        };
        //doReturn(certificates).when(request).getAttribute("javax.servlet.request.X509Certificate");
        request.setAttribute("javax.servlet.request.X509Certificate", certificates);

        filter.doFilter(request, response, chain);

        X509Certificate[] filtered = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
        assertNotNull(filtered);
        assertEquals(2, filtered.length);
        assertSame(certificates[1], filtered[0]);
        assertSame(certificates[3], filtered[1]);

        verify(chain, times(1)).doFilter(request, response);
    }

}
