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

import com.netflix.discovery.AbstractDiscoveryClientOptionalArgs;
import com.netflix.discovery.shared.transport.jersey.EurekaJerseyClient;
import com.netflix.discovery.shared.transport.jersey.EurekaJerseyClientImpl.EurekaJerseyClientBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.util.Timeout;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.security.*;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.time.Duration;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Configuration
public class HttpConfig {

    private static final char[] KEYRING_PASSWORD = "password".toCharArray();

    @Value("${server.ssl.protocol:TLSv1.2}")
    private String protocol;
    @Value("${apiml.httpclient.ssl.enabled-protocols:TLSv1.2,TLSv1.3}")
    private String[] supportedProtocols;
    @Value("${server.ssl.ciphers:TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256,TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256,TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384,TLS_AES_128_GCM_SHA256,TLS_AES_256_GCM_SHA384}")
    private String[] ciphers;

    @Value("${server.ssl.trustStore:#{null}}")
    private String trustStore;

    @Value("${server.ssl.trustStorePassword:#{null}}")
    private char[] trustStorePassword;

    @Value("${server.ssl.trustStoreType:PKCS12}")
    private String trustStoreType;

    @Value("${server.ssl.keyAlias:#{null}}")
    private String keyAlias;

    @Value("${server.ssl.keyStore:#{null}}")
    private String keyStore;

    @Value("${server.ssl.keyStorePassword:#{null}}")
    private char[] keyStorePassword;

    @Value("${server.ssl.keyPassword:#{null}}")
    private char[] keyPassword;

    @Value("${server.ssl.keyStoreType:PKCS12}")
    private String keyStoreType;

    @Value("${apiml.security.ssl.verifySslCertificatesOfServices:true}")
    private boolean verifySslCertificatesOfServices;

    @Value("${apiml.security.ssl.nonStrictVerifySslCertificatesOfServices:false}")
    private boolean nonStrictVerifySslCertificatesOfServices;

    @Value("${spring.application.name}")
    private String serviceId;

    @Value("${server.ssl.trustStoreRequired:false}")
    private boolean trustStoreRequired;

    @Value("${eureka.client.serviceUrl.defaultZone}")
    private String eurekaServerUrl;

    @Value("${server.maxConnectionsPerRoute:#{10}}")
    private Integer maxConnectionsPerRoute;

    @Value("${server.maxTotalConnections:#{100}}")
    private Integer maxTotalConnections;

    @Value("${apiml.httpclient.conn-pool.idleConnTimeoutSeconds:#{5}}")
    private int idleConnTimeoutSeconds;
    @Value("${apiml.httpclient.conn-pool.requestConnectionTimeout:#{10000}}")
    private int requestConnectionTimeout;
    @Value("${apiml.httpclient.conn-pool.readTimeout:#{10000}}")
    private int readTimeout;
    @Value("${apiml.httpclient.conn-pool.timeToLive:#{10000}}")
    private int timeToLive;

    private CloseableHttpClient secureHttpClient;
    private CloseableHttpClient secureHttpClientWithoutKeystore;
    private SSLContext secureSslContext;
    private HostnameVerifier secureHostnameVerifier;
    private EurekaJerseyClientBuilder eurekaJerseyClientBuilder;
    private final Timer connectionManagerTimer = new Timer(
        "ApimlHttpClientConfiguration.connectionManagerTimer", true);

    private Set<String> publicKeyCertificatesBase64;

    @Resource
    private AbstractDiscoveryClientOptionalArgs<?> optionalArgs;

    void updateStorePaths() {
        if (SecurityUtils.isKeyring(keyStore)) {
            keyStore = SecurityUtils.formatKeyringUrl(keyStore);
            if (keyStorePassword == null) keyStorePassword = KEYRING_PASSWORD;
        }
        if (SecurityUtils.isKeyring(trustStore)) {
            trustStore = SecurityUtils.formatKeyringUrl(trustStore);
            if (trustStorePassword == null) trustStorePassword = KEYRING_PASSWORD;
        }
    }

