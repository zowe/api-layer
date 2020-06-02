/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.zaasclient.service.internal;

import lombok.AllArgsConstructor;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.zowe.apiml.zaasclient.config.ConfigProperties;
import org.zowe.apiml.zaasclient.exception.ZaasConfigurationErrorCodes;
import org.zowe.apiml.zaasclient.exception.ZaasConfigurationException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;

@AllArgsConstructor
class HttpsClientProvider implements CloseableClientProvider {
    private final RequestConfig requestConfig;

    public static final String SAFKEYRING = "safkeyring";

    private TrustManagerFactory tmf;
    private KeyManagerFactory kmf;

    // KeyStore is initialized only in the case of PassTicket handling.
    //     It can be null, if passticket functionality not used.
    private final String keyStorePassword;
    private final String keyStoreType;
    private String keyStorePath;

    public HttpsClientProvider(ConfigProperties configProperties) throws ZaasConfigurationException {
        this.requestConfig = this.buildCustomRequestConfig();

        if (configProperties.getTrustStorePath() == null) {
            throw new ZaasConfigurationException(ZaasConfigurationErrorCodes.TRUST_STORE_NOT_PROVIDED);
        }

        initializeTrustManagerFactory(configProperties.getTrustStorePath(), configProperties.getTrustStoreType(), configProperties.getTrustStorePassword());

        this.keyStorePath = configProperties.getKeyStorePath();
        this.keyStorePassword = configProperties.getKeyStorePassword();
        this.keyStoreType = configProperties.getKeyStoreType();
    }

    @Override
    public CloseableHttpClient getHttpsClientWithTrustStore() throws ZaasConfigurationException {
        return sharedHttpClientConfiguration(getSSLContext())
            .build();
    }

    @Override
    public CloseableHttpClient getHttpsClientWithTrustStore(BasicCookieStore cookieStore) throws ZaasConfigurationException {
        return sharedHttpClientConfiguration(getSSLContext())
            .setDefaultCookieStore(cookieStore)
            .build();
    }

    @Override
    public CloseableHttpClient getHttpsClientWithKeyStoreAndTrustStore() throws ZaasConfigurationException {
        if (keyStorePath == null) {
            throw new ZaasConfigurationException(ZaasConfigurationErrorCodes.KEY_STORE_NOT_PROVIDED);
        }

        if (kmf == null) {
            initializeKeyStoreManagerFactory();
        }

        return sharedHttpClientConfiguration(getSSLContext())
            .build();
    }

    private void initializeTrustManagerFactory(String trustStorePath, String trustStoreType, String trustStorePassword)
        throws ZaasConfigurationException {
        try {
            tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyStore trustStore = getKeystore(trustStorePath, trustStoreType, trustStorePassword);
            tmf.init(trustStore);
        } catch (CertificateException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new ZaasConfigurationException(ZaasConfigurationErrorCodes.WRONG_CRYPTO_CONFIGURATION, e);
        } catch (IOException e) {
            throw new ZaasConfigurationException(ZaasConfigurationErrorCodes.IO_CONFIGURATION_ISSUE, e);
        }
    }

    private void initializeKeyStoreManagerFactory() throws ZaasConfigurationException {
        try {
            kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            KeyStore keyStore = getKeystore(keyStorePath, keyStoreType, keyStorePassword);
            kmf.init(keyStore, keyStorePassword.toCharArray());
        } catch (NoSuchAlgorithmException | CertificateException | UnrecoverableKeyException | KeyStoreException e) {
            throw new ZaasConfigurationException(ZaasConfigurationErrorCodes.WRONG_CRYPTO_CONFIGURATION, e);
        } catch (IOException e) {
            throw new ZaasConfigurationException(ZaasConfigurationErrorCodes.IO_CONFIGURATION_ISSUE, e);
        }
    }

    private KeyStore getKeystore(String uri, String keyStoreType, String storePassword) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        InputStream correctInStream = getCorrectInputStream(uri);
        keyStore.load(correctInStream, storePassword.toCharArray());
        return keyStore;
    }

    private InputStream getCorrectInputStream(String uri) throws IOException {
        if (uri.startsWith(SAFKEYRING + ":////")) {
            URL url = new URL(replaceFourSlashes(uri));
            return url.openStream();
        }
        return new FileInputStream(new File(uri));
    }

    public static String replaceFourSlashes(String storeUri) {
        return storeUri == null ? null : storeUri.replaceFirst("////", "//");
    }

    private SSLContext getSSLContext() throws ZaasConfigurationException {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(
                kmf != null ? kmf.getKeyManagers() : null,
                tmf.getTrustManagers(),
                new SecureRandom()
            );
            return sslContext;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new ZaasConfigurationException(ZaasConfigurationErrorCodes.WRONG_CRYPTO_CONFIGURATION, e);
        }
    }

    /**
     * Create Http Configuration with defaults for maximum of connections and maximum of connections per route.
     */
    private HttpClientBuilder sharedHttpClientConfiguration(SSLContext sslContext) {
        final SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext,
            SSLConnectionSocketFactory.getDefaultHostnameVerifier());

        return HttpClients.custom()
            .setSSLSocketFactory(sslsf)
            .setDefaultRequestConfig(this.requestConfig)
            .setMaxConnTotal(3 * 3)
            .setMaxConnPerRoute(3);
    }

    /**
     * Create configuration for requests with default timeouts set to 10s.
     */
    private RequestConfig buildCustomRequestConfig() {
        final RequestConfig.Builder builder = RequestConfig.custom();
        builder.setConnectionRequestTimeout(10 * 1000);
        builder.setSocketTimeout(10 * 1000);
        builder.setConnectTimeout(10 * 1000);
        return builder.build();
    }
}
