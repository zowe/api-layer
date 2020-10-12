/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.login.x509;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.gateway.security.service.TokenCreationService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class X509ExternalMapperTest {

    private X509ExternalMapper x509ExternalMapper;
    private TokenCreationService tokenCreationService;

    private CloseableHttpClient closeableHttpClient;
    private CloseableHttpResponse httpResponse;
    private HttpEntity entity;
    private X509Certificate x509Certificate;
    private static final String CLIENT_AUTH_OID = "1.3.6.1.5.5.7.3.2";

    @BeforeEach
    void setup() throws IOException {
        closeableHttpClient = mock(CloseableHttpClient.class);
        httpResponse = mock(CloseableHttpResponse.class);
        when(closeableHttpClient.execute(any())).thenReturn(httpResponse);
        tokenCreationService = mock(TokenCreationService.class);
        when(tokenCreationService.createJwtTokenWithoutCredentials(anyString())).thenReturn("validJwtToken");

        x509Certificate = mock(X509Certificate.class);
        entity = mock(HttpEntity.class);
    }

    @Test
    void givenValidHttpResponse_thenReturnUserId() throws CertificateEncodingException, IOException, CertificateParsingException {
        x509ExternalMapper = new X509ExternalMapper(closeableHttpClient, tokenCreationService);
        ReflectionTestUtils.setField(x509ExternalMapper,"externalMapperUrl","");
        when(x509Certificate.getExtendedKeyUsage()).thenReturn(Collections.singletonList(CLIENT_AUTH_OID));
        when(x509Certificate.getEncoded()).thenReturn(new byte[2]);
        when(httpResponse.getEntity()).thenReturn(entity);
        when(entity.getContent()).thenReturn(new ByteArrayInputStream("{\"userid\":\"ZOSUSER\",\"returnCode\":0,\"safReturnCode\":0,\"racfReturnCode\":0,\"racfReasonCode\":0}".getBytes()));
        String userId = x509ExternalMapper.mapCertificateToMainframeUserId(x509Certificate);
        assertEquals("ZOSUSER", userId);
    }

    @Test
    void givenValidHttpResponse_andUserIsNotFound_thenReturnNull() throws CertificateEncodingException, IOException, CertificateParsingException {
        x509ExternalMapper = new X509ExternalMapper(closeableHttpClient, tokenCreationService);
        ReflectionTestUtils.setField(x509ExternalMapper,"externalMapperUrl","");
        when(x509Certificate.getExtendedKeyUsage()).thenReturn(Collections.singletonList(CLIENT_AUTH_OID));
        when(x509Certificate.getEncoded()).thenReturn(new byte[2]);
        when(httpResponse.getEntity()).thenReturn(entity);
        when(entity.getContent()).thenReturn(new ByteArrayInputStream("".getBytes()));
        String userId = x509ExternalMapper.mapCertificateToMainframeUserId(x509Certificate);
        assertNull(userId);
    }

    @Test
    void givenInvalidUriCharacters_thenNullIsReturned_andExceptionIsHandled() throws CertificateParsingException {
        when(x509Certificate.getExtendedKeyUsage()).thenReturn(Collections.singletonList(CLIENT_AUTH_OID));
        x509ExternalMapper = new X509ExternalMapper(closeableHttpClient, tokenCreationService);
        ReflectionTestUtils.setField(x509ExternalMapper,"externalMapperUrl","%");
        assertNull(x509ExternalMapper.mapCertificateToMainframeUserId(x509Certificate));
    }

    @Test
    void givenInvalidCertificateEncodedData_thenNullIsReturned_andExceptionIsHandled() throws CertificateEncodingException, CertificateParsingException {
        when(x509Certificate.getExtendedKeyUsage()).thenReturn(Collections.singletonList(CLIENT_AUTH_OID));
        x509ExternalMapper = new X509ExternalMapper(closeableHttpClient, tokenCreationService);
        ReflectionTestUtils.setField(x509ExternalMapper,"externalMapperUrl","");
        when(x509Certificate.getEncoded()).thenThrow(new CertificateEncodingException());
        assertNull(x509ExternalMapper.mapCertificateToMainframeUserId(x509Certificate));
    }

}
