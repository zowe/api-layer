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

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;

public class Main {

    private static final String API_URL = "https://localhost:8080/gateway/api/v1/auth/login"; // Replace with your API URL
    private static final String CLIENT_CERT_PATH = "../keystore/client_cert/client-certs.p12"; // Replace with your client cert path
    private static final String CLIENT_CERT_PASSWORD = "password"; // Replace with your cert password
    private static final String CLIENT_CERT_ALIAS = "apimtst"; // Replace with your signed client cert alias
    private static final String PRIVATE_KEY_ALIAS = "apimtst"; // Replace with your private key alias


    public static void main(String[] args) {
        try {
            // Load the keystore containing the client certificate
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (FileInputStream keyStoreStream = new FileInputStream(new File(CLIENT_CERT_PATH))) {
                keyStore.load(keyStoreStream, CLIENT_CERT_PASSWORD.toCharArray());
            }

            var key = keyStore.getKey(PRIVATE_KEY_ALIAS, CLIENT_CERT_PASSWORD.toCharArray()); // Load private key from original keystore
            var cert = keyStore.getCertificate(CLIENT_CERT_ALIAS); // Load signed certificate from original keystore

            // Create new keystore
            var newKeyStore = KeyStore.getInstance("PKCS12");
            newKeyStore.load(null);
            newKeyStore.setKeyEntry(PRIVATE_KEY_ALIAS, key, CLIENT_CERT_PASSWORD.toCharArray(), new Certificate[]{ cert }); // Create an entry with private key + signed certificate

            // Create SSL context with the client certificate
            var sslContext = new SSLContextBuilder().loadTrustMaterial((chain,type)->true)
                .loadKeyMaterial(newKeyStore, CLIENT_CERT_PASSWORD.toCharArray()).build();
            var sslsf = new DefaultClientTlsStrategy(sslContext);


            var connectionManager = BasicHttpClientConnectionManager.create((s)->sslsf);

            var clientBuilder = HttpClientBuilder.create().setConnectionManager(connectionManager);

            try (var httpClient = clientBuilder.build()) {

                // Create a POST request
                var httpPost = new HttpPost(API_URL);

                // Execute the request
                var response = httpClient.execute(httpPost, res -> res);

                // Print the response status
                System.out.println("Response Code: " + response.getCode());

                // Print headers
                var headers = response.getHeaders();
                for (var header : headers) {
                    System.out.println("Key : " + header.getName()
                        + " ,Value : " + header.getValue());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
