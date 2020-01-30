/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.security;

import com.ca.mfaas.message.log.ApimlLogger;
import com.ca.mfaas.message.yaml.YamlMessageServiceInstance;
import com.ca.mfaas.security.HttpsConfigError.ErrorCode;
import com.netflix.discovery.shared.transport.jersey.EurekaJerseyClientImpl.EurekaJerseyClientBuilder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Objects;

@Slf4j
@Data
@NoArgsConstructor
public class HttpsFactory {

    private HttpsConfig config;
    private SSLContext secureSslContext;
    private ApimlLogger apimlLog;

    public HttpsFactory(HttpsConfig httpsConfig) {
        this.config = httpsConfig;
        this.secureSslContext = null;
        this.apimlLog = ApimlLogger.of(HttpsFactory.class, YamlMessageServiceInstance.getInstance());
    }

    public CloseableHttpClient createSecureHttpClient() {
        Registry<ConnectionSocketFactory> socketFactoryRegistry;
        RegistryBuilder<ConnectionSocketFactory> socketFactoryRegistryBuilder = RegistryBuilder
                .<ConnectionSocketFactory>create().register("http", PlainConnectionSocketFactory.getSocketFactory());

        socketFactoryRegistryBuilder.register("https", createSslSocketFactory());
        socketFactoryRegistry = socketFactoryRegistryBuilder.build();

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(
                Objects.requireNonNull(socketFactoryRegistry));
        return HttpClientBuilder.create().setConnectionManager(connectionManager).disableCookieManagement()
                .disableAuthCaching().build();
    }

    public ConnectionSocketFactory createSslSocketFactory() {
        if (config.isVerifySslCertificatesOfServices()) {
            return createSecureSslSocketFactory();
        } else {
            apimlLog.log("apiml.common.ignoringSsl");
            return createIgnoringSslSocketFactory();
        }
    }

    private ConnectionSocketFactory createIgnoringSslSocketFactory() {
        return new SSLConnectionSocketFactory(createIgnoringSslContext(), new NoopHostnameVerifier());
    }

    private SSLContext createIgnoringSslContext() {
        try {
            return new SSLContextBuilder().loadTrustMaterial(null, (certificate, authType) -> true).setProtocol(config.getProtocol()).build();
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            apimlLog.log("apiml.common.errorInitSsl", e.getMessage());
            throw new HttpsConfigError("Error initializing SSL/TLS context: " + e.getMessage(), e,
                    ErrorCode.SSL_CONTEXT_INITIALIZATION_FAILED, config);
        }
    }

