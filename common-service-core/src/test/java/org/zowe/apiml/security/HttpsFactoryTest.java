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

import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.security.KeyStoreException;

import static org.junit.jupiter.api.Assertions.*;

class HttpsFactoryTest {
    private static final String INCORRECT_PARAMETER_VALUE = "WRONG";

    private HttpsConfig.HttpsConfigBuilder httpsConfigBuilder;

    @BeforeEach
    void setUp() {
        httpsConfigBuilder = SecurityTestUtils.correctHttpsSettings();
    }

    @Test
    void shouldCreateSecureSslSocketFactory() {
        HttpsConfig httpsConfig = httpsConfigBuilder.build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig);
        ConnectionSocketFactory socketFactory = httpsFactory.createSslSocketFactory();
        assertEquals(SSLConnectionSocketFactory.class, socketFactory.getClass());
    }

    @Test
    void shouldCreateIgnoringSslSocketFactory() throws KeyStoreException {
        HttpsConfig httpsConfig = httpsConfigBuilder.verifySslCertificatesOfServices(false).build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig);
        ConnectionSocketFactory socketFactory = httpsFactory.createSslSocketFactory();
        assertEquals(SSLConnectionSocketFactory.class, socketFactory.getClass());
        assertFalse(httpsFactory.getUsedKeyStore().aliases().hasMoreElements());
    }

    @Test
    void shouldCreateSecureSslContextWithEmptyKeystoreWhenNoKeystoreIsProvided() throws KeyStoreException {
        HttpsConfig httpsConfig = HttpsConfig.builder().protocol("TLSv1.2").verifySslCertificatesOfServices(true).build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig);
        httpsFactory.getSslContext();
        assertFalse(httpsFactory.getUsedKeyStore().aliases().hasMoreElements());
    }

    @Test
    void shouldCreateSecureHttpClient() {
        HttpsConfig httpsConfig = httpsConfigBuilder.build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig);

        var httpClient = httpsFactory.buildHttpClient(null);
        assertEquals("org.apache.hc.client5.http.impl.classic.InternalHttpClient", httpClient.getClass().getName());
    }

    @Test
    void shouldCreateSecureSslContext() {
        HttpsConfig httpsConfig = httpsConfigBuilder.build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig);
        SSLContext sslContext = httpsFactory.getSslContext();
        assertNotNull(sslContext);
        assertEquals(SSLContext.class, sslContext.getClass());
    }

    @Test
    void shouldCreateIgnoringSslContext() {
        HttpsConfig httpsConfig = httpsConfigBuilder.verifySslCertificatesOfServices(false).build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig);
        SSLContext sslContext = httpsFactory.getSslContext();
        assertNotNull(sslContext);
        assertEquals(SSLContext.class, sslContext.getClass());
    }

    @Test
    void wrongKeyPasswordConfigurationShouldFail() {
        HttpsConfig httpsConfig = httpsConfigBuilder.keyPassword(INCORRECT_PARAMETER_VALUE.toCharArray()).build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig);
        assertThrows(HttpsConfigError.class, () -> httpsFactory.getSslContext());
    }

    @Test
    void specificIncorrectAliasShouldFail() {
        HttpsConfig httpsConfig = httpsConfigBuilder.trustStorePassword(INCORRECT_PARAMETER_VALUE.toCharArray()).build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig);
        assertThrows(HttpsConfigError.class, () -> httpsFactory.getSslContext());
    }

    @Test
    void incorrectProtocolShouldFail() {
        HttpsConfig httpsConfig = httpsConfigBuilder.verifySslCertificatesOfServices(false).protocol(INCORRECT_PARAMETER_VALUE).build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig);
        assertThrows(HttpsConfigError.class, () -> httpsFactory.getSslContext());
    }

    @Test
    void shouldCreateDefaultHostnameVerifier() {
        HttpsConfig httpsConfig = httpsConfigBuilder.build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig);
        HostnameVerifier hostnameVerifier = httpsFactory.getHostnameVerifier();
        assertEquals(DefaultHostnameVerifier.class, hostnameVerifier.getClass());
    }

    @Test
    void shouldCreateNoopHostnameVerifier() {
        HttpsConfig httpsConfig = httpsConfigBuilder.verifySslCertificatesOfServices(false).build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig);
        HostnameVerifier hostnameVerifier = httpsFactory.getHostnameVerifier();
        assertEquals(NoopHostnameVerifier.class, hostnameVerifier.getClass());
    }
}
