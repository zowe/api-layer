/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.zowe.apiml.security.common.error.AuthExceptionHandler;
import org.zowe.apiml.security.common.error.InvalidCertificateException;
import org.zowe.apiml.security.common.utils.X509Utils;

import java.io.IOException;
import java.security.cert.X509Certificate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.security.common.filter.X509ClientRejectIfMissingFilter.ATTRNAME_CLIENT_AUTH_X509_CERTIFICATE;

class X509ClientRejectIfMissingFilterTest {

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;
    private AuthExceptionHandler authExceptionHandler;
    private Filter nextFilter;

    private final X509Certificate[] x509Certificate =
        new X509Certificate[]{X509Utils.getCertificate(X509Utils.correctBase64("zowe"), "CN=user"),};

    @BeforeEach
    void setUp() {
        authExceptionHandler = Mockito.mock(AuthExceptionHandler.class);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        nextFilter = Mockito.mock(Filter.class);
        chain = new MockFilterChain(Mockito.mock(Servlet.class), new X509ClientRejectIfMissingFilter(authExceptionHandler), nextFilter);
    }

    @Test
    void whenClientCertMissing_thenRejectAndStop() throws ServletException, IOException {
        chain.doFilter(request, response);
        verify(authExceptionHandler, times(1))
            .handleException(any(), any(), any(InvalidCertificateException.class));
        verify(nextFilter, never()).doFilter(any(), any(), eq(chain));
    }

    @Test
    void whenClientCertPresent_thenAllowAndContinue() throws ServletException, IOException {
        request.setAttribute(ATTRNAME_CLIENT_AUTH_X509_CERTIFICATE, x509Certificate);
        chain.doFilter(request, response);
        verify(authExceptionHandler, never()).handleException(any(), any(), any());
        verify(nextFilter, times(1)).doFilter(any(), any(), eq(chain));
    }
}
