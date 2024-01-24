/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.tomcat;

import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.security.ApimlPoolingHttpClientConnectionManager;
import org.zowe.apiml.security.HttpsConfig;
import org.zowe.apiml.security.HttpsConfigError;
import org.zowe.apiml.security.HttpsConfigError.ErrorCode;
import org.zowe.apiml.security.HttpsFactory;
import org.zowe.apiml.security.SecurityTestUtils;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
class TomcatHttpsTest {
    private static final String EXPECTED_SSL_HANDSHAKE_EXCEPTION_NOT_THROWN = "excepted SSLHandshakeException exception not thrown";
    private static final String EXPECTED_HTTPS_CONFIG_ERROR_NOT_THROWN = "excepted HttpsConfigError exception not thrown";
    private static final String UNABLE_TO_FIND_CERTIFICATION_PATH_MESSAGE = "unable to find valid certification path";

    @BeforeEach
    void setUp() {
        System.clearProperty("javax.net.ssl.keyStore");
        System.clearProperty("javax.net.ssl.keyStorePassword");
        System.clearProperty("javax.net.ssl.keyStoreType");
        System.clearProperty("javax.net.ssl.trustStore");
        System.clearProperty("javax.net.ssl.trustStorePassword");
        System.clearProperty("javax.net.ssl.trustStoreType");
    }

    @Test
    void correctConfigurationShouldWork() throws IOException, LifecycleException {
        HttpsConfig httpsConfig = SecurityTestUtils.correctHttpsSettings().build();
        startTomcatAndDoHttpsRequest(httpsConfig);
    }

    @Test
    void noTrustStoreShouldFail() throws IOException, LifecycleException {
        HttpsConfig httpsConfig = SecurityTestUtils.correctHttpsKeyStoreSettings().build();
        try {
            startTomcatAndDoHttpsRequest(httpsConfig);
            fail(EXPECTED_SSL_HANDSHAKE_EXCEPTION_NOT_THROWN);
        } catch (SSLHandshakeException e) {  // NOSONAR
            log.info("SSLHandshakeException: {}", e, e);
            assertTrue(e.getMessage().contains(UNABLE_TO_FIND_CERTIFICATION_PATH_MESSAGE));
        }
    }

    @Test
    void trustStoreWithDifferentCertificateAuthorityShouldFail() throws IOException, LifecycleException {
        HttpsConfig httpsConfig = SecurityTestUtils.correctHttpsSettings()
            .trustStore(SecurityTestUtils.pathFromRepository("keystore/localhost/localhost2.truststore.p12")).build();
        try {
            startTomcatAndDoHttpsRequest(httpsConfig);
            fail(EXPECTED_SSL_HANDSHAKE_EXCEPTION_NOT_THROWN);
        } catch (SSLHandshakeException e) {  // NOSONAR
            assertTrue(e.getMessage().contains(UNABLE_TO_FIND_CERTIFICATION_PATH_MESSAGE));
        }
    }

    @Test
    void trustStoreWithDifferentCertificateAuthorityShouldNotFailWhenCertificateValidationIsDisabled() throws IOException, LifecycleException {
        HttpsConfig httpsConfig = SecurityTestUtils.correctHttpsSettings().verifySslCertificatesOfServices(false)
            .trustStore(SecurityTestUtils.pathFromRepository("keystore/localhost/localhost2.truststore.p12")).build();
        startTomcatAndDoHttpsRequest(httpsConfig);
    }

    @Test
    void trustStoreInInvalidFormatShouldFail() throws IOException, LifecycleException {
        HttpsConfig httpsConfig = SecurityTestUtils.correctHttpsSettings()
            .trustStore(SecurityTestUtils.pathFromRepository("README.md")).build();
        try {
            startTomcatAndDoHttpsRequest(httpsConfig);
            fail(EXPECTED_HTTPS_CONFIG_ERROR_NOT_THROWN);
        } catch (HttpsConfigError e) {  // NOSONAR
            assertEquals(ErrorCode.HTTP_CLIENT_INITIALIZATION_FAILED, e.getCode());
        }
    }

