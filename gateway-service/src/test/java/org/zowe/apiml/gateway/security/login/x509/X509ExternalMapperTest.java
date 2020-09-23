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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class X509ExternalMapperTest {

    private X509ExternalMapper x509ExternalMapper;
    private CloseableHttpClient closeableHttpClient;
    private CloseableHttpResponse httpResponse;
    private HttpEntity entity;
    private X509Certificate x509Certificate;

    @BeforeEach
    void setup() throws IOException {
        closeableHttpClient = mock(CloseableHttpClient.class);
        httpResponse = mock(CloseableHttpResponse.class);
        when(closeableHttpClient.execute(any())).thenReturn(httpResponse);

        x509Certificate = mock(X509Certificate.class);
        entity = mock(HttpEntity.class);
    }

    @Test
    void givenValidHttpResponse_thenReturnUserId() throws CertificateEncodingException, IOException {
        x509ExternalMapper = new X509ExternalMapper(closeableHttpClient, "");
        when(x509Certificate.getEncoded()).thenReturn(new byte[2]);
        when(httpResponse.getEntity()).thenReturn(entity);
        when(entity.getContent()).thenReturn(new ByteArrayInputStream("{\"userid\":\"ZOSUSER\",\"rc\":0,\"saf_rc\":0,\"racf_rc\":0,\"reason_code\":0}".getBytes()));
        String userId = x509ExternalMapper.mapCertificateToMainframeUserId(x509Certificate);
        assertEquals("ZOSUSER", userId);
    }

}
