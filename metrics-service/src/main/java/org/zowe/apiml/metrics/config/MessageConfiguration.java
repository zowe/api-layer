/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.metrics.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.zowe.apiml.message.core.MessageService;

@Configuration
public class MessageConfiguration {
    @Bean
    @Primary
    public MessageService messageServiceMetrics(MessageService messageService) {
        messageService.loadMessages("/utility-log-messages.yml");
        messageService.loadMessages("/security-common-log-messages.yml");
        return messageService;
    }
}
