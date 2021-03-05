/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.util.config;

import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.ssl.PrivateKeyDetails;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.util.ResourceUtils;

import javax.net.ssl.SSLContext;
import java.net.Socket;
import java.security.cert.X509Certificate;
import java.util.Map;

public class SslContext {

    public static final char[] KEYSTORE_PASSWORD = ConfigReader.environmentConfiguration().getTlsConfiguration().getKeyStorePassword();
    public static final String KEYSTORE_LOCALHOST_TEST_JKS = ConfigReader.environmentConfiguration().getTlsConfiguration().getClientKeystore();

    public static RestAssuredConfig clientCertValid;
    public static RestAssuredConfig clientCertApiml;
    public static RestAssuredConfig clientCertUser;
    public static RestAssuredConfig clientCertUnknownUser;
    public static RestAssuredConfig tlsWithoutCert;

    public static void prepareSslAuthentication() throws Exception {
        TrustStrategy trustStrategy = (X509Certificate[] chain, String authType) -> true;

        SSLContext sslContext = SSLContextBuilder
            .create()
            .loadKeyMaterial(ResourceUtils.getFile(KEYSTORE_LOCALHOST_TEST_JKS),
                KEYSTORE_PASSWORD, KEYSTORE_PASSWORD,
                (Map<String, PrivateKeyDetails> aliases, Socket socket) -> "apimtst")
            .loadTrustMaterial(null, trustStrategy)
            .build();
        clientCertValid = RestAssuredConfig.newConfig().sslConfig(new SSLConfig().sslSocketFactory(new SSLSocketFactory(sslContext)));


        SSLContext sslContext2 = SSLContextBuilder
            .create()
            .loadKeyMaterial(ResourceUtils.getFile(ConfigReader.environmentConfiguration().getTlsConfiguration().getKeyStore()),
                KEYSTORE_PASSWORD, KEYSTORE_PASSWORD)
            .loadTrustMaterial(null, trustStrategy)
            .build();
        clientCertApiml = RestAssuredConfig.newConfig().sslConfig(new SSLConfig().sslSocketFactory(new SSLSocketFactory(sslContext2)));

        SSLContext sslContext3 = SSLContextBuilder
            .create()
            .loadTrustMaterial(null, trustStrategy)
            .build();
        tlsWithoutCert = RestAssuredConfig.newConfig().sslConfig(new SSLConfig().sslSocketFactory(new SSLSocketFactory(sslContext3)));
        SSLContext sslContext4 = SSLContextBuilder
            .create()
            .loadKeyMaterial(ResourceUtils.getFile(KEYSTORE_LOCALHOST_TEST_JKS),
                KEYSTORE_PASSWORD, KEYSTORE_PASSWORD,
                (Map<String, PrivateKeyDetails> aliases, Socket socket) -> "unknownuser")
            .loadTrustMaterial(null, trustStrategy)
            .build();
        clientCertUnknownUser = RestAssuredConfig.newConfig().sslConfig(new SSLConfig().sslSocketFactory(new SSLSocketFactory(sslContext4)));

        SSLContext sslContext5 = SSLContextBuilder
            .create()
            .loadKeyMaterial(ResourceUtils.getFile(KEYSTORE_LOCALHOST_TEST_JKS),
                KEYSTORE_PASSWORD, KEYSTORE_PASSWORD,
                (Map<String, PrivateKeyDetails> aliases, Socket socket) -> "user")
            .loadTrustMaterial(null, trustStrategy)
            .build();
        clientCertUser = RestAssuredConfig.newConfig().sslConfig(new SSLConfig().sslSocketFactory(new SSLSocketFactory(sslContext5)));
    }
}
