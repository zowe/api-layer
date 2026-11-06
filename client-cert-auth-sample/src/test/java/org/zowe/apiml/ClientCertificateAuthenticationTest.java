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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.security.cert.Certificate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.zowe.apiml.ClientCertificateAuthentication.API_URL;
import static org.zowe.apiml.ClientCertificateAuthentication.CLIENT_CERT_ALIAS;
import static org.zowe.apiml.ClientCertificateAuthentication.CLIENT_CERT_PASSWORD;
import static org.zowe.apiml.ClientCertificateAuthentication.CLIENT_CERT_PATH;
import static org.zowe.apiml.ClientCertificateAuthentication.PRIVATE_KEY_ALIAS;

@ExtendWith(MockitoExtension.class)
class ClientCertificateAuthenticationTest {

    private static HttpsServer httpServer;
    private static AssertionError error;

    @Spy
    private ClientCertificateAuthentication clientCertificateAuthentication;

    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;

    @BeforeAll
    static void setup() throws Exception {
        // Assertion is done on the server to make sure that client certificate was delivered.
        InetSocketAddress inetAddress = new InetSocketAddress("127.0.0.1", 10010);
        httpServer = HttpsServer.create(inetAddress, 0);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fileInputStream = new FileInputStream("../keystore/service/service.keystore.p12")) {
            keyStore.load(fileInputStream, "password".toCharArray());
        }
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(keyStore, "password".toCharArray());
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fileInputStream = new FileInputStream("../keystore/service/service.truststore.p12")) {
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
                // Client certificate must be present at this stage
                Assertions.assertNotNull(clientCert);
            } catch (AssertionError e) {
                error = e;
            }
            exchange.close();
        });
        httpServer.start();
    }

    @BeforeEach
    void setupStreams() {
        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        error = null;
    }

    @AfterAll
    static void tearDown() {
        httpServer.stop(0);
    }

    @Test
    void givenNoParameters_thenValidateDefaultValues() {
        assertEquals("https://localhost:10010/gateway/api/v1/auth/login", clientCertificateAuthentication.getApiUrl());
        assertEquals("../keystore/client/client-certs.p12", clientCertificateAuthentication.getClientCertPath());
        assertEquals("password", clientCertificateAuthentication.getClientCertPassword());
        assertEquals("user", clientCertificateAuthentication.getClientCertAlias());
        assertEquals("user", clientCertificateAuthentication.getPrivateKeyAlias());
    }

    @Nested
    class GivenCustomParameters {

        @Test
        void whenSettingCustomParameters_thenValidateSetValues() {
            String[] customValues = {"https://testDomain:8888", "../keystore/certificate.p12", "testPassword", "testUser", "keyAlias"};

            lenient().when(clientCertificateAuthentication.getSystemVariable(API_URL)).thenReturn(customValues[0]);
            lenient().when(clientCertificateAuthentication.getSystemVariable(CLIENT_CERT_PATH)).thenReturn(customValues[1]);
            lenient().when(clientCertificateAuthentication.getSystemVariable(CLIENT_CERT_PASSWORD)).thenReturn(customValues[2]);
            lenient().when(clientCertificateAuthentication.getSystemVariable(CLIENT_CERT_ALIAS)).thenReturn(customValues[3]);
            lenient().when(clientCertificateAuthentication.getSystemVariable(PRIVATE_KEY_ALIAS)).thenReturn(customValues[4]);

            // Validate that system parameters are propagated to the application
            assertEquals(customValues[0] + "/gateway/api/v1/auth/login", clientCertificateAuthentication.getApiUrl());
            assertEquals(customValues[1], clientCertificateAuthentication.getClientCertPath());
            assertEquals(customValues[2], clientCertificateAuthentication.getClientCertPassword());
            assertEquals(customValues[3], clientCertificateAuthentication.getClientCertAlias());
            assertEquals(customValues[4], clientCertificateAuthentication.getPrivateKeyAlias());
        }

        @Test
        void givenIncorrectHost_thenValidateError() {
            String incorrectHost = "http://localhost:8080";

            lenient().when(clientCertificateAuthentication.getSystemVariable(API_URL)).thenReturn(incorrectHost);

            assertEquals(incorrectHost + "/gateway/api/v1/auth/login", clientCertificateAuthentication.getApiUrl());

            clientCertificateAuthentication.authenticate();
            assertTrue(outContent.toString().isEmpty(), "System.out should be empty.");
            assertTrue(errContent.toString().startsWith("org.apache.http.conn.HttpHostConnectException: Connect to localhost:8080 [localhost/127.0.0.1, localhost/0:0:0:0:0:0:0:1] failed: Connection refused"),
                "Error not as expected. Actual error is: \n" + errContent);
        }

        @Test
        void givenInvalidKeystorePath_thenValidateError() {
            String fileNotFound = "../keystore/client-certs.p12";

            lenient().when(clientCertificateAuthentication.getSystemVariable(CLIENT_CERT_PATH)).thenReturn(fileNotFound);

            assertEquals(fileNotFound, clientCertificateAuthentication.getClientCertPath());

            clientCertificateAuthentication.authenticate();
            assertTrue(outContent.toString().isEmpty(), "System.out should be empty.");
            assertTrue(errContent.toString().startsWith("java.io.FileNotFoundException: "),
                "Error not as expected. Actual error is: \n" + errContent);
        }

        @Test
        void givenIncorrectClientCertificatePassword_thenValidateError() {
            String incorrectPassword = "testPassword";

            lenient().when(clientCertificateAuthentication.getSystemVariable(CLIENT_CERT_PASSWORD)).thenReturn(incorrectPassword);

            assertEquals(incorrectPassword, clientCertificateAuthentication.getClientCertPassword());

            clientCertificateAuthentication.authenticate();
            assertTrue(outContent.toString().isEmpty(), "System.out should be empty.");
            assertTrue(errContent.toString().startsWith("java.io.IOException: keystore password was incorrect"),
                "Error not as expected. Actual error is: \n" + errContent);
        }

        @Test
        void givenInvalidPrivateKeyAlias_thenValidateError() {
            String incorrectKeyAlias = "testUser";

            lenient().when(clientCertificateAuthentication.getSystemVariable(PRIVATE_KEY_ALIAS)).thenReturn(incorrectKeyAlias);

            assertEquals(incorrectKeyAlias, clientCertificateAuthentication.getPrivateKeyAlias());

            clientCertificateAuthentication.authenticate();
            assertTrue(outContent.toString().isEmpty(), "System.out should be empty.");
            assertTrue(errContent.toString().contains("java.security.KeyStoreException"), "Error not as expected. Actual error is: \n" + errContent);
            assertTrue(errContent.toString().contains("Unsupported Key type"), "Error not as expected. Actual error is: \n" + errContent);
        }
    }

    @Test
    void givenHttpsRequestWithClientCertificate_thenPeerCertificateMustBeAvailable() {
        // Assertion is done on the server to make sure that client certificate was delivered.
        clientCertificateAuthentication.authenticate();

        assertTrue(outContent.toString().startsWith("Response Code: 204"));
        // Validate that the Assertion error is null in case the certificate was present.
        Assertions.assertNull(error);
    }

    @Test
    void givenHttpsRequestWithUntrustedClientCertificate_thenValidateError() {
        lenient().when(clientCertificateAuthentication.getClientCertPath()).thenReturn("../keystore/negative/selfsigned.keystore.p12");
        lenient().when(clientCertificateAuthentication.getClientCertAlias()).thenReturn("localhost");
        lenient().when(clientCertificateAuthentication.getPrivateKeyAlias()).thenReturn("localhost");

        // Assertion is done on the server to make sure that client certificate was delivered.
        // In this case, since the certificate is not trusted, it should not reach the server
        clientCertificateAuthentication.authenticate();

        assertTrue(outContent.toString().isEmpty(), "System.out should be empty.");
        // The error may differ because of how we have set up the testing httpServer so we are checking only that the request does not go through
        // and an error is printed in the console
        Assertions.assertFalse(errContent.toString().isEmpty(), "Should result in an error.");
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
            SSLParameters sslParameters = getSSLContext().getDefaultSSLParameters();
            sslParameters.setNeedClientAuth(true);
            params.setWantClientAuth(true);
            params.setSSLParameters(sslParameters);
        }
    }

}
