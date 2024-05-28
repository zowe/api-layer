/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.mapping;

import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.zaas.security.mapping.model.MapperResponse;
import org.zowe.apiml.zaas.security.service.TokenCreationService;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSource;
import org.zowe.apiml.zaas.security.service.schema.source.JwtAuthSource;
import org.zowe.apiml.zaas.security.service.schema.source.X509AuthSource;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class X509ExternalMapperTest {

    private X509ExternalMapper x509ExternalMapper;
    private X509Certificate x509Certificate;
    X509AuthSource x509AuthSource;

    @BeforeEach
    void setup() throws CertificateEncodingException {
        x509Certificate = mock(X509Certificate.class);
        when(x509Certificate.getEncoded()).thenReturn(new byte[2]);
        x509AuthSource = new X509AuthSource(x509Certificate);
        x509ExternalMapper = spy(new X509ExternalMapper("https://domain.com/mapper", "mapper_user", mock(CloseableHttpClient.class), mock(TokenCreationService.class), mock(AuthConfigurationProperties.class)));
    }

    @Nested
    class GivenValidMapperResponse {
        @BeforeEach
        void setup() {
            doReturn(new MapperResponse("ZOSUSER", 0, 0 ,0 ,0)).when(x509ExternalMapper).callExternalMapper(any());
        }
        @Test
        void thenReturnMappedUserId() {
            String userId = x509ExternalMapper.mapToMainframeUserId(x509AuthSource);
            assertEquals("ZOSUSER", userId);
        }
    }

    @Nested
    class GivenNullMapperResponse {
        @BeforeEach
        void setup() {
            doReturn(null).when(x509ExternalMapper).callExternalMapper(any());
        }
        @Test
        void thenNullReturned() {
            String userId = x509ExternalMapper.mapToMainframeUserId(x509AuthSource);
            assertNull(userId);
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
        void thenNullReturned() {
            String userId = x509ExternalMapper.mapToMainframeUserId(anotherSource);
            assertNull(userId);
        }
    }

    @Nested
    class GivenInvalidCertificate {
        @BeforeEach
        void setup() throws CertificateEncodingException {
            when(x509Certificate.getEncoded()).thenThrow(new CertificateEncodingException());
        }
        @Test
        void thenNullReturned() {
            String userId = x509ExternalMapper.mapToMainframeUserId(x509AuthSource);
            assertNull(userId);
        }
    }
}
