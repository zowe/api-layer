/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security;

import com.netflix.discovery.shared.transport.jersey.EurekaJerseyClientImpl.EurekaJerseyClientBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.UserTokenHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.PrivateKeyStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.message.yaml.YamlMessageServiceInstance;
import org.zowe.apiml.security.HttpsConfigError.ErrorCode;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;


@Slf4j
@Data
public class HttpsFactory {

    private HttpsConfig config;
    private SSLContext secureSslContext;
    private KeyStore usedKeyStore = null;
    private ApimlLogger apimlLog;

    public HttpsFactory(HttpsConfig httpsConfig) {
        this.config = httpsConfig;
        this.apimlLog = ApimlLogger.of(HttpsFactory.class, YamlMessageServiceInstance.getInstance());
    }


    public CloseableHttpClient createSecureHttpClient(HttpClientConnectionManager connectionManager) {

        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(config.getRequestConnectionTimeout())
            .setSocketTimeout(config.getRequestConnectionTimeout())
            .setConnectionRequestTimeout(config.getRequestConnectionTimeout()).build();
        UserTokenHandler userTokenHandler = context -> context.getAttribute("my-token");

        return HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).setSSLHostnameVerifier(getHostnameVerifier())
            .setConnectionTimeToLive(config.getTimeToLive(), TimeUnit.MILLISECONDS)
            .setConnectionManager(connectionManager).disableCookieManagement().setUserTokenHandler(userTokenHandler)
            .setKeepAliveStrategy(ApimlKeepAliveStrategy.INSTANCE)
            .disableAuthCaching().build();

    }

    public ConnectionSocketFactory createSslSocketFactory() {
        if (config.isVerifySslCertificatesOfServices()) {
            return getSSLConnectionSocketFactory();
        } else {
            apimlLog.log("org.zowe.apiml.common.ignoringSsl");
            return createIgnoringSslSocketFactory();
        }
    }

    private ConnectionSocketFactory createIgnoringSslSocketFactory() {
        return new SSLConnectionSocketFactory(createIgnoringSslContext(), new NoopHostnameVerifier());
    }

    private SSLContext createIgnoringSslContext() {
        try {
            KeyStore emptyKeystore = KeyStore.getInstance(KeyStore.getDefaultType());
            emptyKeystore.load(null, null);
            usedKeyStore = emptyKeystore;
            return new SSLContextBuilder()
                .loadTrustMaterial(null, (certificate, authType) -> true)
                .loadKeyMaterial(emptyKeystore, null)
                .setProtocol(config.getProtocol()).build();
        } catch (Exception e) {
            apimlLog.log("org.zowe.apiml.common.errorInitSsl", e.getMessage());
            throw new HttpsConfigError("Error initializing SSL/TLS context: " + e.getMessage(), e,
                ErrorCode.SSL_CONTEXT_INITIALIZATION_FAILED, config);
        }
    }

    private void loadTrustMaterial(SSLContextBuilder sslContextBuilder)
        throws NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
        if (StringUtils.isNotEmpty(config.getTrustStore())) {
            sslContextBuilder.setKeyStoreType(config.getTrustStoreType()).setProtocol(config.getProtocol());

            if (!SecurityUtils.isKeyring(config.getTrustStore())) {
                if (config.getTrustStorePassword() == null) {
                    apimlLog.log("org.zowe.apiml.common.truststorePasswordNotDefined");
                    throw new HttpsConfigError("server.ssl.trustStorePassword configuration parameter is not defined",
                        ErrorCode.TRUSTSTORE_PASSWORD_NOT_DEFINED, config);
                }

                log.info("Loading trust store file: " + config.getTrustStore());
                File trustStoreFile = new File(config.getTrustStore());

                sslContextBuilder.loadTrustMaterial(trustStoreFile, config.getTrustStorePassword());
            } else {
                log.info("Original truststore keyring URL from configuration: " + config.getTrustStore());
                URL keyRingUrl = SecurityUtils.keyRingUrl(config.getTrustStore());
                log.info("Loading trusted certificates from keyring: " + keyRingUrl);
                sslContextBuilder.loadTrustMaterial(keyRingUrl, config.getTrustStorePassword());
            }
        } else {
            if (config.isTrustStoreRequired()) {
                apimlLog.log("org.zowe.apiml.common.truststoreNotDefined");
                throw new HttpsConfigError(
                    "server.ssl.trustStore configuration parameter is not defined but trust store is required",
                    ErrorCode.TRUSTSTORE_NOT_DEFINED, config);
            } else {
                log.info("No trust store is defined");
            }
        }
    }

    private void loadKeyMaterial(SSLContextBuilder sslContextBuilder) throws NoSuchAlgorithmException,
        KeyStoreException, CertificateException, IOException, UnrecoverableKeyException {
        if (StringUtils.isNotEmpty(config.getKeyStore())) {
            sslContextBuilder.setKeyStoreType(config.getKeyStoreType()).setProtocol(config.getProtocol());

            if (!SecurityUtils.isKeyring(config.getKeyStore())) {
                loadKeystoreMaterial(sslContextBuilder);
            } else {
                loadKeyringMaterial(sslContextBuilder);
            }
        } else {
            log.info("No keystore is defined and empty will be used.");
            KeyStore emptyKeystore = KeyStore.getInstance(KeyStore.getDefaultType());
            emptyKeystore.load(null, null);
            usedKeyStore = emptyKeystore;
            sslContextBuilder.loadKeyMaterial(emptyKeystore, null);
        }
    }

    private void loadKeystoreMaterial(SSLContextBuilder sslContextBuilder) throws UnrecoverableKeyException,
        NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
        if (StringUtils.isEmpty(config.getKeyStore())) {
            apimlLog.log("org.zowe.apiml.common.keystoreNotDefined");
            throw new HttpsConfigError("server.ssl.keyStore configuration parameter is not defined",
                ErrorCode.KEYSTORE_NOT_DEFINED, config);
        }
        if (config.getKeyStorePassword() == null) {
            apimlLog.log("org.zowe.apiml.common.keystorePasswordNotDefined");
            throw new HttpsConfigError("server.ssl.keyStorePassword configuration parameter is not defined",
                ErrorCode.KEYSTORE_PASSWORD_NOT_DEFINED, config);
        }
        log.info("Loading keystore file: " + config.getKeyStore());
        File keyStoreFile = new File(config.getKeyStore());
        sslContextBuilder.loadKeyMaterial(
            keyStoreFile, config.getKeyStorePassword(), config.getKeyPassword(),
            getPrivateKeyStrategy()
        );
    }

    private PrivateKeyStrategy getPrivateKeyStrategy() {
        return config.getKeyAlias() != null ? (aliases, socket) -> config.getKeyAlias() : null;
    }

    private void loadKeyringMaterial(SSLContextBuilder sslContextBuilder) throws UnrecoverableKeyException,
        NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
        log.info("Original keyring URL from configuration: " + config.getKeyStore());
        URL keyRingUrl = SecurityUtils.keyRingUrl(config.getKeyStore());
        log.info("Loading keyring from updated URL: " + keyRingUrl);
        sslContextBuilder.loadKeyMaterial(keyRingUrl, config.getKeyStorePassword(),
            config.getKeyPassword(), getPrivateKeyStrategy());
    }

    private synchronized SSLContext createSecureSslContext() {
        log.debug("Protocol: {}", config.getProtocol());
        SSLContextBuilder sslContextBuilder = SSLContexts.custom();
        try {
            loadTrustMaterial(sslContextBuilder);
            loadKeyMaterial(sslContextBuilder);
            secureSslContext = sslContextBuilder.build();
            validateSslConfig();
            return secureSslContext;
        } catch (NoSuchAlgorithmException | KeyStoreException | CertificateException | IOException
                 | UnrecoverableKeyException | KeyManagementException e) {
            log.error("error", e);
            apimlLog.log("org.zowe.apiml.common.sslContextInitializationError", e.getMessage());
            throw new HttpsConfigError("Error initializing SSL Context: " + e.getMessage(), e,
                ErrorCode.HTTP_CLIENT_INITIALIZATION_FAILED, config);
        }
    }

    private void validateSslConfig() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        if (StringUtils.isNotEmpty(config.getKeyAlias())) {
            KeyStore ks = SecurityUtils.loadKeyStore(config);
            if (!ks.containsAlias(config.getKeyAlias())) {
                apimlLog.log("org.zowe.apiml.common.invalidKeyAlias", config.getKeyAlias());
                throw new HttpsConfigError(String.format("Invalid key alias '%s'", config.getKeyAlias()), ErrorCode.WRONG_KEY_ALIAS, config);
            }
        }
    }

    private ConnectionSocketFactory getSSLConnectionSocketFactory() {
        return new SSLConnectionSocketFactory(
            createSecureSslContext(),
            config.getEnabledProtocols(), null,
            getHostnameVerifier()
        );
    }

    public SSLContext getSslContext() {
        if (config.isVerifySslCertificatesOfServices()) {
            return createSecureSslContext();
        } else {
            return createIgnoringSslContext();
        }
    }

    public HostnameVerifier getHostnameVerifier() {
        if (config.isVerifySslCertificatesOfServices() && !config.isNonStrictVerifySslCertificatesOfServices()) {
            return SSLConnectionSocketFactory.getDefaultHostnameVerifier();
        } else {
            return new NoopHostnameVerifier();
        }
    }

    public EurekaJerseyClientBuilder createEurekaJerseyClientBuilder(String eurekaServerUrl, String serviceId, boolean attlsEnabled) {
        EurekaJerseyClientBuilder builder = new EurekaJerseyClientBuilder();
        builder.withClientName(serviceId);
        builder.withMaxTotalConnections(10);
        builder.withMaxConnectionsPerHost(10);
        builder.withConnectionIdleTimeout(10);
        builder.withConnectionTimeout(5000);
        builder.withReadTimeout(5000);
        // See:
        // https://github.com/Netflix/eureka/blob/master/eureka-core/src/main/java/com/netflix/eureka/transport/JerseyReplicationClient.java#L160
        if (eurekaServerUrl.startsWith("http://")) {
            if (!attlsEnabled) {
                apimlLog.log("org.zowe.apiml.common.insecureHttpWarning");
            }
        } else {
            builder.withCustomSSL(getSslContext());
            builder.withHostnameVerifier(getHostnameVerifier());
        }
        return builder;
    }
}
