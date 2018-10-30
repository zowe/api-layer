/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.ssl;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.net.ssl.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Unit testing SSL with Apache HttpClient's LocalTestServer
 */
@Ignore
public class SSLTests {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static final boolean ONE_WAY_SSL = false; // no client certificates
    private static final String PROTOCOL = "TLS";

    private static final KeyStore NO_CLIENT_KEYSTORE = null;
    private static final SSLContext NO_SSL_CONTEXT = null;

    private static final char[] KEYPASS_AND_STOREPASS_VALUE = "password".toCharArray();
    private static final String JAVA_KEYSTORE = "jks";
    private static final String SERVER_KEYSTORE = "ssl/server_keystore.jks";
    private static final String SERVER_TRUSTSTORE = "ssl/server_truststore.jks";
    private static final String CLIENT_KEYSTORE = "ssl/client_keystore.jks";
    private static final String CLIENT_TRUSTSTORE = "ssl/client_truststore.jks";

    private static final TrustManager[] NO_SERVER_TRUST_MANAGER = null;

    private CloseableHttpClient httpclient;

    @Before
    public void setUp() throws Exception {
        httpclient = HttpClients.createDefault();
    }

    @Test
    public void execute_WithNoScheme_ThrowsClientProtocolExceptionInvalidHostname()
            throws Exception {
        final HttpServer server = createLocalTestServer(NO_SSL_CONTEXT, ONE_WAY_SSL);
        server.start();

        String baseUrl = getBaseUrl(server);

        thrown.expect(IsInstanceOf.instanceOf(ClientProtocolException.class));
        thrown.expectMessage("URI does not specify a valid host name");

        httpclient.execute(new HttpGet(baseUrl + "/echo/this"));

        server.stop();
    }

