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

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.cloud.gateway.config.HttpClientFactory;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.cloud.gateway.config.HttpClientSslConfigurer;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import io.netty.resolver.DefaultAddressResolverGroup;

import java.security.KeyStore;
import java.util.List;
import org.springframework.cloud.gateway.config.HttpClientProperties;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.zowe.apiml.security.HttpsConfigError;
import org.zowe.apiml.security.SecurityUtils;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

@Configuration
public class WebClientConfig {


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

}
