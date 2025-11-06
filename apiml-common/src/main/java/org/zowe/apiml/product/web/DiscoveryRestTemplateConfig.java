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

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.eureka.http.EurekaClientHttpRequestFactorySupplier;
import org.springframework.cloud.netflix.eureka.http.RestClientDiscoveryClientOptionalArgs;
import org.springframework.cloud.netflix.eureka.http.RestClientTransportClientFactories;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.message.yaml.YamlMessageServiceInstance;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.util.concurrent.TimeUnit;

@Configuration
public class DiscoveryRestTemplateConfig {

    private static final ApimlLogger apimlLog = ApimlLogger.of(DiscoveryRestTemplateConfig.class, YamlMessageServiceInstance.getInstance());

    @Value("${server.attlsClient.enabled:false}")
    private boolean isClientAttlsEnabled;

    private static int connectTimeout = 180000;
    private static int requestTimeout = 180000;
    private static int socketTimeout = 180000;

    @Bean
    RestClientTransportClientFactories restTemplateTransportClientFactories(RestClientDiscoveryClientOptionalArgs restClientDiscoveryClientOptionalArgs) {
        return new RestClientTransportClientFactories(restClientDiscoveryClientOptionalArgs);
    }

    @Bean
    RestClientDiscoveryClientOptionalArgs defaultArgs(@Value("${eureka.client.serviceUrl.defaultZone}") String eurekaServerUrl,
                                                      @Qualifier("secureSslContext") SSLContext secureSslContext,
                                                      HostnameVerifier secureHostnameVerifier
    ) {
        RestClientDiscoveryClientOptionalArgs clientArgs = new RestClientDiscoveryClientOptionalArgs(getDefaultEurekaClientHttpRequestFactorySupplier(), RestClient::builder);

        if (eurekaServerUrl.startsWith("http://")) {
            if (!isClientAttlsEnabled) {
                apimlLog.log("org.zowe.apiml.common.insecureHttpWarning");
            }
        } else {
            clientArgs.setSSLContext(secureSslContext);
            clientArgs.setHostnameVerifier(secureHostnameVerifier);
        }

        return clientArgs;
    }

    public static EurekaClientHttpRequestFactorySupplier getDefaultEurekaClientHttpRequestFactorySupplier() {
        return (sslContext, hostnameVerifier) -> {
            var requestFactory = new HttpComponentsClientHttpRequestFactory();
            var httpClientBuilder = HttpClients
                .custom()
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.ofSeconds(30))
                .setConnectionManager(buildConnectionManager(sslContext, hostnameVerifier));
            RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();

            requestConfigBuilder.setConnectionRequestTimeout(
                Timeout.of(requestTimeout, TimeUnit.MILLISECONDS));

            httpClientBuilder.setDefaultRequestConfig(requestConfigBuilder.build());

            CloseableHttpClient httpClient = httpClientBuilder.build();

            requestFactory.setHttpClient(httpClient);
            return requestFactory;
        };
    }

    private static HttpClientConnectionManager buildConnectionManager(SSLContext sslContext, HostnameVerifier hostnameVerifier) {
        PoolingHttpClientConnectionManagerBuilder connectionManagerBuilder = PoolingHttpClientConnectionManagerBuilder
            .create();
        connectionManagerBuilder.setTlsSocketStrategy(new DefaultClientTlsStrategy(sslContext, hostnameVerifier));
        connectionManagerBuilder.setDefaultSocketConfig(SocketConfig.custom()
            .setSoTimeout(Timeout.of(socketTimeout, TimeUnit.MILLISECONDS))
            .build());
        connectionManagerBuilder.setDefaultConnectionConfig(ConnectionConfig.custom()
            .setConnectTimeout(Timeout.of(connectTimeout, TimeUnit.MILLISECONDS))
            .build());
        return connectionManagerBuilder.build();
    }


}
