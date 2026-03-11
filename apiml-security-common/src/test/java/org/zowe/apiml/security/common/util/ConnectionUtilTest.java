/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.util;

import io.netty.handler.ssl.SslContextBuilder;
import io.netty.resolver.DefaultAddressResolverGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.product.web.HttpConfig;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConnectionUtilTest {

    @Mock private HttpConfig httpConfig;
    @Mock private SslContextBuilder builder;

    @BeforeEach
    void setUp() {
        when(httpConfig.getTrustStoreType()).thenReturn("PKCS12");
        when(httpConfig.getTrustStorePath()).thenReturn("../keystore/localhost/localhost.truststore.p12");
        when(httpConfig.getTrustStorePassword()).thenReturn("password".toCharArray()); //NOSONAR
        when(httpConfig.isVerifySslCertificatesOfServices()).thenReturn(true);
    }

    @Test
    void onGetSslContextWithKeystore_thenUse() throws UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        when(httpConfig.getKeyStoreType()).thenReturn("PKCS12");
        when(httpConfig.getKeyStorePath()).thenReturn("../keystore/localhost/localhost.keystore.p12");
        when(httpConfig.getKeyStorePassword()).thenReturn("password".toCharArray()); //NOSONAR

        try (MockedStatic<SslContextBuilder> sslContextBuilder = Mockito.mockStatic(SslContextBuilder.class)) {
            sslContextBuilder.when(SslContextBuilder::forClient).thenReturn(builder);

            ConnectionUtil.getSslContext(httpConfig, true);

            verify(builder, times(1)).trustManager(any(TrustManagerFactory.class));
            verify(builder, never()).endpointIdentificationAlgorithm(any());
            verify(builder, times(1)).keyManager(any(X509KeyManager.class));
            verify(builder, times(1)).keyManager((X509KeyManager) argThat(x509KeyManager -> {
                var m = (X509KeyManager) x509KeyManager;
                assertNotNull(m.getPrivateKey("localhost"));
                assertTrue(m.getCertificateChain("localhost").length > 0);
                return true;
            }));

        }

    }

    @Test
    void onGetSslContextWithoutKeystore_thenEmpty() throws UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        try (MockedStatic<SslContextBuilder> sslContextBuilder = Mockito.mockStatic(SslContextBuilder.class)) {
            sslContextBuilder.when(SslContextBuilder::forClient).thenReturn(builder);

            ConnectionUtil.getSslContext(httpConfig, false);

            verify(builder, times(1)).trustManager(any(TrustManagerFactory.class));
            verify(builder, never()).endpointIdentificationAlgorithm(any());
            verify(builder, times(1)).keyManager(any(KeyManagerFactory.class));
            verify(builder, times(1)).keyManager((KeyManagerFactory) argThat(keyManagerFactory -> {
                var f = (KeyManagerFactory) keyManagerFactory;
                assertTrue(f.getKeyManagers().length > 0);
                return true;
            }));

        }

    }

    @Test
    void whenNonStrict_thenDisableEndpointIdentificationAlgorithm() throws UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        when(httpConfig.isVerifySslCertificatesOfServices()).thenReturn(true);
        when(httpConfig.isNonStrictVerifySslCertificatesOfServices()).thenReturn(true);

        try (MockedStatic<SslContextBuilder> sslContextBuilder = Mockito.mockStatic(SslContextBuilder.class)) {
            sslContextBuilder.when(SslContextBuilder::forClient).thenReturn(builder);

            ConnectionUtil.getSslContext(httpConfig, false);

            verify(builder, times(1)).trustManager(any(TrustManagerFactory.class));
            verify(builder, times(1)).endpointIdentificationAlgorithm(isNull());
        }

    }

    @Nested
    class WhenGetHttpClient {

        @Test
        void whenHostnameVerificationEnabled_thenEnableEndpointIdentificationAlgorithm() throws UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
            when(httpConfig.isVerifySslCertificatesOfServices()).thenReturn(true);
            when(httpConfig.isNonStrictVerifySslCertificatesOfServices()).thenReturn(false);
            var baseHttpClient = HttpClient.create();
            var result = ConnectionUtil.getHttpClient(httpConfig, baseHttpClient, false);

            var resolver = result.configuration().resolver();

            assertEquals(DefaultAddressResolverGroup.INSTANCE, resolver);
            assertEquals("HTTPS", ReflectionTestUtils.getField(result.configuration().sslProvider().getSslContext(), "endpointIdentificationAlgorithm"));
        }

        @Test
        void whenUseClientCertTrueAndHostnameVerificationStrict_thenEnableEndpointIdentificationAlgorithm() throws UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
            when(httpConfig.isVerifySslCertificatesOfServices()).thenReturn(true);
            when(httpConfig.isNonStrictVerifySslCertificatesOfServices()).thenReturn(false);
            when(httpConfig.getKeyStoreType()).thenReturn("PKCS12");
            when(httpConfig.getKeyStorePath()).thenReturn("../keystore/localhost/localhost.keystore.p12");
            when(httpConfig.getKeyStorePassword()).thenReturn("password".toCharArray()); //NOSONAR

            var baseHttpClient = HttpClient.create();
            var result = ConnectionUtil.getHttpClient(httpConfig, baseHttpClient, true);

            assertEquals("HTTPS", ReflectionTestUtils.getField(result.configuration().sslProvider().getSslContext(), "endpointIdentificationAlgorithm"));
        }

        @Test
        void whenHostnameVerificationDisabled_thenDisableEndpointIdentificationAlgorithm() throws UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
            when(httpConfig.isVerifySslCertificatesOfServices()).thenReturn(true);
            when(httpConfig.isNonStrictVerifySslCertificatesOfServices()).thenReturn(true);

            var baseHttpClient = HttpClient.create();
            var result = ConnectionUtil.getHttpClient(httpConfig, baseHttpClient, false);

            assertNull(ReflectionTestUtils.getField(result.configuration().sslProvider().getSslContext(), "endpointIdentificationAlgorithm"));
        }

        @Test
        void whenVerifySslDisabled_thenDisableEndpointIdentificationAlgorithm() throws UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
            when(httpConfig.isVerifySslCertificatesOfServices()).thenReturn(false);
            when(httpConfig.isNonStrictVerifySslCertificatesOfServices()).thenReturn(false);

            var baseHttpClient = HttpClient.create();
            var result = ConnectionUtil.getHttpClient(httpConfig, baseHttpClient, false);

            assertNull(ReflectionTestUtils.getField(result.configuration().sslProvider().getSslContext(), "endpointIdentificationAlgorithm"));
        }

    }

}