    private void loadTrustMaterial(SSLContextBuilder sslContextBuilder)
            throws NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
        if (config.getTrustStore() != null) {
            sslContextBuilder.setKeyStoreType(config.getTrustStoreType()).setProtocol(config.getProtocol());

            if (!config.getTrustStore().startsWith(SecurityUtils.SAFKEYRING)) {
                if (config.getTrustStorePassword() == null) {
                    apimlLog.log("apiml.common.truststorePasswordNotDefined");
                    throw new HttpsConfigError("server.ssl.trustStorePassword configuration parameter is not defined",
                            ErrorCode.TRUSTSTORE_PASSWORD_NOT_DEFINED, config);
                }

                log.info("Loading trust store file: " + config.getTrustStore());
                File trustStoreFile = new File(config.getTrustStore());

                sslContextBuilder.loadTrustMaterial(trustStoreFile, config.getTrustStorePassword().toCharArray());
            } else {
                log.info("Loading trust store key ring: " + config.getTrustStore());
                sslContextBuilder.loadTrustMaterial(keyRingUrl(config.getTrustStore()),
                        config.getTrustStorePassword() == null ? null : config.getTrustStorePassword().toCharArray());
            }
        } else {
            if (config.isTrustStoreRequired()) {
                apimlLog.log("apiml.common.truststoreNotDefined");
                throw new HttpsConfigError(
                        "server.ssl.trustStore configuration parameter is not defined but trust store is required",
                        ErrorCode.TRUSTSTORE_NOT_DEFINED, config);
            } else {
                log.info("No trust store is defined");
            }
        }
    }

    private URL keyRingUrl(String uri) throws MalformedURLException {
        return SecurityUtils.keyRingUrl(uri, config.getTrustStore());
    }

    private void loadKeyMaterial(SSLContextBuilder sslContextBuilder) throws NoSuchAlgorithmException,
            KeyStoreException, CertificateException, IOException, UnrecoverableKeyException {
        if (config.getKeyStore() != null) {
            sslContextBuilder.setKeyStoreType(config.getKeyStoreType()).setProtocol(config.getProtocol());

            if (!config.getKeyStore().startsWith(SecurityUtils.SAFKEYRING)) {
                loadKeystoreMaterial(sslContextBuilder);
            } else {
                loadKeyringMaterial(sslContextBuilder);
            }
        } else {
            log.info("No key store is defined");
        }
    }

    private void loadKeystoreMaterial(SSLContextBuilder sslContextBuilder) throws UnrecoverableKeyException,
            NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
        if (config.getKeyStore() == null) {
            apimlLog.log("apiml.common.keystoreNotDefined");
            throw new HttpsConfigError("server.ssl.keyStore configuration parameter is not defined",
                    ErrorCode.KEYSTORE_NOT_DEFINED, config);
        }
        if (config.getKeyStorePassword() == null) {
            apimlLog.log("apiml.common.keystorePasswordNotDefined");
            throw new HttpsConfigError("server.ssl.keyStorePassword configuration parameter is not defined",
                    ErrorCode.KEYSTORE_PASSWORD_NOT_DEFINED, config);
        }
        log.info("Loading key store file: " + config.getKeyStore());
        File keyStoreFile = new File(config.getKeyStore());
        sslContextBuilder.loadKeyMaterial(keyStoreFile,
                config.getKeyStorePassword() == null ? null : config.getKeyStorePassword().toCharArray(),
                config.getKeyPassword() == null ? null : config.getKeyPassword().toCharArray());
    }

    private void loadKeyringMaterial(SSLContextBuilder sslContextBuilder) throws UnrecoverableKeyException,
            NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
        log.info("Loading trust key ring: " + config.getKeyStore());
        sslContextBuilder.loadKeyMaterial(keyRingUrl(config.getKeyStore()),
                config.getKeyStorePassword() == null ? null : config.getKeyStorePassword().toCharArray(),
                config.getKeyPassword() == null ? null : config.getKeyPassword().toCharArray(), null);
    }

    private synchronized SSLContext createSecureSslContext() {
        if (secureSslContext == null) {
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
                apimlLog.log("apiml.common.sslContextInitializationError", e.getMessage());
                throw new HttpsConfigError("Error initializing SSL Context: " + e.getMessage(), e,
                        ErrorCode.HTTP_CLIENT_INITIALIZATION_FAILED, config);
            }
        } else {
            return secureSslContext;
        }
    }

    private void validateSslConfig() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        if (config.getKeyAlias() != null) {
            KeyStore ks = SecurityUtils.loadKeyStore(config);
            if (!ks.containsAlias(config.getKeyAlias())) {
                apimlLog.log("apiml.common.invalidKeyAlias", config.getKeyAlias());
                throw new HttpsConfigError(String.format("Invalid key alias '%s'", config.getKeyAlias()), ErrorCode.WRONG_KEY_ALIAS, config);
            }
        }
    }

    private ConnectionSocketFactory createSecureSslSocketFactory() {
        return new SSLConnectionSocketFactory(createSecureSslContext(),
                SSLConnectionSocketFactory.getDefaultHostnameVerifier());
    }

    public SSLContext createSslContext() {
        if (config.isVerifySslCertificatesOfServices()) {
            return createSecureSslContext();
        } else {
            return createIgnoringSslContext();
        }
    }

    private void setSystemProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    public void setSystemSslProperties() {
        setSystemProperty("javax.net.ssl.keyStore", SecurityUtils.replaceFourSlashes(config.getKeyStore()));
        setSystemProperty("javax.net.ssl.keyStorePassword", config.getKeyStorePassword());
        setSystemProperty("javax.net.ssl.keyStoreType", config.getKeyStoreType());

        setSystemProperty("javax.net.ssl.trustStore", SecurityUtils.replaceFourSlashes(config.getTrustStore()));
        setSystemProperty("javax.net.ssl.trustStorePassword", config.getTrustStorePassword());
        setSystemProperty("javax.net.ssl.trustStoreType", config.getTrustStoreType());
    }

    public HostnameVerifier createHostnameVerifier() {
        if (config.isVerifySslCertificatesOfServices()) {
            return SSLConnectionSocketFactory.getDefaultHostnameVerifier();
        } else {
            return new NoopHostnameVerifier();
        }
    }

    public EurekaJerseyClientBuilder createEurekaJerseyClientBuilder(String eurekaServerUrl, String serviceId) {
        EurekaJerseyClientBuilder builder = new EurekaJerseyClientBuilder();
        builder.withClientName(serviceId);
        builder.withMaxTotalConnections(10);
        builder.withMaxConnectionsPerHost(10);

        // See:
        // https://github.com/Netflix/eureka/blob/master/eureka-core/src/main/java/com/netflix/eureka/transport/JerseyReplicationClient.java#L160
        if (eurekaServerUrl.startsWith("http://")) {
            apimlLog.log("apiml.common.insecureHttpWarning");
        } else {
            System.setProperty("com.netflix.eureka.shouldSSLConnectionsUseSystemSocketFactory", "true");
            setSystemSslProperties();
            builder.withCustomSSL(createSecureSslContext());
            builder.withHostnameVerifier(createHostnameVerifier());
        }
        return builder;
    }
}
