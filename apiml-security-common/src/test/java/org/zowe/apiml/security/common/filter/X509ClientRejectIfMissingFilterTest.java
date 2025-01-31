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
import org.springframework.security.core.context.SecurityContextHolder;
import org.zowe.apiml.security.common.error.AuthExceptionHandler;
import org.zowe.apiml.security.common.error.InvalidCertificateException;
import org.zowe.apiml.security.common.verify.CertificateValidator;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class X509ClientRejectIfMissingFilterTest {

    private static final String CLIENT_CERT_HEADER = "Client-Cert";
    private static final String CLIENT_CERT_HEADER_VALUE =
        "MIIEFTCCAv2gAwIBAgIEKWdbVTANBgkqhkiG9w0BAQsFADCBjDELMAkGA1UEBhMC" +
            "Q1oxDTALBgNVBAgTBEJybm8xDTALBgNVBAcTBEJybm8xFDASBgNVBAoTC1pvd2Ug" +
            "U2FtcGxlMRwwGgYDVQQLExNBUEkgTWVkaWF0aW9uIExheWVyMSswKQYDVQQDEyJa" +
            "b3dlIFNlbGYtU2lnbmVkIFVudHJ1c3RlZCBTZXJ2aWNlMB4XDTE4MTIwNzIwMDc1" +
            "MloXDTI4MTIwNDIwMDc1MlowgYwxCzAJBgNVBAYTAkNaMQ0wCwYDVQQIEwRCcm5v" +
            "MQ0wCwYDVQQHEwRCcm5vMRQwEgYDVQQKEwtab3dlIFNhbXBsZTEcMBoGA1UECxMT" +
            "QVBJIE1lZGlhdGlvbiBMYXllcjErMCkGA1UEAxMiWm93ZSBTZWxmLVNpZ25lZCBV" +
            "bnRydXN0ZWQgU2VydmljZTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEB" +
            "AJti8p4nr8ztRSbemrAv1ytVLQMbXozhLe3lNaiVADGTFPZYeJ2lDt7oAl238HOY" +
            "ScpOz+JjTeUkL0jsjNYgMhi4J07II/3sJL0SBfVqvvgjUL4BvcpdBl0crSuI/3D4" +
            "OaPue+ZmPFijwdCcw5JbazMoOka/zUwpYYdbwxPUH2BbKfwtmmygX88nkJcRSoQO" +
            "KBdNsUs+QRuUiokZ/FJi7uiOsNZ8eEfQv6qJ7mOJ7l1IrMcNm3jHgodoQi/4jXO1" +
            "np/hZaz/ZDni9kBwcyd64AViB2v7VrrBmjdESt1mtCIMvKMlwAZAqrDO75Q9pepO" +
            "Y7zbN4s9s7IUfyb9431xg2MCAwEAAaN9MHswHQYDVR0lBBYwFAYIKwYBBQUHAwIG" +
            "CCsGAQUFBwMBMA4GA1UdDwEB/wQEAwIE8DArBgNVHREEJDAighVsb2NhbGhvc3Qu" +
            "bG9jYWxkb21haW6CCWxvY2FsaG9zdDAdBgNVHQ4EFgQUIeSN7aNtwH2MnBAGDLre" +
            "TtcSaZ4wDQYJKoZIhvcNAQELBQADggEBAELPbHlG60nO164yrBjZcpQJ/2e5ThOR" +
            "8efXUWExuy/NpwVx0vJg4tb8s9NI3X4pRh3WyD0uGPGkO9w+CAvgUaECePLYjkov" +
            "KIS6Cvlcav9nWqdZau1fywltmOLu8Sq5i42Yvb7ZcPOEwDShpuq0ql7LR7j7P4XH" +
            "+JkA0k9Zi6RfYJAyOOpbD2R4JoMbxBKrxUVs7cEajl2ltckjyRWoB6FBud1IthRR" +
            "mZoPMtlCleKlsKp7yJiE13hpX+qIGnzEQE2gNgQ94dSl4m2xO6pnyDRMAEncmd33" +
            "oehy77omRxNsLzkWe6mjaC8ShMGzG9jYR02iN2h4083/PVXvTZIqwhg=";

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;
    private AuthExceptionHandler authExceptionHandler;
    private Filter nextFilter;
    private CertificateValidator certificateValidator;

    @BeforeEach
    void setUp() {
        authExceptionHandler = Mockito.mock(AuthExceptionHandler.class);
        certificateValidator = Mockito.mock(CertificateValidator.class);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        nextFilter = Mockito.mock(Filter.class);
        chain = new MockFilterChain(Mockito.mock(Servlet.class), new X509ClientRejectIfMissingFilter(certificateValidator, authExceptionHandler), nextFilter);
        SecurityContextHolder.clearContext();
    }

    @Test
    void whenClientCertMissing_thenRejectAndStop() throws ServletException, IOException {
        chain.doFilter(request, response);
        verify(authExceptionHandler, times(1))
            .handleException(any(), any(), any(InvalidCertificateException.class));
        verify(nextFilter, never()).doFilter(any(), any(), eq(chain));
        assertThat(SecurityContextHolder.getContext().getAuthentication(), is(nullValue()));
    }

    @Test
    void whenClientCertPresent_thenAllowAndContinue() throws ServletException, IOException {
        request.addHeader(CLIENT_CERT_HEADER, CLIENT_CERT_HEADER_VALUE);
        chain.doFilter(request, response);
        verify(authExceptionHandler, never()).handleException(any(), any(), any());
        verify(nextFilter, times(1)).doFilter(any(), any(), eq(chain));
        assertThat(SecurityContextHolder.getContext().getAuthentication().getCredentials(), is(notNullValue()));
    }
}
