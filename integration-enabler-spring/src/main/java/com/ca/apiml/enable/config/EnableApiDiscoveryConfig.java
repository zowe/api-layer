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

import com.ca.mfaas.message.core.MessageService;
import com.ca.mfaas.message.yaml.YamlMessageServiceInstance;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;

@ComponentScan(value = {"com.ca.apiml.enable"})
public class EnableApiDiscoveryConfig {

    @Bean
    @Primary
    public MessageService messageServiceEnabler() {
        MessageService messageService = YamlMessageServiceInstance.getInstance();
        messageService.loadMessages("/integration-enabler-spring-messages.yml");

        return messageService;
    }
}
