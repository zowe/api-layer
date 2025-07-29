/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.config;

import io.netty.handler.ssl.SslContext;
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
import org.zowe.apiml.security.common.util.ConnectionUtil;
import reactor.netty.http.client.HttpClient;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final HttpConfig config;

    private static final ApimlLogger apimlLog = ApimlLogger.of(WebClientConfig.class, YamlMessageServiceInstance.getInstance());

    @Bean
    HttpClientFactory gatewayHttpClientFactory(
        HttpClientProperties properties,
        ServerProperties serverProperties, List<HttpClientCustomizer> customizers,
        HttpClientSslConfigurer sslConfigurer
    ) {
        SslContext sslContext;
        try {
            sslContext = ConnectionUtil.getSslContext(config, false);
        } catch (Exception e) {
            apimlLog.log("org.zowe.apiml.common.sslContextInitializationError", e.getMessage());
            throw new HttpsConfigError("Error initializing SSL Context: " + e.getMessage(), e,
                HttpsConfigError.ErrorCode.HTTP_CLIENT_INITIALIZATION_FAILED, config.httpsConfig());
        }
        return new HttpClientFactory(properties, serverProperties, sslConfigurer, customizers) {
            @Override
            protected HttpClient createInstance() {
                return super.createInstance()
                    .secure(sslContextSpec -> sslContextSpec.sslContext(sslContext))
                    .resolver(DefaultAddressResolverGroup.INSTANCE);
            }
        };
    }

    HttpClient getHttpClient(HttpClient httpClient, boolean useClientCert) {
        try {
            return ConnectionUtil.getHttpClient(config, httpClient, useClientCert);
        } catch (Exception e) {
            apimlLog.log("org.zowe.apiml.common.sslContextInitializationError", e.getMessage());
            throw new HttpsConfigError("Error initializing SSL Context: " + e.getMessage(), e,
                HttpsConfigError.ErrorCode.HTTP_CLIENT_INITIALIZATION_FAILED, config.httpsConfig());
        }
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
