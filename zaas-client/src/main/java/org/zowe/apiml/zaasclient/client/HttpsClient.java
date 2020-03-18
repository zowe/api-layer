/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.zaasclient.client;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.zowe.apiml.zaasclient.config.ConfigProperties;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;

public final class HttpsClient implements Closeable
{
    private final CloseableHttpClient closeableHttpsClient;

    private final CloseableHttpClient closeableHttpClient;
    private final String keyStorePassword;
    private final String trustStorePassword;
    private final String keyStoreType;
    private final String trustStoreType;

    public CloseableHttpClient getCloseableHttpClient() {
        return closeableHttpClient;
    }

    private final RequestConfig requestConfig;

    private String trustStorePath;
    private String keyStorePath;

    public HttpsClient(ConfigProperties configProperties) throws NoSuchAlgorithmException, KeyStoreException, IOException, CertificateException, KeyManagementException, UnrecoverableKeyException {
        this.keyStorePath = configProperties.getKeyStorePath();
        this.keyStorePassword = configProperties.getKeyStorePassword();
        this.keyStoreType = configProperties.getKeyStoreType();
        this.trustStorePath = configProperties.getTrustStorePath();
        this.trustStorePassword = configProperties.getTrustStorePassword();
        this.trustStoreType = configProperties.getTrustStoreType();

        SSLContext sslContext;
        TrustManagerFactory tmf = null;
        KeyManagerFactory kmf = null;

        if(trustStorePath!=null) {
//        Load Trust Store *************
            tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyStore trustStore = KeyStore.getInstance(trustStoreType);
            File trustFile = new File(trustStorePath);
            trustStore.load(new FileInputStream(trustFile), trustStorePassword.toCharArray());
            tmf.init(trustStore);
//        **************
        }

        if(keyStorePath!=null) {
            kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            File keyFile = new File(keyStorePath);
            keyStore.load(new FileInputStream(keyFile), keyStorePassword.toCharArray());
            kmf.init(keyStore,keyStorePassword.toCharArray());
        }

        sslContext = this.getSSLContext(kmf, tmf);

        final SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext,
                SSLConnectionSocketFactory.getDefaultHostnameVerifier());
        this.closeableHttpsClient = HttpClients.custom()
                .setSSLSocketFactory(sslsf)
                .build();

        this.requestConfig = this.buildCustomRequestConfig();

        this.closeableHttpClient = HttpClients.custom()
                .setSSLSocketFactory(sslsf)
                .setDefaultRequestConfig(this.requestConfig)
                .setMaxConnTotal(3 * 3)
                .setMaxConnPerRoute(3)
                .build();
    }

    /**
     * @return
     * @throws Exception
     * @param kmf
     * @param tmf
     */
    private SSLContext getSSLContext(KeyManagerFactory kmf, TrustManagerFactory tmf) throws KeyManagementException, NoSuchAlgorithmException {
        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf != null?kmf.getKeyManagers():null, tmf!=null?tmf.getTrustManagers():null, new SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw e;
        }
        return sslContext;
    }

    private RequestConfig buildCustomRequestConfig() {
        final RequestConfig.Builder builder = RequestConfig.custom();
        builder.setConnectionRequestTimeout(10*1000);
        builder.setSocketTimeout(10*1000);
        builder.setConnectTimeout(10*1000);
        return builder.build();
    }

    public CloseableHttpClient getCloseableHttpsClient() {
        return closeableHttpsClient;
    }

    @Override
    public void close() throws IOException {
        closeableHttpsClient.close();
    }
}
