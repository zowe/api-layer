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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.eureka.RestClientTimeoutProperties;
import org.springframework.cloud.netflix.eureka.http.DefaultEurekaClientHttpRequestFactorySupplier;
import org.springframework.cloud.netflix.eureka.http.RestClientDiscoveryClientOptionalArgs;
import org.springframework.cloud.netflix.eureka.http.RestClientTransportClientFactories;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.message.yaml.YamlMessageServiceInstance;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

@Configuration
public class DiscoveryRestTemplateConfig {

    private static final ApimlLogger apimlLog = ApimlLogger.of(DiscoveryRestTemplateConfig.class, YamlMessageServiceInstance.getInstance());

    @Bean
    public RestClientTransportClientFactories restTemplateTransportClientFactories(RestClientDiscoveryClientOptionalArgs restClientDiscoveryClientOptionalArgs) {
        return new RestClientTransportClientFactories(restClientDiscoveryClientOptionalArgs);
    }

    @Bean
    public RestClientDiscoveryClientOptionalArgs defaultArgs(@Value("${eureka.client.serviceUrl.defaultZone}") String eurekaServerUrl,
                                                             @Qualifier("secureSslContext") SSLContext secureSslContext,
                                                             HostnameVerifier secureHostnameVerifier
    ) {
        RestClientDiscoveryClientOptionalArgs clientArgs = new RestClientDiscoveryClientOptionalArgs(getDefaultEurekaClientHttpRequestFactorySupplier(), RestClient::builder);

        if (eurekaServerUrl.startsWith("http://")) {
            apimlLog.log("org.zowe.apiml.common.insecureHttpWarning");
        } else {
            clientArgs.setSSLContext(secureSslContext);
            clientArgs.setHostnameVerifier(secureHostnameVerifier);
        }

        return clientArgs;
    }

    private static DefaultEurekaClientHttpRequestFactorySupplier getDefaultEurekaClientHttpRequestFactorySupplier() {
        RestClientTimeoutProperties properties = new RestClientTimeoutProperties();
        properties.setConnectTimeout(180000);
        properties.setConnectRequestTimeout(180000);
        properties.setSocketTimeout(180000);
        return new DefaultEurekaClientHttpRequestFactorySupplier(properties);
    }

}
