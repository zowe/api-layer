/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.config;

import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageServiceInstance;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * General beans setup and creation class for Discovery service
 */
@Configuration
public class BeanConfig {

    @Bean
    @Primary
    public MessageService messageServiceDiscovery() {
        MessageService messageService = YamlMessageServiceInstance.getInstance();
        messageService.loadMessages("/common-log-messages.yml");
        messageService.loadMessages("/security-common-log-messages.yml");
        messageService.loadMessages("/discovery-log-messages.yml");
        return messageService;
    }
}
