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
import org.apache.http.client.CookieStore;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;

@AllArgsConstructor
class ZaasHttpsClientProvider implements CloseableClientProvider {
    private static final int REQUEST_TIMEOUT = 30 * 1000;

    private final RequestConfig requestConfig;

    public static final String SAFKEYRING = "safkeyring";

    private TrustManagerFactory tmf;
    private KeyManagerFactory kmf;

    private final char[] keyStorePassword;
    private final String keyStoreType;
    private final String keyStorePath;

    private final CookieStore cookieStore = new BasicCookieStore();

    private CloseableHttpClient httpsClientWithKeyStoreAndTrustStore;

    public ZaasHttpsClientProvider(ConfigProperties configProperties) throws ZaasConfigurationException {
        this.requestConfig = this.buildCustomRequestConfig();

        if (configProperties.getTrustStorePath() == null) {
            throw new ZaasConfigurationException(ZaasConfigurationErrorCodes.TRUST_STORE_NOT_PROVIDED);
        }

        initializeTrustManagerFactory(configProperties.getTrustStorePath(), configProperties.getTrustStoreType(), configProperties.getTrustStorePassword());

        this.keyStorePath = configProperties.getKeyStorePath();
        this.keyStorePassword = configProperties.getKeyStorePassword();
        this.keyStoreType = configProperties.getKeyStoreType();
    }

    public void clearCookieStore() {
        this.cookieStore.clear();
    }

    @Override
    public synchronized CloseableHttpClient getHttpClient() throws ZaasConfigurationException {
        if (keyStorePath == null) {
            throw new ZaasConfigurationException(ZaasConfigurationErrorCodes.KEY_STORE_NOT_PROVIDED);
        }
        if (httpsClientWithKeyStoreAndTrustStore == null) {
            if (kmf == null) {
                initializeKeyStoreManagerFactory();
            }
            httpsClientWithKeyStoreAndTrustStore = sharedHttpClientConfiguration(getSSLContext())
                .build();
        }
        return httpsClientWithKeyStoreAndTrustStore;
    }

    private void initializeTrustManagerFactory(String trustStorePath, String trustStoreType, char[] trustStorePassword)
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
            kmf.init(keyStore, keyStorePassword);
        } catch (NoSuchAlgorithmException | CertificateException | UnrecoverableKeyException | KeyStoreException e) {
            throw new ZaasConfigurationException(ZaasConfigurationErrorCodes.WRONG_CRYPTO_CONFIGURATION, e);
        } catch (IOException e) {
            throw new ZaasConfigurationException(ZaasConfigurationErrorCodes.IO_CONFIGURATION_ISSUE, e);
        }
    }

    private KeyStore getKeystore(String uri, String keyStoreType, char[] storePassword) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        InputStream correctInStream = getCorrectInputStream(uri);
        keyStore.load(correctInStream, storePassword);
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
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
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
            .setMaxConnPerRoute(3)
            .setDefaultCookieStore(cookieStore);
    }

    private RequestConfig buildCustomRequestConfig() {
        final RequestConfig.Builder builder = RequestConfig.custom();
        builder.setConnectionRequestTimeout(REQUEST_TIMEOUT);
        builder.setSocketTimeout(REQUEST_TIMEOUT);
        builder.setConnectTimeout(REQUEST_TIMEOUT);
        return builder.build();
    }
}