    @Test
    public void httpRequest_Returns200OK() throws Exception {
        final HttpServer server = createLocalTestServer(NO_SSL_CONTEXT, ONE_WAY_SSL);
        server.start();

        String baseUrl = getBaseUrl(server);

        try {
            HttpResponse httpResponse = httpclient.execute(
                    new HttpGet("http://" + baseUrl + "/echo/this"));

            assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(200));
        } finally {
            server.stop();
        }
    }

    @Test
    public void httpsRequest_WithNoSSLContext_ThrowsSSLExceptionPlaintextConnection() throws
            Exception {
        final HttpServer server = createLocalTestServer(NO_SSL_CONTEXT, ONE_WAY_SSL);
        server.start();

        String baseUrl = getBaseUrl(server);

        try {
            thrown.expect(IsInstanceOf.instanceOf(SSLException.class));
            thrown.expectMessage("Unrecognized SSL message, plaintext connection?");

            httpclient.execute(new HttpGet("https://" + baseUrl + "/echo/this"));
        } finally {
            server.stop();
        }
    }

    @Test
    public void httpsRequest_With1WaySSLAndTrustingAllCertsButNoClientTrustStore_Returns200OK()
            throws Exception {
        /*
        This time, we tell the client to trust all certificates presented to it, so certificate
         validation is bypassed and the request succeeds
         */
        SSLContext trustedSSLContext =
                new SSLContextBuilder().loadTrustMaterial(
                        NO_CLIENT_KEYSTORE,
                        (X509Certificate[] arg0, String arg1) -> true) // trust all
                        .build();

        httpclient = HttpClients.custom().setSSLContext(trustedSSLContext).build();

        SSLContext serverSSLContext = createServerSSLContext(SERVER_KEYSTORE,
                NO_SERVER_TRUST_MANAGER, KEYPASS_AND_STOREPASS_VALUE);

        final HttpServer server = createLocalTestServer(serverSSLContext, ONE_WAY_SSL);
        server.start();

        String baseUrl = getBaseUrl(server);

        try {
            HttpResponse httpResponse = httpclient.execute(
                    new HttpGet("https://" + baseUrl + "/echo/this"));

            assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(200));
        } finally {
            server.stop();
        }
    }

    @Ignore // TODO implement in clients
    public void httpsRequest_With1WaySSLAndValidatingCertsAndClientTrustStore_Returns200OK()
            throws Exception {
        SSLContext serverSSLContext = createServerSSLContext(SERVER_KEYSTORE,
                NO_SERVER_TRUST_MANAGER, KEYPASS_AND_STOREPASS_VALUE);

        final HttpServer server = createLocalTestServer(serverSSLContext, ONE_WAY_SSL);
        server.start();

        String baseUrl = getBaseUrl(server);

        // The server certificate was imported into the client's TrustStore (using keytool -import)
        KeyStore clientTrustStore = getStore(CLIENT_TRUSTSTORE, KEYPASS_AND_STOREPASS_VALUE);

        SSLContext sslContext =
                new SSLContextBuilder().loadTrustMaterial(
                        clientTrustStore, new TrustSelfSignedStrategy()).build();

        httpclient = HttpClients.custom().setSSLContext(sslContext).build();

        /*
        The HTTP client will now validate the server's presented certificate using its TrustStore.
         Since the cert was imported to the client's TrustStore explicitly (see above), the
         certificate will validate and the request will succeed
         */
        try {
            HttpResponse httpResponse = httpclient.execute(
                    new HttpGet("https://" + baseUrl + "/echo/this"));

            assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(200));
        } finally {
            server.stop();
        }
    }

    private HttpServer createLocalTestServer(SSLContext sslContext, boolean forceSSLAuth)
            throws UnknownHostException {

        return ServerBootstrap.bootstrap()
                .setLocalAddress(Inet4Address.getByName("apigateway"))
                .setSslContext(sslContext)
                .setSslSetupHandler(socket -> socket.setNeedClientAuth(forceSSLAuth))
                .registerHandler("*",
                        (request, response, context) -> response.setStatusCode(HttpStatus.SC_OK))
                .create();
    }

    private String getBaseUrl(HttpServer server) {
        return server.getInetAddress().getHostName() + ":" + server.getLocalPort();
    }

    /*
    Create an SSLContext for the server using the server's JKS. This instructs the server to
    present its certificate when clients connect over HTTPS.
     */
    private SSLContext createServerSSLContext(final String keyStoreFileName,
            TrustManager[] serverTrustManagers, final char[] password) throws CertificateException,
            NoSuchAlgorithmException, KeyStoreException, IOException, UnrecoverableKeyException,
            KeyManagementException {
        KeyStore serverKeyStore = getStore(keyStoreFileName, password);
        KeyManager[] serverKeyManagers = getKeyManagers(serverKeyStore, password);

        SSLContext sslContext = SSLContexts.custom().useProtocol(PROTOCOL).build();
        sslContext.init(serverKeyManagers, serverTrustManagers, new SecureRandom());

        return sslContext;
    }

    /**
     * KeyStores provide credentials, TrustStores verify credentials.
     *
     * Server KeyStores stores the server's private keys, and certificates for corresponding public
     * keys. Used here for HTTPS connections over localhost.
     *
     * Client TrustStores store servers' certificates.
     */
    private KeyStore getStore(final String storeFileName, final char[] password) throws
            KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        final KeyStore store = KeyStore.getInstance(JAVA_KEYSTORE);
        URL url = getClass().getClassLoader().getResource(storeFileName);
        assert url != null;
        try (InputStream inputStream = url.openStream()) {
            store.load(inputStream, password);
        }

        return store;
    }

    /**
     * KeyManagers decide which authentication credentials (e.g. certs) should be sent to the remote
     * host for authentication during the SSL handshake.
     *
     * Server KeyManagers use their private keys during the key exchange algorithm and send
     * certificates corresponding to their public keys to the clients. The certificate comes from
     * the KeyStore.
     */
    private KeyManager[] getKeyManagers(KeyStore store, final char[] password) throws
            NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException {
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(store, password);

        return keyManagerFactory.getKeyManagers();
    }

    /**
     * TrustManagers determine if the remote connection should be trusted or not.
     *
     * Clients will use certificates stored in their TrustStores to verify identities of servers.
     * Servers will use certificates stored in their TrustStores to verify identities of clients.
     */
    private TrustManager[] getTrustManagers(KeyStore store) throws NoSuchAlgorithmException,
            KeyStoreException {
        TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(store);

        return trustManagerFactory.getTrustManagers();
    }

}