    @Test
    void wrongKeyAliasShouldFail() throws IOException, LifecycleException {
        HttpsConfig httpsConfig = SecurityTestUtils.correctHttpsKeyStoreSettings().keyAlias("wrong").build();
        try {
            startTomcatAndDoHttpsRequest(httpsConfig);
            fail(EXPECTED_HTTPS_CONFIG_ERROR_NOT_THROWN);
        } catch (HttpsConfigError e) {  // NOSONAR
            assertEquals(ErrorCode.WRONG_KEY_ALIAS, e.getCode());
        }
    }

    @Test
    void correctConfigurationWithClientAuthenticationShouldWork() throws IOException, LifecycleException {
        HttpsConfig httpsConfig = SecurityTestUtils.correctHttpsSettings().clientAuth("true").build();
        startTomcatAndDoHttpsRequest(httpsConfig);
    }

    @Test
    void wrongClientCertificateShouldNotFailWhenClientAuthIsWant() throws Exception {
        HttpsConfig serverConfig = SecurityTestUtils.correctHttpsSettings().clientAuth("want").build();
        HttpsConfig clientConfig = SecurityTestUtils.correctHttpsSettings().keyStore(SecurityTestUtils.pathFromRepository("keystore/localhost/localhost2.keystore.p12")).build();

        startTomcatAndDoHttpsRequest(serverConfig, clientConfig);

    }

    @Test
    void wrongClientCertificateShouldNotFailWhenCertificateValidationIsDisabled() throws IOException, LifecycleException {
        HttpsConfig serverConfig = SecurityTestUtils.correctHttpsSettings().clientAuth("true").verifySslCertificatesOfServices(false).build();
        HttpsConfig clientConfig = SecurityTestUtils.correctHttpsSettings().keyStore(SecurityTestUtils.pathFromRepository("keystore/localhost/localhost2.keystore.p12")).build();
        startTomcatAndDoHttpsRequest(serverConfig, clientConfig);
    }

    private void startTomcatAndDoHttpsRequest(HttpsConfig config) throws IOException, LifecycleException {
        startTomcatAndDoHttpsRequest(config, config);
    }

    private void startTomcatAndDoHttpsRequest(HttpsConfig serverConfig, HttpsConfig clientConfig) throws IOException, LifecycleException {
        Tomcat tomcat = new TomcatServerFactory().startTomcat(serverConfig);
        try {
            HttpsFactory clientHttpsFactory = new HttpsFactory(clientConfig);
            RegistryBuilder<ConnectionSocketFactory> socketFactoryRegistryBuilder = RegistryBuilder
                .<ConnectionSocketFactory>create().register("http", PlainConnectionSocketFactory.getSocketFactory());
            socketFactoryRegistryBuilder.register("https", clientHttpsFactory.createSslSocketFactory());
            Registry<ConnectionSocketFactory> socketFactoryRegistry = socketFactoryRegistryBuilder.build();
            ApimlPoolingHttpClientConnectionManager connectionManager = new ApimlPoolingHttpClientConnectionManager(socketFactoryRegistry, clientConfig.getTimeToLive());
            var client = clientHttpsFactory.createSecureHttpClient(connectionManager);
            int port = TomcatServerFactory.getLocalPort(tomcat);

            var get = new HttpGet(String.format("https://localhost:%d", port));
            var response = client.execute(get, response1 -> response1);

            String responseBody = EntityUtils.toString(response.getEntity());

            assertEquals(200, response.getCode());
            assertEquals("OK", responseBody);
        } catch (ParseException e) {
            log.error("Exception while parsing HTTP response with message: " + e.getMessage(), e);
        } finally {
            tomcat.stop();
        }
    }
}
