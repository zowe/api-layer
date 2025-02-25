/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsExchange;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.security.cert.Certificate;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class MainTest {

    static HttpsServer httpServer;
    static AssertionError error;

    @BeforeAll
    static void setup() throws Exception {
        InetSocketAddress inetAddress = new InetSocketAddress("127.0.0.1", 8080);
        httpServer = HttpsServer.create(inetAddress, 0);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fileInputStream = new FileInputStream("../keystore/localhost/localhost.keystore.p12")) {
            keyStore.load(fileInputStream, "password".toCharArray());
        }
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(keyStore, "password".toCharArray());
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fileInputStream = new FileInputStream("../keystore/localhost/localhost.truststore.p12")) {
            trustStore.load(fileInputStream, "password".toCharArray());
        }
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(trustStore);

        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

        TestHttpsConfigurator httpsConfigurator = new TestHttpsConfigurator(sslContext);
        httpServer.setHttpsConfigurator(httpsConfigurator);
        httpServer.createContext("/gateway/api/v1/auth/login", exchange -> {

            exchange.sendResponseHeaders(204, 0);
            Certificate[] clientCert = ((HttpsExchange) exchange).getSSLSession().getPeerCertificates();
            try {
                // client certificate must be present at this stage
                assertNotNull(clientCert);
            } catch (AssertionError e) {
                error = e;
            }
            exchange.close();
        });
        httpServer.start();

    }

    @AfterAll
    static void tearDown() {

        httpServer.stop(0);
        if (error != null) {
            throw error;
        }
    }

    @Test
    void givenHttpsRequestWithClientCertificate_thenPeerCertificateMustBeAvailable() {
        // Assertion is done on the server to make sure that client certificate was delivered.
        // Assertion error is then rethrown in the tear down method in case certificate was not present.
        Main.main(null);
    }

    static class TestHttpsConfigurator extends HttpsConfigurator {
        /**
         * Creates a Https configuration, with the given {@link SSLContext}.
         *
         * @param context the {@code SSLContext} to use for this configurator
         * @throws NullPointerException if no {@code SSLContext} supplied
         */
        public TestHttpsConfigurator(SSLContext context) {
            super(context);
        }

        @Override
        public void configure(HttpsParameters params) {
            SSLParameters parms = getSSLContext().getDefaultSSLParameters();
            parms.setNeedClientAuth(true);
            params.setWantClientAuth(true);
            params.setSSLParameters(parms);
        }
    }

}
