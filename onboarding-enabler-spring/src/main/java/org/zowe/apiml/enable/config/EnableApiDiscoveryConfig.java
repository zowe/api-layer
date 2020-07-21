/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.enable.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.zowe.apiml.eurekaservice.client.ApiMediationClient;
import org.zowe.apiml.eurekaservice.client.EurekaClientConfigProvider;
import org.zowe.apiml.eurekaservice.client.EurekaClientProvider;
import org.zowe.apiml.eurekaservice.client.config.ApiMediationServiceConfig;
import org.zowe.apiml.eurekaservice.client.impl.ApiMediationClientImpl;
import org.zowe.apiml.eurekaservice.client.impl.DiscoveryClientProvider;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageServiceInstance;
import org.zowe.apiml.product.logging.annotations.EnableApimlLogger;


@Configuration
@ComponentScan(value = {"org.zowe.apiml.enable"})
@EnableApimlLogger
public class EnableApiDiscoveryConfig {

    @Bean
    public MessageService messageServiceDiscovery() {
        MessageService messageService = YamlMessageServiceInstance.getInstance();
        messageService.loadMessages("/onboarding-enabler-spring-messages.yml");

        return messageService;
    }

    @ConditionalOnMissingBean(EurekaClientProvider.class)
    @Bean
    public ApiMediationClient defaultApiMediationClient() {
        return new ApiMediationClientImpl();
    }

    @ConditionalOnBean(EurekaClientProvider.class)
    @Bean
    public ApiMediationClient apiMediationClient(EurekaClientProvider eurekaClientProvider) {
        if (eurekaClientProvider == null) {
            return new ApiMediationClientImpl();
        }
        return new ApiMediationClientImpl(eurekaClientProvider);
    }

    @ConditionalOnBean(name = "EurekaClientProvider.class, EurekaClientConfigProvider.class")
    @Bean
    public ApiMediationClient apiMediationClient(EurekaClientProvider eurekaClientProvider, EurekaClientConfigProvider eurekaClientConfigProvider) {
        if (eurekaClientProvider != null) {
            if (eurekaClientConfigProvider != null) {
                return new ApiMediationClientImpl(eurekaClientProvider, eurekaClientConfigProvider);
            } else {
                return new ApiMediationClientImpl(eurekaClientProvider);
            }
        } else {
            if (eurekaClientConfigProvider != null) {
                return new ApiMediationClientImpl(new DiscoveryClientProvider(), eurekaClientConfigProvider);
            } else {
                return new ApiMediationClientImpl();
            }
        }
    }

    @ConfigurationProperties(prefix = "apiml.service")
    @Bean
    public ApiMediationServiceConfig apiMediationServiceConfig() {
        return new ApiMediationServiceConfig();
    }
}
