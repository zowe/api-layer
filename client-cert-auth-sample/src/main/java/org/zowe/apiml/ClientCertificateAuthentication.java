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

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;

import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Optional;

/**
 * Java-based client that performs authentication to the gateway ("/gateway/api/v1/auth/login")
 * using a client certificate.
 *
 * <p>
 * User can specify via system variables:
 * - API_URL: host url against which the request will be sent
 * - CLIENT_CERT_PATH: path of the client certificate to validate
 * - CLIENT_CERT_PASSWORD: client certificate password
 * - CLIENT_CERT_ALIAS: client certificate alias
 * - PRIVATE_KEY_ALIAS: private key alias
 * <p>
 * If not specified, the application will use the default values.
 */
@SuppressWarnings("squid:S106") // Ignore the System.out warnings
public class ClientCertificateAuthentication {

    protected static final String API_URL = "API_URL"; // Replace with your API URL
    protected static final String CLIENT_CERT_PATH = "CLIENT_CERT_PATH"; // Replace with your client cert path
    protected static final String CLIENT_CERT_PASSWORD = "CLIENT_CERT_PASSWORD";
    protected static final String CLIENT_CERT_ALIAS = "CLIENT_CERT_ALIAS";
    protected static final String PRIVATE_KEY_ALIAS = "PRIVATE_KEY_ALIAS";

    protected String getSystemVariable(String variableName) {
        return System.getenv(variableName);
    }

    protected String getApiUrl() {
        return Optional.ofNullable(getSystemVariable(API_URL)).orElse("https://localhost:10010") + "/gateway/api/v1/auth/login";
    }

    protected String getClientCertPath() {
        return Optional.ofNullable(getSystemVariable(CLIENT_CERT_PATH)).orElse("../keystore/client_cert/client-certs.p12");
    }

    protected String getClientCertPassword() {
        return Optional.ofNullable(getSystemVariable(CLIENT_CERT_PASSWORD)).orElse("password");
    }

    protected String getClientCertAlias() {
        return Optional.ofNullable(getSystemVariable(CLIENT_CERT_ALIAS)).orElse("user");
    }

    protected String getPrivateKeyAlias() {
        return Optional.ofNullable(getSystemVariable(PRIVATE_KEY_ALIAS)).orElse("user");
    }

    public void authenticate() {
        try {
            String clientCertPassword = getClientCertPassword();
            String privateKeyAlias = getPrivateKeyAlias();

            // Load the keystore containing the client certificate
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (FileInputStream keyStoreStream = new FileInputStream(getClientCertPath())) {
                keyStore.load(keyStoreStream, clientCertPassword.toCharArray());
            }

            Key key = keyStore.getKey(privateKeyAlias, clientCertPassword.toCharArray()); // Load private key from original keystore
            Certificate cert = keyStore.getCertificate(getClientCertAlias()); // Load signed certificate from original keystore

            // Create new keystore
            KeyStore newKeyStore = KeyStore.getInstance("PKCS12");
            newKeyStore.load(null);
            newKeyStore.setKeyEntry(privateKeyAlias, key, clientCertPassword.toCharArray(), new Certificate[]{cert}); // Create an entry with private key + signed certificate

            // Create SSL context with the client certificate
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial((chain, type) -> true)
                .loadKeyMaterial(newKeyStore, clientCertPassword.toCharArray()).build();
            ConnectionSocketFactory connectionSocketFactory = new SSLConnectionSocketFactory(sslContext);

            RegistryBuilder<ConnectionSocketFactory> socketFactoryRegistryBuilder = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", connectionSocketFactory);
            Registry<ConnectionSocketFactory> socketFactoryRegistry = socketFactoryRegistryBuilder.build();

            try (BasicHttpClientConnectionManager connectionManager = new BasicHttpClientConnectionManager(socketFactoryRegistry)) {

                HttpClientBuilder clientBuilder = HttpClientBuilder.create().setConnectionManager(connectionManager);

                try (CloseableHttpClient httpClient = clientBuilder.build()) {

                    // Create a POST request
                    HttpPost httpPost = new HttpPost(getApiUrl());

                    // Execute the request
                    CloseableHttpResponse response = httpClient.execute(httpPost);

                    // Print the response status
                    System.out.println("Response Code: " + response.getStatusLine().getStatusCode());

                    // Print headers
                    Header[] headers = response.getAllHeaders();
                    for (Header header : headers) {
                        System.out.println("Key : " + header.getName() + " ,Value : " + header.getValue());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
