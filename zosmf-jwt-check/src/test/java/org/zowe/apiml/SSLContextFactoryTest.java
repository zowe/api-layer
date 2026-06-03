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
import com.sun.net.httpserver.HttpsServer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.security.KeyStore;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SSLContextFactoryTest {

    private static final String TRUSTSTORE_PATH = "../keystore/localhost/localhost.truststore.p12";
    private static final String KEYSTORE_PATH = "../keystore/localhost/localhost.keystore.p12";
    private static final String PASSWORD = "password";

    @Nested
    class InitSSLContextWithRealStores {

        @Test
        void createsNonNullSSLContext() throws Exception {
            ZosmfJwtCheckConfig conf = createConf(null, null, TRUSTSTORE_PATH, PASSWORD, "PKCS12");
            ZosmfStores stores = new ZosmfStores(conf);

            SSLContextFactory factory = SSLContextFactory.initSSLContext(stores);

            assertNotNull(factory);
            assertNotNull(factory.getSslContext());
        }

        @Test
        void sslContextProtocolIsTLS() throws Exception {
            ZosmfJwtCheckConfig conf = createConf(null, null, TRUSTSTORE_PATH, PASSWORD, "PKCS12");
            ZosmfStores stores = new ZosmfStores(conf);

            SSLContextFactory factory = SSLContextFactory.initSSLContext(stores);

            assertEquals("TLSv1.2", factory.getSslContext().getProtocol());
        }

        @Test
        void worksWithKeystoreAndTruststore() throws Exception {
            ZosmfJwtCheckConfig conf = createConf(KEYSTORE_PATH, PASSWORD, TRUSTSTORE_PATH, PASSWORD, "PKCS12");
            ZosmfStores stores = new ZosmfStores(conf);

            SSLContextFactory factory = SSLContextFactory.initSSLContext(stores);

            assertNotNull(factory.getSslContext());
        }

        @Test
        void worksWithoutKeystore() throws Exception {
            ZosmfJwtCheckConfig conf = createConf(null, null, TRUSTSTORE_PATH, PASSWORD, "PKCS12");
            ZosmfStores stores = new ZosmfStores(conf);

            SSLContextFactory factory = SSLContextFactory.initSSLContext(stores);

            SSLContext ctx = factory.getSslContext();
            assertNotNull(ctx.getSocketFactory());
        }
    }

    @Nested
    class InitTrustAllSSLContext {

        @Test
        void createsNonNullSSLContext() throws Exception {
            SSLContextFactory factory = SSLContextFactory.initTrustAllSSLContext();

            assertNotNull(factory);
            assertNotNull(factory.getSslContext());
        }

        @Test
        void sslContextProtocolIsTLS() throws Exception {
            SSLContextFactory factory = SSLContextFactory.initTrustAllSSLContext();

            assertEquals("TLSv1.2", factory.getSslContext().getProtocol());
        }

        @Test
        void socketFactoryIsAvailable() throws Exception {
            SSLContextFactory factory = SSLContextFactory.initTrustAllSSLContext();

            assertNotNull(factory.getSslContext().getSocketFactory());
        }

        @Test
        void trustAllContextAcceptsSelfSignedCert() throws Exception {
            // Start a local HTTPS server with a self-signed cert
            KeyStore ks = KeyStore.getInstance("PKCS12");
            try (InputStream is = new java.io.FileInputStream("../keystore/localhost/localhost.keystore.p12")) {
                ks.load(is, "password".toCharArray());
            }
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, "password".toCharArray());
            SSLContext serverCtx = SSLContext.getInstance("TLSv1.2");
            serverCtx.init(kmf.getKeyManagers(), null, null);

            HttpsServer server = HttpsServer.create(new InetSocketAddress(0), 0);
            server.setHttpsConfigurator(new HttpsConfigurator(serverCtx));
            server.createContext("/", exchange -> {
                byte[] resp = "OK".getBytes();
                exchange.sendResponseHeaders(200, resp.length);
                exchange.getResponseBody().write(resp);
                exchange.getResponseBody().close();
            });
            server.start();

            try {
                int port = server.getAddress().getPort();
                SSLContextFactory factory = SSLContextFactory.initTrustAllSSLContext();
                HttpClientWrapper client = new HttpClientWrapper(factory.getSslContext(), (hostname, session) -> true);
                HttpClientWrapper.Response response = client.executeCall(
                    new URL("https://localhost:" + port + "/"), Collections.emptyMap());

                assertEquals(200, response.getStatusCode());
                assertEquals("OK", response.getBody());
            } finally {
                server.stop(0);
            }
        }
    }

    private ZosmfJwtCheckConfig createConf(String keyStore, String keyStorePassword,
                                           String trustStore, String trustStorePassword,
                                           String storeType) {
        ZosmfJwtCheckConfig conf = mock(ZosmfJwtCheckConfig.class);
        when(conf.getKeyStore()).thenReturn(keyStore);
        when(conf.getKeyStorePassword()).thenReturn(keyStorePassword);
        when(conf.getKeyStoreType()).thenReturn(storeType);
        when(conf.getTrustStore()).thenReturn(trustStore);
        when(conf.getTrustStorePassword()).thenReturn(trustStorePassword);
        when(conf.getTrustStoreType()).thenReturn(storeType);
        return conf;
    }
}
