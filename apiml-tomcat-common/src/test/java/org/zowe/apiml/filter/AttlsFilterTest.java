/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.tomcat.util.codec.binary.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.zowe.commons.attls.ContextIsNotInitializedException;

import java.io.IOException;
import java.security.cert.CertificateException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class AttlsFilterTest {

    @Test
    void providedCertificateInCorrectFormat_thenPopulateRequest() throws CertificateException, ContextIsNotInitializedException {
        AttlsFilter attlsFilter = new AttlsFilter();
        String certificate = """
            MIID8TCCAtmgAwIBAgIUVyBCWfHF/ZwZKVsBEpTNIBj9mQcwDQYJKoZIhvcNAQEL
            BQAwfzELMAkGA1UEBhMCQ1oxDzANBgNVBAgMBlByYWd1ZTEPMA0GA1UEBwwGUHJh
            Z3VlMREwDwYDVQQKDAhCcm9hZGNvbTEMMAoGA1UECwwDTUZEMS0wKwYDVQQDDCRB
            UElNTCBFeHRlcm5hbCBDZXJ0aWZpY2F0ZSBBdXRob3JpdHkwHhcNMjAwOTA0MTE0
            MzM2WhcNMjMwNjAxMTE0MzM2WjBiMQswCQYDVQQGEwJDWjEQMA4GA1UECAwHQ3pl
            Y2hpYTEPMA0GA1UEBwwGUHJhZ3VlMREwDwYDVQQKDAhCcm9hZGNvbTEOMAwGA1UE
            CwwFQ0EgQ1oxDTALBgNVBAMMBFVTRVIwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAw
            ggEKAoIBAQDN0NIJjuRJPE43CRvIVEg2hfIUWpos6lNs4ZNEyCxOPU0b6kyNxpTm
            aSX8LUX0JQ9c5N1Yie6F7k2JJzfkhoHh/x67CsoHYvaV60gJGuhO1PPM/QsGFrXH
            7Po0fS5jsqmJWnn+B8mUoNWFSqKUuusyuMT+Y8d8cr67g4MmnA7YEDag7F7i2s7x
            yrBMiU5IcLChMmWsZiar/vl0ykDb5Fsjt8pCFAPeuwT+nLUxCcqY5N5t11qxuS2a
            roZvM2PHdVkCQagB9dKlIYEtJeD6ZkYS0C/CHiJBqujx9dxAate/WJc5r6rdCkfB
            RGN0nZQaE7AupgDi4BqoZQzbeyU+DRutAgMBAAGjgYEwfzALBgNVHQ8EBAMCBeAw
            HQYDVR0lBBYwFAYIKwYBBQUHAwIGCCsGAQUFBwMBMB0GA1UdDgQWBBRbQ4fcpM8i
            paU2mwI4Fl/GRfNy/TARBglghkgBhvhCAQEEBAMCBeAwHwYDVR0jBBgwFoAUcYHv
            14ClCeqgaHg5n4LYjlmgj3cwDQYJKoZIhvcNAQELBQADggEBANMyHteCcktXSloB
            w3BrCYe0mup+BKe9lT2ARVoCuamxj/VqKq6IbNUBNqvIIbEK5wIEiSPvQ6VJqmks
            yw+gr+Dch+2sd9jiCKYAPTeDeoBHzRo88j4L7y+w/FN+13y3QIUxSfzEdrcsSA5Z
            VwTQsFF5zC6u2k7onDlE6NiYnuU5VUaM4jel1atSeA6xLdD+ePfI70B+m034Nhrk
            aUUTK+iCCeM9heokpWqpA9bqlHcIP0fliG6Estnt9xaPwA4VpLkQR6t9E49yBDJ5
            C0lqInPB7xGphhYZ6cV9/c3u/B3r9iK1IHQffhdANStogiQrzGk2yvUg0/t0qmle
            PzYuxfM=
            """.stripIndent();

        HttpServletRequest request = new MockHttpServletRequest();
        attlsFilter.populateRequestWithCertificate(request, Base64.decodeBase64(certificate));
        assertNotNull(request.getAttribute("jakarta.servlet.request.X509Certificate"));
    }

    @Test
    void whenExceptionOccurs_thenCreateCorrectResponse() throws ServletException, IOException {
        AttlsFilter filter = new AttlsFilter();
        FilterChain chain = mock(FilterChain.class);
        HttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(new MockHttpServletRequest(), response, chain);
        assertEquals(500, response.getStatus());
    }

}
