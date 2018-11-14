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

import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import javax.net.ssl.SSLHandshakeException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.ca.mfaas.security.HttpsConfig;
import com.ca.mfaas.security.HttpsConfigError;
import com.ca.mfaas.security.HttpsFactory;
import com.ca.mfaas.security.HttpsConfigError.ErrorCode;

public class TomcatHttpsTest {
    @Test
    public void correctConfigurationShouldWork() throws IOException, LifecycleException {
        HttpsConfig httpsConfig = correctHttpsSettings().build();
        startTomcatAndDoHttpsRequest(httpsConfig);
    }

    @Test
    public void noTrustStoreShouldFail() throws IOException, LifecycleException {
        HttpsConfig httpsConfig = correctHttpsKeyStoreSettings().build();
        try {
            startTomcatAndDoHttpsRequest(httpsConfig);
            fail("excepted SSLHandshakeException message not thrown");
        } catch (SSLHandshakeException e) {
            assertTrue(e.getMessage().contains("unable to find valid certification path"));
        }
    }

    @Test
    public void trustStoreWithDifferentCertificateAuthorityShouldFail() throws IOException, LifecycleException {
        HttpsConfig httpsConfig = correctHttpsSettings()
                .trustStore(pathFromRepository("keystore/localhost/localhost2.truststore.p12")).build();
        try {
            startTomcatAndDoHttpsRequest(httpsConfig);
            fail("excepted SSLHandshakeException message not thrown");
        } catch (SSLHandshakeException e) {
            assertTrue(e.getMessage().contains("unable to find valid certification path"));
        }
    }

    @Test
    public void trustStoreInInvalidFormatShouldFail() throws IOException, LifecycleException {
        HttpsConfig httpsConfig = correctHttpsSettings()
                .trustStore(pathFromRepository("README.md")).build();
        try {
            startTomcatAndDoHttpsRequest(httpsConfig);
            fail("excepted SSLHandshakeException message not thrown");
        } catch (HttpsConfigError e) {
            assertEquals(ErrorCode.HTTP_CLIENT_INITIALIZATION_FAILED, e.getCode());
        }
    }

    @Test
    public void wrongKeyAliasShouldFail() throws IOException, LifecycleException {
        HttpsConfig httpsConfig = correctHttpsKeyStoreSettings().keyAlias("wrong").build();
        try {
            startTomcatAndDoHttpsRequest(httpsConfig);
            fail("excepted message not thrown");
        } catch (HttpsConfigError e) {
            assertEquals(ErrorCode.WRONG_KEY_ALIAS, e.getCode());
        }
    }

    private String pathFromRepository(String path) {
        try {
            return new File("../" + path).getCanonicalPath();
        } catch (IOException e) {
            throw new Error("Invalid repository path: " + path, e);
        }
    }

    private void startTomcatAndDoHttpsRequest(HttpsConfig httpsConfig) throws IOException, LifecycleException {
        Tomcat tomcat = new TomcatServerFactory().startTomcat(httpsConfig);
        try {
            HttpsFactory httpsFactory = new HttpsFactory(httpsConfig);
            HttpClient client = httpsFactory.createSecureHttpClient();

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

    private HttpsConfig.HttpsConfigBuilder correctHttpsKeyStoreSettings() throws IOException {
        return HttpsConfig.builder().protocol("TLSv1.2")
                .keyStore(pathFromRepository("keystore/localhost/localhost.keystore.p12")).keyStorePassword("password")
                .keyPassword("password");
    }

    private HttpsConfig.HttpsConfigBuilder correctHttpsSettings() throws IOException {
        return correctHttpsKeyStoreSettings()
                .trustStore(pathFromRepository("keystore/localhost/localhost.truststore.p12"))
                .trustStorePassword("password");
    }
}
