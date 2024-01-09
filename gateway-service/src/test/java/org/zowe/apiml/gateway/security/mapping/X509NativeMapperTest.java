/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.mapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.JwtAuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.X509AuthSource;
import org.zowe.commons.usermap.CertificateResponse;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class X509NativeMapperTest {

    private static final byte[] CERT_BYTES = "certificate".getBytes();
    private static final String MF_ID = "mf_user";
    private X509Certificate x509Certificate;
    private X509AuthSource authSource;
    private X509NativeMapper x509NativeMapper;
    private NativeMapperWrapper mockMapper;

    @BeforeEach
    void setUp() throws CertificateEncodingException {
        x509Certificate = mock(X509Certificate.class);
        when(x509Certificate.getEncoded()).thenReturn(CERT_BYTES);
        authSource = new X509AuthSource(x509Certificate);
        mockMapper = mock(NativeMapperWrapper.class);
        x509NativeMapper = new X509NativeMapper(mockMapper);
    }

    @Nested
    class GivenCertificateMappingExists {
        @BeforeEach
        void setup() {
            when(mockMapper.getUserIDForCertificate(CERT_BYTES)).thenReturn(new CertificateResponse(MF_ID, 0, 0, 0));
        }

        @Test
        void thenReturnMappedUserId() {
            String userId = x509NativeMapper.mapToMainframeUserId(authSource);
            assertEquals(MF_ID, userId);
        }
    }

    @Nested
    class GivenNoMappingExists {
        @BeforeEach
        void setup() {
            when(mockMapper.getUserIDForCertificate(CERT_BYTES)).thenReturn(new CertificateResponse("", 8, 8, 8));
        }

        @Test
        void thenNullIsReturned() {
            String userId = x509NativeMapper.mapToMainframeUserId(authSource);
            assertNull(userId);
            verify(mockMapper, times(1)).getUserIDForCertificate(any());
        }
    }

    @Nested
    class GivenUnsupportedAuthSource {
        AuthSource anotherSource;
        @BeforeEach
        void setup() {
            anotherSource = new JwtAuthSource("jwt");
        }

        @Test
        void thenNullIsReturned() {
            String userId = x509NativeMapper.mapToMainframeUserId(anotherSource);
            assertNull(userId);
            verifyNoInteractions(mockMapper);
        }
    }

    @Nested
    class GivenInvalidCertificate {
        @BeforeEach
        void setup() throws CertificateEncodingException {
            when(x509Certificate.getEncoded()).thenThrow(new CertificateEncodingException("test exception"));
        }
        @Test
        void thenNullIsReturned() {
            String userId = x509NativeMapper.mapToMainframeUserId(authSource);
            assertNull(userId);
            verifyNoInteractions(mockMapper);
        }
    }
}
