/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.tomcat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import javax.net.ssl.SSLHandshakeException;

import com.ca.mfaas.security.HttpsConfig;
import com.ca.mfaas.security.HttpsConfigError;
import com.ca.mfaas.security.HttpsConfigError.ErrorCode;
import com.ca.mfaas.security.HttpsFactory;
import com.ca.mfaas.security.SecurityTestUtils;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TomcatHttpsTest {
    private static final String EXPECTED_SSL_HANDSHAKE_EXCEPTION_NOT_THROWN = "excepted SSLHandshakeException exception not thrown";
    private static final String EXPECTED_HTTPS_CONFIG_ERROR_NOT_THROWN = "excepted HttpsConfigError exception not thrown";
    private static final String UNABLE_TO_FIND_CERTIFICATION_PATH_MESSAGE = "unable to find valid certification path";

    @Before
    public void setUp() {
        System.clearProperty("javax.net.ssl.keyStore");
        System.clearProperty("javax.net.ssl.keyStorePassword");
        System.clearProperty("javax.net.ssl.keyStoreType");
        System.clearProperty("javax.net.ssl.trustStore");
        System.clearProperty("javax.net.ssl.trustStorePassword");
        System.clearProperty("javax.net.ssl.trustStoreType");
    }

   @Test
    public void correctConfigurationShouldWork() throws IOException, LifecycleException {
        HttpsConfig httpsConfig = SecurityTestUtils.correctHttpsSettings().build();
        startTomcatAndDoHttpsRequest(httpsConfig);
    }

    @Test
    public void noTrustStoreShouldFail() throws IOException, LifecycleException {
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
    public void trustStoreWithDifferentCertificateAuthorityShouldFail() throws IOException, LifecycleException {
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
    public void trustStoreWithDifferentCertificateAuthorityShouldNotFailWhenCertificateValidationIsDisabled() throws IOException, LifecycleException {
        HttpsConfig httpsConfig = SecurityTestUtils.correctHttpsSettings().verifySslCertificatesOfServices(false)
                .trustStore(SecurityTestUtils.pathFromRepository("keystore/localhost/localhost2.truststore.p12")).build();
        startTomcatAndDoHttpsRequest(httpsConfig);
    }

    @Test
    public void trustStoreInInvalidFormatShouldFail() throws IOException, LifecycleException {
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
    public void wrongKeyAliasShouldFail() throws IOException, LifecycleException {
        HttpsConfig httpsConfig = SecurityTestUtils.correctHttpsKeyStoreSettings().keyAlias("wrong").build();
        try {
            startTomcatAndDoHttpsRequest(httpsConfig);
            fail(EXPECTED_HTTPS_CONFIG_ERROR_NOT_THROWN);
        } catch (HttpsConfigError e) {  // NOSONAR
            assertEquals(ErrorCode.WRONG_KEY_ALIAS, e.getCode());
        }
    }

    @Test
    public void correctConfigurationWithClientAuthenticationShouldWork() throws IOException, LifecycleException {
        HttpsConfig httpsConfig = SecurityTestUtils.correctHttpsSettings().clientAuth(true).build();
        startTomcatAndDoHttpsRequest(httpsConfig);
    }

    @Test
    public void wrongClientCertificateShouldFail() throws IOException, LifecycleException {
        HttpsConfig serverConfig = SecurityTestUtils.correctHttpsSettings().clientAuth(true).build();
        HttpsConfig clientConfig = SecurityTestUtils.correctHttpsSettings().keyStore(SecurityTestUtils.pathFromRepository("keystore/localhost/localhost2.keystore.p12")).build();
        try {
            startTomcatAndDoHttpsRequest(serverConfig, clientConfig);
            fail(EXPECTED_SSL_HANDSHAKE_EXCEPTION_NOT_THROWN);
        } catch (SSLHandshakeException e) {  // NOSONAR
            assertTrue(e.getMessage().contains("bad_certificate"));
        }
    }

    @Test
    public void wrongClientCertificateShouldNotFailWhenCertificateValidationIsDisabled() throws IOException, LifecycleException {
        HttpsConfig serverConfig = SecurityTestUtils.correctHttpsSettings().clientAuth(true).verifySslCertificatesOfServices(false).build();
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
            HttpClient client = clientHttpsFactory.createSecureHttpClient();

            int port = TomcatServerFactory.getLocalPort(tomcat);
            HttpGet get = new HttpGet(String.format("https://localhost:%d", port));
            HttpResponse response = client.execute(get);

            String responseBody = EntityUtils.toString(response.getEntity());

            assertEquals(200, response.getStatusLine().getStatusCode());
            assertEquals("OK", responseBody);
        } finally {
            tomcat.stop();
        }
    }
}
