/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.web;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.security.ApimlPoolingHttpClientConnectionManager;
import org.zowe.apiml.security.HttpsConfig;
import org.zowe.apiml.security.HttpsConfigError;
import org.zowe.apiml.security.HttpsFactory;
import org.zowe.apiml.security.SecurityUtils;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Supplier;

@Slf4j
@Configuration
@RequiredArgsConstructor
@Getter
public class HttpConfig implements InitializingBean {

    private static final char[] KEYRING_PASSWORD = "password".toCharArray();

    @Value("${apiml.service.ssl.protocol:${server.ssl.protocol:TLSv1.2}}")
    private String protocol;

    @Value("${apiml.service.ssl.enabled-protocols:TLSv1.2,TLSv1.3}")
    private String[] supportedProtocols;

    @Value("${apiml.service.ssl.ciphers:${server.ssl.ciphers:TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256,TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256,TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384,TLS_AES_128_GCM_SHA256,TLS_AES_256_GCM_SHA384}}")
    private String[] ciphers;

    @Value("${apiml.service.ssl.trust-store:${server.ssl.trustStore:#{null}}}")
    private String trustStorePath;

    @Value("${apiml.service.ssl.trust-store-password:${server.ssl.trustStorePassword:#{null}}}")
    private char[] trustStorePassword;

    @Value("${apiml.service.ssl.trust-store-type:${server.ssl.trustStoreType:PKCS12}}")
    private String trustStoreType;

    @Value("${apiml.service.ssl.key-alias:${server.ssl.keyAlias:#{null}}}")
    private String keyAlias;

    @Value("${apiml.service.ssl.key-store:${server.ssl.keyStore:#{null}}}")
    private String keyStorePath;

    @Value("${apiml.service.ssl.key-store-password:${server.ssl.keyStorePassword:#{null}}}")
    private char[] keyStorePassword;

    @Value("${apiml.service.ssl.key-password:${server.ssl.keyPassword:#{null}}}")
    private char[] keyPassword;

    @Value("${apiml.service.ssl.key-store-type:${server.ssl.keyStoreType:PKCS12}}")
    private String keyStoreType;

    @Value("${apiml.security.ssl.verifySslCertificatesOfServices:true}")
    private boolean verifySslCertificatesOfServices;

    @Value("${apiml.security.ssl.nonStrictVerifySslCertificatesOfServices:false}")
    private boolean nonStrictVerifySslCertificatesOfServices;

    @Value("${apiml.service.ssl.trust-store-required:${server.ssl.trustStoreRequired:false}}")
    private boolean trustStoreRequired;

    @Value("${server.maxConnectionsPerRoute:#{10}}")
    private Integer maxConnectionsPerRoute;

    @Value("${server.maxTotalConnections:#{100}}")
    private Integer maxTotalConnections;

    @Value("${apiml.connection.idleConnectionTimeoutSeconds:#{5}}")
    private int idleConnTimeoutSeconds;

    @Value("${apiml.connection.timeout:#{60000}}")
    private int requestConnectionTimeout;

    @Value("${apiml.connection.timeToLive:#{60000}}")
    private int timeToLive;

    private final Timer connectionManagerTimer = new Timer("ApimlHttpClientConfiguration.connectionManagerTimer", true);

    private CloseableHttpClient secureHttpClient;
    private CloseableHttpClient secureHttpClientWithoutKeystore;
    private HttpsConfig httpsConfig;
    private HttpsFactory httpsFactory;
    private SSLContext secureSslContext;
    private SSLContext secureSslContextWithoutKeystore;
    private HostnameVerifier secureHostnameVerifier;
    private Set<String> publicKeyCertificatesBase64;
    private final ApplicationContext context;

