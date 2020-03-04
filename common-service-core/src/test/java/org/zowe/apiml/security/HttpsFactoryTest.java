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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.security.KeyStoreException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import com.netflix.discovery.shared.transport.jersey.EurekaJerseyClientImpl;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.junit.Before;
import org.junit.Test;

public class HttpsFactoryTest {
    private static final String EUREKA_URL_NO_SCHEME = "://localhost:10011/eureka/";
    private static final String TEST_SERVICE_ID = "service1";
    private static final String INCORRECT_PARAMETER_VALUE = "WRONG";

    private HttpsConfig.HttpsConfigBuilder httpsConfigBuilder;

    @Before
    public void setUp() {
        httpsConfigBuilder = SecurityTestUtils.correctHttpsSettings();
    }

    @Test
    public void shouldCreateSecureSslSocketFactory() {
        HttpsConfig httpsConfig = httpsConfigBuilder.build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig);
        ConnectionSocketFactory socketFactory = httpsFactory.createSslSocketFactory();
        assertEquals(SSLConnectionSocketFactory.class, socketFactory.getClass());
    }

    @Test
    public void shouldCreateIgnoringSslSocketFactory() throws KeyStoreException {
        HttpsConfig httpsConfig = httpsConfigBuilder.verifySslCertificatesOfServices(false).build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig);
        ConnectionSocketFactory socketFactory = httpsFactory.createSslSocketFactory();
        assertEquals(SSLConnectionSocketFactory.class, socketFactory.getClass());
        assertFalse(httpsFactory.getUsedKeyStore().aliases().hasMoreElements());
    }

    @Test
    public void shouldCreateSecureSslContextWithEmptyKeystoreWhenNoKeystoreIsProvided() throws KeyStoreException {
        HttpsConfig httpsConfig = HttpsConfig.builder().protocol("TLSv1.2").verifySslCertificatesOfServices(true).build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig);
        httpsFactory.createSslContext();
        assertFalse(httpsFactory.getUsedKeyStore().aliases().hasMoreElements());
    }

    @Test
    public void shouldCreateSecureHttpClient() {
        HttpsConfig httpsConfig = httpsConfigBuilder.build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig);
        HttpClient httpClient = httpsFactory.createSecureHttpClient();
        assertEquals("org.apache.http.impl.client.InternalHttpClient", httpClient.getClass().getName());
    }

    @Test
    public void shouldCreateSecureSslContext() {
        HttpsConfig httpsConfig = httpsConfigBuilder.build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig);
        SSLContext sslContext = httpsFactory.createSslContext();
        assertNotNull(sslContext);
        assertEquals(SSLContext.class, sslContext.getClass());
    }

    @Test
    public void shouldCreateIgnoringSslContext() {
        HttpsConfig httpsConfig = httpsConfigBuilder.verifySslCertificatesOfServices(false).build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig);
        SSLContext sslContext = httpsFactory.createSslContext();
        assertNotNull(sslContext);
        assertEquals(SSLContext.class, sslContext.getClass());
    }

    @Test(expected = HttpsConfigError.class)
    public void wrongKeyPasswordConfigurationShouldFail() {
        HttpsConfig httpsConfig = httpsConfigBuilder.keyPassword(INCORRECT_PARAMETER_VALUE).build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig);
        SSLContext sslContext = httpsFactory.createSslContext();
        assertNull(sslContext);
    }

    @Test(expected = HttpsConfigError.class)
    public void specificIncorrectAliasShouldFail() {
        HttpsConfig httpsConfig = httpsConfigBuilder.trustStorePassword(INCORRECT_PARAMETER_VALUE).build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig);
        SSLContext sslContext = httpsFactory.createSslContext();
        assertNull(sslContext);
    }

    @Test(expected = HttpsConfigError.class)
    public void incorrectProtocolShouldFail() {
        HttpsConfig httpsConfig = httpsConfigBuilder.verifySslCertificatesOfServices(false).protocol(INCORRECT_PARAMETER_VALUE).build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig);
        SSLContext sslContext = httpsFactory.createSslContext();
        assertNull(sslContext);
    }

    @Test
    public void shouldSetSystemSslProperties() {
        HttpsConfig httpsConfig = httpsConfigBuilder.build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig);
        httpsFactory.setSystemSslProperties();

        assertEquals(SecurityUtils.replaceFourSlashes(httpsConfig.getKeyStore()), System.getProperty("javax.net.ssl.keyStore"));
        assertEquals(httpsConfig.getKeyStorePassword(), System.getProperty("javax.net.ssl.keyStorePassword"));
        assertEquals(httpsConfig.getKeyStoreType(), System.getProperty("javax.net.ssl.keyStoreType"));

        assertEquals(SecurityUtils.replaceFourSlashes(httpsConfig.getTrustStore()), System.getProperty("javax.net.ssl.trustStore"));
        assertEquals(httpsConfig.getTrustStorePassword(), System.getProperty("javax.net.ssl.trustStorePassword"));
        assertEquals(httpsConfig.getTrustStoreType(), System.getProperty("javax.net.ssl.trustStoreType"));
    }

    @Test
    public void shouldCreateDefaultHostnameVerifier() {
        HttpsConfig httpsConfig = httpsConfigBuilder.build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig);
        HostnameVerifier hostnameVerifier = httpsFactory.createHostnameVerifier();
        assertEquals(DefaultHostnameVerifier.class, hostnameVerifier.getClass());
    }

    @Test
    public void shouldCreateNoopHostnameVerifier() {
        HttpsConfig httpsConfig = httpsConfigBuilder.verifySslCertificatesOfServices(false).build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig);
        HostnameVerifier hostnameVerifier = httpsFactory.createHostnameVerifier();
        assertEquals(NoopHostnameVerifier.class, hostnameVerifier.getClass());
    }

    @Test
    public void shouldCreateEurekaJerseyClientBuilderForHttps() {
        HttpsConfig httpsConfig = httpsConfigBuilder.build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig);
        EurekaJerseyClientImpl.EurekaJerseyClientBuilder clientBuilder =
            httpsFactory.createEurekaJerseyClientBuilder("https" + EUREKA_URL_NO_SCHEME, TEST_SERVICE_ID);
        assertNotNull(clientBuilder);
    }

    @Test
    public void shouldCreateEurekaJerseyClientBuilderForHttp() {
        HttpsConfig httpsConfig = httpsConfigBuilder.build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig);
        EurekaJerseyClientImpl.EurekaJerseyClientBuilder clientBuilder =
            httpsFactory.createEurekaJerseyClientBuilder("http" + EUREKA_URL_NO_SCHEME, TEST_SERVICE_ID);
        assertNotNull(clientBuilder);
    }
}
