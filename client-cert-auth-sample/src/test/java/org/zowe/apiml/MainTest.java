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
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assumptions.assumeFalse;

class MainTest {

    static HttpsServer httpServer;
    static AssertionError error;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeAll
    static void setup() throws Exception {
        // Assertion is done on the server to make sure that client certificate was delivered.
        InetSocketAddress inetAddress = new InetSocketAddress("127.0.0.1", 10010);
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
    void givenNoParameters_thenValidateDefaultValues() throws Exception {
        Field apiURL = Main.class.getDeclaredField("API_URL");
        apiURL.setAccessible(true);
        Assertions.assertEquals("https://localhost:10010/gateway/api/v1/auth/login", apiURL.get(null));

        Field clientCertificatePath = Main.class.getDeclaredField("CLIENT_CERT_PATH");
        clientCertificatePath.setAccessible(true);
        Assertions.assertEquals("../keystore/client_cert/client-certs.p12", clientCertificatePath.get(null));

        Field clientCertificatePassword = Main.class.getDeclaredField("CLIENT_CERT_PASSWORD");
        clientCertificatePassword.setAccessible(true);
        Assertions.assertEquals("password", clientCertificatePassword.get(null));

        Field clientCertificateAlias = Main.class.getDeclaredField("CLIENT_CERT_ALIAS");
        clientCertificateAlias.setAccessible(true);
        Assertions.assertEquals("user", clientCertificateAlias.get(null));

        Field privateKeyAlias = Main.class.getDeclaredField("PRIVATE_KEY_ALIAS");
        privateKeyAlias.setAccessible(true);
        Assertions.assertEquals("user", privateKeyAlias.get(null));
    }

    @Nested
    class WhenSettingCustomParameters {

        @Test
        @SuppressWarnings("unchecked")
        void givenSystemVariables_thenValidateSetValues() throws NoSuchFieldException, IllegalAccessException {
            assumeFalse(StringUtils.containsIgnoreCase(System.getProperty("os.name"), "win"),
                "This test cannot be executed on Windows machine");

            String[] customParameters = {"API_URL", "CLIENT_CERT_PATH", "CLIENT_CERT_PASSWORD", "CLIENT_CERT_ALIAS", "PRIVATE_KEY_ALIAS"};
            String[] customValues = {"https://testDomain:8888", "../keystore/certificate.p12", "testPassword", "testUser", "keyAlias"};

            Class<?> classOfMap = System.getenv().getClass();
            Field mapField = classOfMap.getDeclaredField("m");
            mapField.setAccessible(true);
            Map<String, String> writeableEnvironmentVariables = (Map<String, String>) mapField.get(System.getenv());

            try {
                writeableEnvironmentVariables.put(customParameters[0], customValues[0]);
                writeableEnvironmentVariables.put(customParameters[1], customValues[1]);
                writeableEnvironmentVariables.put(customParameters[2], customValues[2]);
                writeableEnvironmentVariables.put(customParameters[3], customValues[3]);
                writeableEnvironmentVariables.put(customParameters[4], customValues[4]);

                // Validate that system parameters are set
                Field apiURL = Main.class.getDeclaredField(customParameters[0]);
                apiURL.setAccessible(true);
                Assertions.assertEquals(customValues[0] + "/gateway/api/v1/auth/login", apiURL.get(null));

                Field clientCertificatePath = Main.class.getDeclaredField(customParameters[1]);
                clientCertificatePath.setAccessible(true);
                Assertions.assertEquals(customValues[1], clientCertificatePath.get(null));

                Field clientCertificatePassword = Main.class.getDeclaredField(customParameters[2]);
                clientCertificatePassword.setAccessible(true);
                Assertions.assertEquals(customValues[2], clientCertificatePassword.get(null));

                Field clientCertificateAlias = Main.class.getDeclaredField(customParameters[3]);
                clientCertificateAlias.setAccessible(true);
                Assertions.assertEquals(customValues[3], clientCertificateAlias.get(null));

                Field privateKeyAlias = Main.class.getDeclaredField(customParameters[4]);
                privateKeyAlias.setAccessible(true);
                Assertions.assertEquals(customValues[4], privateKeyAlias.get(null));
            } finally {
                Arrays.stream(customParameters).forEach(writeableEnvironmentVariables::remove);
            }
        }

        @Test
        @SuppressWarnings("unchecked")
        void givenInvalidKeystorePath_thenValidateError() throws Exception {
            assumeFalse(StringUtils.containsIgnoreCase(System.getProperty("os.name"), "win"),
                "This test cannot be executed on Windows machine");

            String clientCertPath = "CLIENT_CERT_PATH";
            String fileNotFound = "../keystore/client-certs.p12";

            Class<?> classOfMap = System.getenv().getClass();
            Field mapField = classOfMap.getDeclaredField("m");
            mapField.setAccessible(true);
            Map<String, String> writeableEnvironmentVariables = (Map<String, String>) mapField.get(System.getenv());

            try {
                writeableEnvironmentVariables.put(clientCertPath, fileNotFound);

                // Validate that system parameters are set
                Field clientCertificatePath = Main.class.getDeclaredField(clientCertPath);
                clientCertificatePath.setAccessible(true);
                Assertions.assertEquals(fileNotFound, clientCertificatePath.get(null));

                Main.main(null);
                Assertions.assertTrue(outContent.toString().isEmpty());
                Assertions.assertTrue(errContent.toString()
                    .startsWith(String.format("java.io.FileNotFoundException: %s (The system cannot find the path specified)", fileNotFound)));
            } finally {
                writeableEnvironmentVariables.remove(clientCertPath);
            }
        }

        @Test
        @SuppressWarnings("unchecked")
        void givenIncorrectClientCertificatePassword_thenValidateError() throws Exception {
            assumeFalse(StringUtils.containsIgnoreCase(System.getProperty("os.name"), "win"),
                "This test cannot be executed on Windows machine");

            String clientCertPasswordParameter = "CLIENT_CERT_PASSWORD";
            String incorrectPassword = "testPassword";

            Class<?> classOfMap = System.getenv().getClass();
            Field mapField = classOfMap.getDeclaredField("m");
            mapField.setAccessible(true);
            Map<String, String> writeableEnvironmentVariables = (Map<String, String>) mapField.get(System.getenv());

            try {
                writeableEnvironmentVariables.put(clientCertPasswordParameter, incorrectPassword);

                Field clientCertificatePassword = Main.class.getDeclaredField(clientCertPasswordParameter);
                clientCertificatePassword.setAccessible(true);
                Assertions.assertEquals(incorrectPassword, clientCertificatePassword.get(null));

                Main.main(null);
                Assertions.assertTrue(outContent.toString().isEmpty());
                Assertions.assertTrue(errContent.toString().startsWith("java.io.IOException: keystore password was incorrect"));
            } finally {
                writeableEnvironmentVariables.remove(clientCertPasswordParameter);
            }
        }

        @Test
        @SuppressWarnings("unchecked")
        void givenInvalidPrivateKeyAlias_thenValidateError() throws Exception {
            assumeFalse(StringUtils.containsIgnoreCase(System.getProperty("os.name"), "win"),
                "This test cannot be executed on Windows machine");

            String privateKeyAliasParameter = "PRIVATE_KEY_ALIAS";
            String incorrectKeyAlias = "testUser";

            Class<?> classOfMap = System.getenv().getClass();
            Field mapField = classOfMap.getDeclaredField("m");
            mapField.setAccessible(true);
            Map<String, String> writeableEnvironmentVariables = (Map<String, String>) mapField.get(System.getenv());

            try {
                writeableEnvironmentVariables.put(privateKeyAliasParameter, incorrectKeyAlias);

                Field privateKeyAlias = Main.class.getDeclaredField(privateKeyAliasParameter);
                privateKeyAlias.setAccessible(true);
                Assertions.assertEquals(incorrectKeyAlias, privateKeyAlias.get(null));

                Main.main(null);
                Assertions.assertTrue(outContent.toString().isEmpty());
                Assertions.assertTrue(errContent.toString().startsWith("java.security.KeyStoreException: Key protection algorithm not found: java.security.KeyStoreException: Unsupported Key type"));
            } finally {
                writeableEnvironmentVariables.remove(privateKeyAliasParameter);
            }
        }

    }


    @Test
    void givenHttpsRequestWithClientCertificate_thenPeerCertificateMustBeAvailable() {
        // Assertion is done on the server to make sure that client certificate was delivered.
        // Assertion error is then rethrown in the tear down method in case certificate was not present.
        Main.main(null);

        Assertions.assertTrue(outContent.toString().startsWith("Response Code: 204"));
        Assertions.assertNull(error);
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