    void updateStorePaths() {
        if (SecurityUtils.isKeyring(keyStorePath)) {
            keyStorePath = SecurityUtils.formatKeyringUrl(keyStorePath);
            if (keyStorePassword == null) keyStorePassword = KEYRING_PASSWORD;
        }
        if (SecurityUtils.isKeyring(trustStorePath)) {
            trustStorePath = SecurityUtils.formatKeyringUrl(trustStorePath);
            if (trustStorePassword == null) trustStorePassword = KEYRING_PASSWORD;
        }

        ServerProperties serverProperties = context.getBean(ServerProperties.class);
        if (serverProperties.getSsl() != null) {
            String serverKeyStore = serverProperties.getSsl().getKeyStore();
            if (SecurityUtils.isKeyring(serverKeyStore)) {
                serverProperties.getSsl().setKeyStore(SecurityUtils.formatKeyringUrl(serverKeyStore));
            }
            String serverTrustStore = serverProperties.getSsl().getTrustStore();
            if (SecurityUtils.isKeyring(serverTrustStore)) {
                serverProperties.getSsl().setTrustStore(SecurityUtils.formatKeyringUrl(serverTrustStore));
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }

    public void init() {
        updateStorePaths();

        try {
            X509Certificate certificate = null;
            if (StringUtils.isNotBlank(keyStorePath)) {
                var ks = SecurityUtils.loadKeyStore(keyStoreType, keyStorePath, keyStorePassword);
                certificate = (X509Certificate) ks.getCertificate(keyAlias);
            }
            Supplier<HttpsConfig.HttpsConfigBuilder> httpsConfigSupplier = () ->
                HttpsConfig.builder()
                    .protocol(protocol).enabledProtocols(supportedProtocols).cipherSuite(ciphers)
                    .trustStore(trustStorePath).trustStoreType(trustStoreType)
                    .trustStorePassword(trustStorePassword).trustStoreRequired(trustStoreRequired)
                    .verifySslCertificatesOfServices(verifySslCertificatesOfServices)
                    .nonStrictVerifySslCertificatesOfServices(nonStrictVerifySslCertificatesOfServices)
                    .maxConnectionsPerRoute(maxConnectionsPerRoute).maxTotalConnections(maxTotalConnections)
                    .idleConnTimeoutSeconds(idleConnTimeoutSeconds).requestConnectionTimeout(requestConnectionTimeout)
                    .timeToLive(timeToLive);

            httpsConfig = httpsConfigSupplier.get()
                .keyAlias(keyAlias).keyStore(keyStorePath).keyPassword(keyPassword)
                .keyStorePassword(keyStorePassword).keyStoreType(keyStoreType).certificate(certificate)
                .build();

            HttpsConfig httpsConfigWithoutKeystore = httpsConfigSupplier.get().build();

            log.debug("Using HTTPS configuration: {}", httpsConfig.toString());

            httpsFactory = new HttpsFactory(httpsConfig);
            ApimlPoolingHttpClientConnectionManager secureConnectionManager = getConnectionManager(httpsFactory);
            secureHttpClient = httpsFactory.buildHttpClient(secureConnectionManager);
            secureSslContext = httpsFactory.getSslContext();
            secureHostnameVerifier = httpsFactory.getHostnameVerifier();
            HttpsFactory factoryWithoutKeystore = new HttpsFactory(httpsConfigWithoutKeystore);
            ApimlPoolingHttpClientConnectionManager connectionManagerWithoutKeystore = getConnectionManager(factoryWithoutKeystore);
            secureHttpClientWithoutKeystore = factoryWithoutKeystore.buildHttpClient(connectionManagerWithoutKeystore);
            secureSslContextWithoutKeystore = factoryWithoutKeystore.getSslContext();

            publicKeyCertificatesBase64 = SecurityUtils.loadCertificateChainBase64(httpsConfig);
        } catch (HttpsConfigError e) {
            log.error("Invalid configuration of HTTPs: {}", e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            log.error("Cannot construct configuration of HTTPs: {}", e.getMessage());
            System.exit(1);
        }
    }

    public ApimlPoolingHttpClientConnectionManager getConnectionManager(HttpsFactory factory) {
        RegistryBuilder<ConnectionSocketFactory> socketFactoryRegistryBuilder = RegistryBuilder
            .<ConnectionSocketFactory>create().register("http", PlainConnectionSocketFactory.getSocketFactory());
        socketFactoryRegistryBuilder.register("https", factory.createSslSocketFactory());
        Registry<ConnectionSocketFactory> socketFactoryRegistry = socketFactoryRegistryBuilder.build();
        ApimlPoolingHttpClientConnectionManager connectionManager = new ApimlPoolingHttpClientConnectionManager(socketFactoryRegistry, timeToLive);
        ConnectionConfig connConfig = ConnectionConfig.custom()
            .setConnectTimeout(Timeout.ofMilliseconds(requestConnectionTimeout))
            .setSocketTimeout(Timeout.ofMilliseconds(requestConnectionTimeout))
            .setTimeToLive(Timeout.ofMilliseconds(timeToLive))
            .build();
        this.connectionManagerTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                connectionManager.closeExpired();
                connectionManager.closeIdle(Timeout.ofSeconds(idleConnTimeoutSeconds));
            }
        }, 30000, 30000);

        connectionManager.setDefaultConnectionConfig(connConfig);
        connectionManager.setDefaultMaxPerRoute(maxConnectionsPerRoute);
        connectionManager.setMaxTotal(maxTotalConnections);

        return connectionManager;
    }

    @Bean
    Set<String> publicKeyCertificatesBase64() {
        return publicKeyCertificatesBase64;
    }

    @Bean
    public HttpsConfig httpsConfig() {
        return httpsConfig;
    }

    @Bean
    public HttpsFactory httpsFactory() {
        return httpsFactory;
    }

    /**
     * Returns RestTemplate with keystore. This RestTemplate makes calls to other systems with a certificate to sign to
     * other systems by certificate. It is necessary to call systems like DiscoverySystem etc.
     *
     * @return RestTemplate, which uses certificate from keystore to authenticate
     */
    @Bean
    @Primary
    RestTemplate restTemplateWithKeystore() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(secureHttpClient);
        factory.setConnectionRequestTimeout(requestConnectionTimeout);
        factory.setConnectTimeout(requestConnectionTimeout);
        return new RestTemplate(factory);
    }

    /**
     * Returns RestTemplate without keystore. The purpose is to call z/OSMF (or other systems), which accept login by
     * certificate. In case of login into z/OSMF can certificate has higher priority. It breaks credentials
     * verification.
     *
     * @return default RestTemplate, which doesn't use certificate from keystore
     */
    @Bean
    RestTemplate restTemplateWithoutKeystore() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(secureHttpClientWithoutKeystore);
        factory.setConnectionRequestTimeout(requestConnectionTimeout);
        factory.setConnectTimeout(requestConnectionTimeout);
        return new RestTemplate(factory);
    }

    /**
     * @return HttpClient which use a certificate to authenticate
     */
    @Bean("secureHttpClientWithKeystore")
    @Primary
    CloseableHttpClient secureHttpClient() {
        return secureHttpClient;
    }

    /**
     * @return HttpClient, which doesn't use a certificate to authenticate
     */
    @Bean
    CloseableHttpClient secureHttpClientWithoutKeystore() {
        return secureHttpClientWithoutKeystore;
    }

    @Bean
    public SSLContext secureSslContext() {
        return secureSslContext;
    }

    @Bean
    SSLContext secureSslContextWithoutKeystore() {
        return secureSslContextWithoutKeystore;
    }

    @Bean
    HostnameVerifier secureHostnameVerifier() {
        return secureHostnameVerifier;
    }

}
