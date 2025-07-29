/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.config;

import com.google.common.annotations.VisibleForTesting;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.resolver.DefaultAddressResolverGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.cloud.gateway.config.HttpClientFactory;
import org.springframework.cloud.gateway.config.HttpClientProperties;
import org.springframework.cloud.gateway.config.HttpClientSslConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.message.yaml.YamlMessageServiceInstance;
import org.zowe.apiml.product.web.HttpConfig;
import org.zowe.apiml.security.HttpsConfigError;
import org.zowe.apiml.security.SecurityUtils;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientSecurityUtils;
import reactor.netty.tcp.SslProvider;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import java.net.Socket;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final HttpConfig config;

    private static final ApimlLogger apimlLog = ApimlLogger.of(WebClientConfig.class, YamlMessageServiceInstance.getInstance());

    @VisibleForTesting
    X509KeyManager x509KeyManagerSelectedAlias(KeyManagerFactory keyManagerFactory) {
        return new X509KeyManagerSelectedAlias(keyManagerFactory, config.getKeyAlias());
    }

    /**
     * @return io.netty.handler.ssl.SslContext for http client.
     */
    SslContext getSslContext(boolean setKeystore) {
        try {
            SslContextBuilder builder = SslContextBuilder.forClient();

            KeyStore trustStore = SecurityUtils.loadKeyStore(
                config.getTrustStoreType(), config.getTrustStorePath(), config.getTrustStorePassword());
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);
            builder.trustManager(trustManagerFactory);

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            if (setKeystore) {
                log.info("Loading keystore: {}: {}", config.getKeyStoreType(), config.getKeyStorePath());
                KeyStore keyStore = SecurityUtils.loadKeyStore(
                    config.getKeyStoreType(), config.getKeyStorePath(), config.getKeyStorePassword());
                keyManagerFactory.init(keyStore, config.getKeyStorePassword());
                builder.keyManager(x509KeyManagerSelectedAlias(keyManagerFactory));
            } else {
                KeyStore emptyKeystore = KeyStore.getInstance(KeyStore.getDefaultType());
                emptyKeystore.load(null, null);
                keyManagerFactory.init(emptyKeystore, null);
                builder.keyManager(keyManagerFactory);
            }

            if (config.isVerifySslCertificatesOfServices() && config.isNonStrictVerifySslCertificatesOfServices()) {
                builder.endpointIdentificationAlgorithm(null);
            }

            return builder.build();
        } catch (Exception e) {
            apimlLog.log("org.zowe.apiml.common.sslContextInitializationError", e.getMessage());
            throw new HttpsConfigError("Error initializing SSL Context: " + e.getMessage(), e,
                HttpsConfigError.ErrorCode.HTTP_CLIENT_INITIALIZATION_FAILED, config.httpsConfig());
        }
    }

    @Bean
    HttpClientFactory gatewayHttpClientFactory(
        HttpClientProperties properties,
        ServerProperties serverProperties, List<HttpClientCustomizer> customizers,
        HttpClientSslConfigurer sslConfigurer
    ) {
        SslContext sslContext = getSslContext(false);
        return new HttpClientFactory(properties, serverProperties, sslConfigurer, customizers) {
            @Override
            protected HttpClient createInstance() {
                return super.createInstance()
                    .secure(sslContextSpec -> sslContextSpec.sslContext(sslContext))
                    .resolver(DefaultAddressResolverGroup.INSTANCE);
            }
        };
    }

    public HttpClient getHttpClient(HttpClient httpClient, boolean useClientCert) {
        var sslContextBuilder = SslProvider.builder().sslContext(getSslContext(useClientCert));
        if (!config.isNonStrictVerifySslCertificatesOfServices()) {
            sslContextBuilder.handlerConfigurator(HttpClientSecurityUtils.HOSTNAME_VERIFICATION_CONFIGURER);
        }
        return httpClient.secure(sslContextBuilder.build());
    }

    @Bean
    @Primary
    WebClient webClient(HttpClient httpClient) {
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(getHttpClient(httpClient, false)))
            .build();
    }

    @Bean
    WebClient webClientClientCert(HttpClient httpClient) {
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(getHttpClient(httpClient, true)))
            .build();
    }

    static class X509KeyManagerSelectedAlias implements X509KeyManager {

        private final X509KeyManager originalKm;
        private final String keyAlias;

        X509KeyManagerSelectedAlias(KeyManagerFactory keyManagerFactory, String keyAlias) {
            this.originalKm = (X509KeyManager) keyManagerFactory.getKeyManagers()[0];
            this.keyAlias = keyAlias;
        }

        @Override
        public String[] getClientAliases(String keyType, Principal[] issuers) {
            return originalKm.getClientAliases(keyType, issuers);
        }

        @Override
        public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
            if (keyAlias != null) {
                return keyAlias;
            }
            return originalKm.chooseClientAlias(keyType, issuers, socket);
        }

        @Override
        public String[] getServerAliases(String keyType, Principal[] issuers) {
            return originalKm.getServerAliases(keyType, issuers);
        }

        @Override
        public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
            if (keyAlias != null) {
                return keyAlias;
            }
            return originalKm.chooseServerAlias(keyType, issuers, socket);
        }

        @Override
        public X509Certificate[] getCertificateChain(String alias) {
            return originalKm.getCertificateChain(alias);
        }

        @Override
        public PrivateKey getPrivateKey(String alias) {
            return originalKm.getPrivateKey(alias);
        }

    }

}
