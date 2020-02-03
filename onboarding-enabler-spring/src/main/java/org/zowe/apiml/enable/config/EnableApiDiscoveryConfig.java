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

import org.zowe.apiml.eurekaservice.client.ApiMediationClient;
import org.zowe.apiml.eurekaservice.client.config.ApiMediationServiceConfig;
import org.zowe.apiml.eurekaservice.client.impl.ApiMediationClientImpl;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageServiceInstance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;


@Configuration
@ComponentScan(value = {"org.zowe.apiml.enable"})
public class EnableApiDiscoveryConfig {

    @Bean
    public MessageService messageServiceDiscovery() {
        MessageService messageService = YamlMessageServiceInstance.getInstance();
        messageService.loadMessages("/onboarding-enabler-spring-messages.yml");

        return messageService;
    }

    @Bean
    public ApiMediationClient apiMediationClient() {
       return new ApiMediationClientImpl();
    }

    @ConfigurationProperties(prefix = "apiml.service")
    @Bean
    public ApiMediationServiceConfig apiMediationServiceConfig() {
        return new ApiMediationServiceConfig();
    }

    @Value("${apiml.enabled:false}")
    private boolean enabled;

    @Bean
    public Boolean apimlEnabled() {
        return enabled;
    }
}
