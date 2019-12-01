/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.client.configuration;

import com.ca.mfaas.message.core.MessageService;
import com.ca.mfaas.message.yaml.YamlMessageServiceInstance;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {
    @Bean
    public MessageService messageService() {
        MessageService messageService = YamlMessageServiceInstance.getInstance();
        messageService.loadMessages("/apiml-common-messages.yml");
        messageService.loadMessages("/api-messages.yml");
        messageService.loadMessages("/log-messages.yml");
        return messageService;
    }
}
