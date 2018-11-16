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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Objects;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

@Slf4j
@Data
@NoArgsConstructor
public class HttpsFactory {
    private static final String SAFKEYRING = "safkeyring";

    private HttpsConfig config;
    private SSLContext secureSslContext;

    public HttpsFactory(HttpsConfig httpsConfig) {
        this.config = httpsConfig;
        this.secureSslContext = null;
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
            log.warn("The gateway is not verifying the TLS/SSL certificates of the services");
            return createIgnoringSslSocketFactory();
        }
    }

    private ConnectionSocketFactory createIgnoringSslSocketFactory() {
        return new SSLConnectionSocketFactory(createIgnoringSslContext(), new NoopHostnameVerifier());
    }

    private SSLContext createIgnoringSslContext() {
        try {
            return new SSLContextBuilder().loadTrustMaterial(null, (certificate, authType) -> true).build();
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            throw new HttpsConfigError("Error initializing SSL/TLS context: " + e.getMessage(), e,
                    ErrorCode.SSL_CONTEXT_INITIALIZATION_FAILED, config);
        }
    }

    private void loadTrustMaterial(SSLContextBuilder sslContextBuilder)
            throws NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
        if (config.getTrustStore() != null) {
            sslContextBuilder.setKeyStoreType(config.getTrustStoreType()).setProtocol(config.getProtocol());

            if (!config.getTrustStore().startsWith(SAFKEYRING)) {
                if (config.getTrustStorePassword() == null) {
                    throw new HttpsConfigError("server.ssl.trustStorePassword configuration parameter is not defined",
                            ErrorCode.TRUSTSTORE_PASSWORD_NOT_DEFINED, config);
                }

                log.info("Loading trust store file: " + config.getTrustStore());
                File trustStoreFile = new File(config.getTrustStore());

                sslContextBuilder.loadTrustMaterial(trustStoreFile, config.getTrustStorePassword().toCharArray());
            } else {
                log.info("Loading trust store key ring: " + config.getTrustStore());
                if (!config.getTrustStore().startsWith(SAFKEYRING + ":////")) {
                    throw new MalformedURLException("Incorrect key ring format: " + config.getTrustStore()
                            + ". Make sure you use format safkeyring:////userId/keyRing");
                }
                URL keyRing = new URL(config.getTrustStore().replaceFirst("////", "//"));

                sslContextBuilder.loadTrustMaterial(keyRing, config.getTrustStorePassword().toCharArray());
            }
        } else {
            if (config.isTrustStoreRequired()) {
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
        if (config.getKeyStore() != null) {
            sslContextBuilder.setKeyStoreType(config.getKeyStoreType()).setProtocol(config.getProtocol());

            if (!config.getKeyStore().startsWith(SAFKEYRING)) {
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
            throw new HttpsConfigError("server.ssl.keyStore configuration parameter is not defined",
                    ErrorCode.KEYSTORE_NOT_DEFINED, config);
        }
        if (config.getKeyStorePassword() == null) {
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
        log.info("Loading key ring: " + config.getKeyStore());
        if (!config.getKeyStore().startsWith(SAFKEYRING + ":////")) {
            throw new MalformedURLException("Incorrect key ring format: " + config.getKeyStore()
                    + ". Make sure you use format safkeyring:////userId/keyRing");
        }
        URL keyRing = new URL(config.getKeyStore().replaceFirst("////", "//"));

        sslContextBuilder.loadKeyMaterial(keyRing, config.getKeyStorePassword().toCharArray(),
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
                log.error("Error initializing HTTP client: {}", e.getMessage(), e);
                throw new HttpsConfigError("Error initializing HTTP client: " + e.getMessage(), e,
                        ErrorCode.HTTP_CLIENT_INITIALIZATION_FAILED, config);
            }
        } else {
            return secureSslContext;
        }
    }

    private void validateSslConfig() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        if ((config.getKeyAlias() != null) && !config.getKeyStore().startsWith(SAFKEYRING)) {
            KeyStore ks = KeyStore.getInstance(config.getKeyStoreType());
            log.debug("Validating alias {} in key store: {}", config.getKeyAlias(), config.getKeyStore());
            File keyStoreFile = new File(config.getKeyStore());
            InputStream istream = new FileInputStream(keyStoreFile);
            ks.load(istream, config.getKeyStorePassword() == null ? null : config.getKeyStorePassword().toCharArray());
            if (!ks.containsAlias(config.getKeyAlias())) {
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
        setSystemProperty("javax.net.ssl.keyStore",
                config.getKeyStore() == null ? null : config.getKeyStore().replaceFirst("////", "//"));
        setSystemProperty("javax.net.ssl.keyStorePassword", config.getKeyStorePassword());
        setSystemProperty("javax.net.ssl.keyStoreType", config.getKeyStoreType());

        setSystemProperty("javax.net.ssl.trustStore",
                config.getTrustStore() == null ? null : config.getTrustStore().replaceFirst("////", "//"));
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

        if (eurekaServerUrl.startsWith("http://")) {
            log.warn("Unsecure HTTP is used to connect to Discovery Service");
        } else {
            // Setup HTTPS for Eureka replication client:
            System.setProperty("com.netflix.eureka.shouldSSLConnectionsUseSystemSocketFactory", "true");
            setSystemSslProperties();
            // See:
            // https://github.com/Netflix/eureka/blob/master/eureka-core/src/main/java/com/netflix/eureka/transport/JerseyReplicationClient.java#L160

            // Setup HTTPS for Eureka client:
            builder.withCustomSSL(createSecureSslContext());
            builder.withHostnameVerifier(createHostnameVerifier());
        }
        return builder;
    }
}
