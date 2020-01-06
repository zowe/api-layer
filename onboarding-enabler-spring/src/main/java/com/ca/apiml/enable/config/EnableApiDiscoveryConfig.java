/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.apiml.enable.config;

import com.ca.mfaas.eurekaservice.client.impl.ApiMediationClientImpl;
import com.ca.mfaas.message.core.MessageService;
import com.ca.mfaas.message.yaml.YamlMessageServiceInstance;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(value = {"com.ca.apiml.enable"})
public class EnableApiDiscoveryConfig {

    @Bean
    public MessageService messageServiceDiscovery() {
        MessageService messageService = YamlMessageServiceInstance.getInstance();
        messageService.loadMessages("/onboarding-enabler-spring-messages.yml");

        return messageService;
    }

    @Bean
    public ApiMediationClientImpl apiMediationClient() {
       return new ApiMediationClientImpl();
    }
}
