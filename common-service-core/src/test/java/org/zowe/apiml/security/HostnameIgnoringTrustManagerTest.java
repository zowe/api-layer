/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509TrustManager;

import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HostnameIgnoringTrustManagerTest {

    private HostnameIgnoringTrustManager trustManager;

    @Mock
    private X509TrustManager delegate;

    @BeforeEach
    void setUp() {
        this.trustManager = new HostnameIgnoringTrustManager(delegate);
    }

    @Test
    void testCheckClientTrusted() throws CertificateException {
        X509Certificate[] certs = new X509Certificate[]{};
        var authType = "test";
        Socket s = mock(Socket.class);

        doNothing().when(delegate).checkClientTrusted(certs, authType);

        trustManager.checkClientTrusted(certs, authType, s);

        verify(delegate, times(1)).checkClientTrusted(certs, authType);
    }

    @Test
    void testCheckClientTrusted2() throws CertificateException {
        X509Certificate[] certs = new X509Certificate[]{};
        var authType = "test";
        SSLEngine e = mock(SSLEngine.class);

        doNothing().when(delegate).checkClientTrusted(certs, authType);

        trustManager.checkClientTrusted(certs, authType, e);

        verify(delegate, times(1)).checkClientTrusted(certs, authType);
    }

    @Test
    void testCheckServerTrusted() throws CertificateException {
        X509Certificate[] certs = new X509Certificate[]{};
        var authType = "test";
        Socket s = mock(Socket.class);

        doNothing().when(delegate).checkServerTrusted(certs, authType);

        trustManager.checkServerTrusted(certs, authType, s);

        verify(delegate, times(1)).checkServerTrusted(certs, authType);
    }

    @Test
    void testCheckServerTrusted2() throws CertificateException {
        X509Certificate[] certs = new X509Certificate[]{};
        var authType = "test";
        SSLEngine e = mock(SSLEngine.class);

        doNothing().when(delegate).checkServerTrusted(certs, authType);

        trustManager.checkServerTrusted(certs, authType, e);

        verify(delegate, times(1)).checkServerTrusted(certs, authType);
    }

    @Test
    void testGetAcceptedIssuers() {
        when(delegate.getAcceptedIssuers()).thenReturn(null);
        trustManager.getAcceptedIssuers();
        verify(delegate, times(1)).getAcceptedIssuers();
    }
}