    @PostConstruct
    public void init() {
        updateStorePaths();

        try {
            Supplier<HttpsConfig.HttpsConfigBuilder> httpsConfigSupplier = () ->
                HttpsConfig.builder()
                    .protocol(protocol).enabledProtocols(supportedProtocols).cipherSuite(ciphers)
                    .trustStore(trustStore).trustStoreType(trustStoreType)
                    .trustStorePassword(trustStorePassword).trustStoreRequired(trustStoreRequired)
                    .verifySslCertificatesOfServices(verifySslCertificatesOfServices)
                    .nonStrictVerifySslCertificatesOfServices(nonStrictVerifySslCertificatesOfServices)
                    .maxConnectionsPerRoute(maxConnectionsPerRoute).maxTotalConnections(maxTotalConnections)
                    .idleConnTimeoutSeconds(idleConnTimeoutSeconds).requestConnectionTimeout(requestConnectionTimeout)
                    .timeToLive(timeToLive);

            HttpsConfig httpsConfig = httpsConfigSupplier.get()
                .keyAlias(keyAlias).keyStore(keyStore).keyPassword(keyPassword)
                .keyStorePassword(keyStorePassword).keyStoreType(keyStoreType)
                .build();

            HttpsConfig httpsConfigWithoutKeystore = httpsConfigSupplier.get().build();

            log.info("Using HTTPS configuration: {}", httpsConfig.toString());

            HttpsFactory factory = new HttpsFactory(httpsConfig);
            ApimlPoolingHttpClientConnectionManager secureConnectionManager = getConnectionManager(factory);
            secureHttpClient = factory.createSecureHttpClient(secureConnectionManager);
            secureSslContext = factory.getSslContext();
            secureHostnameVerifier = factory.getHostnameVerifier();
            eurekaJerseyClientBuilder = factory.createEurekaJerseyClientBuilder(eurekaServerUrl, serviceId);
            optionalArgs.setEurekaJerseyClient(eurekaJerseyClient());
            HttpsFactory factoryWithoutKeystore = new HttpsFactory(httpsConfigWithoutKeystore);
            ApimlPoolingHttpClientConnectionManager connectionManagerWithoutKeystore = getConnectionManager(factoryWithoutKeystore);
            secureHttpClientWithoutKeystore = factoryWithoutKeystore.createSecureHttpClient(connectionManagerWithoutKeystore);

            factory.setSystemSslProperties();

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
//        this.connectionManagerTimer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                connectionManager.closeExpiredConnections();
//                connectionManager.closeIdleConnections(idleConnTimeoutSeconds, TimeUnit.SECONDS);
//            }
//        }, 30000, 30000);
        ConnectionConfig connConfig = ConnectionConfig.custom()
            .setConnectTimeout(Timeout.ofMilliseconds(requestConnectionTimeout))
            .setSocketTimeout(Timeout.ofMilliseconds(requestConnectionTimeout))
            .setTimeToLive(Timeout.ofMilliseconds(timeToLive))
            .build();
        connectionManager.setDefaultConnectionConfig(connConfig);
        connectionManager.setDefaultMaxPerRoute(maxConnectionsPerRoute);
        connectionManager.setMaxTotal(maxTotalConnections);
        return connectionManager;
    }

    @Bean
    @Qualifier("publicKeyCertificatesBase64")
    public Set<String> publicKeyCertificatesBase64() {
        return publicKeyCertificatesBase64;
    }

    private void setTruststore(SslContextFactory sslContextFactory) {
        if (StringUtils.isNotEmpty(trustStore)) {
            sslContextFactory.setTrustStorePath(SecurityUtils.formatKeyringUrl(trustStore));
            sslContextFactory.setTrustStoreType(trustStoreType);
            sslContextFactory.setTrustStorePassword(trustStorePassword == null ? null : String.valueOf(trustStorePassword));
        }
    }

    @Bean
    @Qualifier("jettyClientSslContextFactory")
    public SslContextFactory.Client jettyClientSslContextFactory() {
        SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
        sslContextFactory.setProtocol(protocol);
        sslContextFactory.setExcludeCipherSuites("^.*_(MD5|SHA|SHA1)$", "^TLS_RSA_.*$");
        setTruststore(sslContextFactory);
        log.debug("jettySslContextFactory: {}", sslContextFactory.dump());
        sslContextFactory.setHostnameVerifier(secureHostnameVerifier());
        if (!verifySslCertificatesOfServices) {
            sslContextFactory.setTrustAll(true);
        }

        return sslContextFactory;
    }

    /**
     * Returns RestTemplate with keystore. This RestTemplate makes calls to other systems with a certificate to sign to
     * other systems by certificate. It is necessary to call systems like DiscoverySystem etc.
     *
     * @return RestTemplate, which uses certificate from keystore to authenticate
     */
    @Bean
    @Primary
    @Qualifier("restTemplateWithKeystore")
    public RestTemplate restTemplateWithKeystore(RestTemplateBuilder builder) {
//        return builder.requestFactory(HttpComponentsClientHttpRequestFactory.class)
//            .setConnectTimeout(Duration.ofMillis(requestConnectionTimeout))
//            .setReadTimeout(Duration.ofMillis(readTimeout))
//            .build();


        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(secureHttpClient);
  //      factory.setReadTimeout(readTimeout);
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
    @Qualifier("restTemplateWithoutKeystore")
    public RestTemplate restTemplateWithoutKeystore() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(secureHttpClientWithoutKeystore);
    //    factory.setReadTimeout(readTimeout);
        factory.setConnectTimeout(requestConnectionTimeout);
        return new RestTemplate(factory);
    }

    /**
     * @return HttpClient which use a certificate to authenticate
     */
    @Bean
    @Primary
    @Qualifier("secureHttpClientWithKeystore")
    public CloseableHttpClient secureHttpClient() {
        return secureHttpClient;
    }

    /**
     * @return HttpClient, which doesn't use a certificate to authenticate
     */
    @Bean
    @Qualifier("secureHttpClientWithoutKeystore")
    public CloseableHttpClient secureHttpClientWithoutKeystore() {
        return secureHttpClientWithoutKeystore;
    }

    @Bean
    public SSLContext secureSslContext() {
        return secureSslContext;
    }

    @Bean
    public HostnameVerifier secureHostnameVerifier() {
        return secureHostnameVerifier;
    }

    @Bean
    public EurekaJerseyClient eurekaJerseyClient() {
        return eurekaJerseyClientBuilder.build();
    }

    @Bean
    public EurekaJerseyClientBuilder eurekaJerseyClientBuilder() {
        return eurekaJerseyClientBuilder;
    }

}
