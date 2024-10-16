package org.zowe.apiml;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.ssl.SSLContextBuilder;


import javax.net.ssl.SSLContext;

import java.awt.desktop.ScreenSleepEvent;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;

public class Main {
    //private static final String API_URL = "https://hostname:port/bcmdbmdsh/api/v1/gen/agents?flattenResults=false"; // Replace with your API URL
    private static final String API_URL = "https://hostname:port/gateway/api/v1/auth/login"; // Replace with your API URL
    private static final String CLIENT_CERT_PATH = "/path/to/keystore.p12"; // Replace with your client cert path
    private static final String CLIENT_CERT_PASSWORD = "password"; // Replace with your cert password
    private static final String CLIENT_CERT_ALIAS = "mycert"; // Replace with your signed client cert alias
    private static final String PRIVATE_KEY_ALIAS = "server"; // Replace with your private key alias


    public static void main(String[] args) {
        try {
            // Load the keystore containing the client certificate
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (FileInputStream keyStoreStream = new FileInputStream(new File(CLIENT_CERT_PATH))) {
                keyStore.load(keyStoreStream, CLIENT_CERT_PASSWORD.toCharArray());
            }

            Key key = keyStore.getKey(PRIVATE_KEY_ALIAS, CLIENT_CERT_PASSWORD.toCharArray()); // Load private key from original keystore
            Certificate cert = keyStore.getCertificate(CLIENT_CERT_ALIAS); // Load signed certificate from original keystore

            // Create new keystore
            KeyStore newKeyStore = KeyStore.getInstance("PKCS12");
            newKeyStore.load(null);
            newKeyStore.setKeyEntry(PRIVATE_KEY_ALIAS, key, CLIENT_CERT_PASSWORD.toCharArray(), new Certificate[]{ cert }); // Create an entry with private key + signed certificate

            // Create SSL context with the client certificate
//            SSLContext sslContext = SSLContexts.custom()
//                .loadKeyMaterial(newKeyStore, CLIENT_CERT_PASSWORD.toCharArray())
//                .build();
            var sslContext = new SSLContextBuilder()
                .loadKeyMaterial(newKeyStore, CLIENT_CERT_PASSWORD.toCharArray()).build();

            // Create an HTTP client with the SSL context
            HttpClientBuilder clientBuilder = HttpClientBuilder.create();

            var sslsf = new DefaultClientTlsStrategy(sslContext);
//            var socketFactoryRegistry =
//                RegistryBuilder.<DefaultClientTlsStrategy> create()
//                    .register("https", sslsf)
//                    .build();

            var connectionManager = BasicHttpClientConnectionManager.create((s)->sslsf);
            var clientBuilder1 = clientBuilder.setConnectionManager(connectionManager);


            try (CloseableHttpClient httpClient = clientBuilder1.build()) {

//                // Create a GET request
//                HttpGet httpGet = new HttpGet(API_URL);

                // Create a POST request
                HttpPost httpPost = new HttpPost(API_URL);

                // Execute the request
                HttpResponse response = httpClient.execute(httpPost, res -> res);

                // Print the response status
                System.out.println("Response Code: " + response.getCode());

                // Print headers
                Header[] headers = response.getHeaders();
                for (Header header : headers) {
                    System.out.println("Key : " + header.getName()
                        + " ,Value : " + header.getValue());
                }

                // You can read the response body if needed
//                HttpEntity entity = response.getEntity();
//                StringBuilder builder = new StringBuilder();
//                try (BufferedInputStream is = new BufferedInputStream(entity.getContent())) {
//                    byte[] contents = new byte[1024];
//                    int bytesRead = 0;
//                    while ((bytesRead = is.read(contents)) != -1) {
//                        builder.append(new String(contents, 0, bytesRead));
//                    }
//                }
//                System.out.println("Response Body: " + builder.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
