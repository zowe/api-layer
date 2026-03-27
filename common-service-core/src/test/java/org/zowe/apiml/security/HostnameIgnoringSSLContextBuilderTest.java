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
import org.mockito.junit.jupiter.MockitoExtension;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.security.KeyManagementException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class HostnameIgnoringSSLContextBuilderTest {

    private HostnameIgnoringSSLContextBuilder builder;

    @BeforeEach
    void setUp() {
        this.builder = HostnameIgnoringSSLContextBuilder.create();
    }

    @Test
    void testInitSSLContext_withX509Trust() throws KeyManagementException {
        var sslContext = mock(SSLContext.class);
        var secureRandom = mock(SecureRandom.class);
        Collection<KeyManager> km = new ArrayList<>();
        Collection<TrustManager> tm = new ArrayList<>();

        var x509tm = mock(X509TrustManager.class);
        km.add(mock(KeyManager.class));
        tm.add(x509tm);

        doNothing().when(sslContext).init((KeyManager[]) eq(km.toArray()), argThat(t -> {
            TrustManager[] trustManagers = t;
            assertNotNull(trustManagers);
            assertEquals(1, trustManagers.length);
            assertTrue(trustManagers[0] instanceof HostnameIgnoringTrustManager);
            return true;
        }), any());

        builder.initSSLContext(sslContext, km, tm, secureRandom);
    }

    @Test
    void testInitSSLContext_withoutX509Trust() throws KeyManagementException {
        var sslContext = mock(SSLContext.class);
        var secureRandom = mock(SecureRandom.class);
        Collection<KeyManager> km = new ArrayList<>();
        Collection<TrustManager> tm = new ArrayList<>();

        var nonX509tm = mock(TrustManager.class);
        km.add(mock(KeyManager.class));
        tm.add(nonX509tm);

        doNothing().when(sslContext).init((KeyManager[]) eq(km.toArray()), argThat(t -> {
            TrustManager[] trustManagers = t;
            assertNotNull(trustManagers);
            assertEquals(1, trustManagers.length);
            assertSame(nonX509tm, trustManagers[0]);
            return true;
        }), any());

        builder.initSSLContext(sslContext, km, tm, secureRandom);
    }

}
