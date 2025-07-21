/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.zowe.apiml.config.ApplicationInfo;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageServiceInstance;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.product.routing.transform.TransformService;

/**
 * General configuration of the API Catalog.
 */
@Configuration("catalogBeanConfig")
public class BeanConfig {

    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "modulithConfig")
    public MessageService messageServiceCatalog() {
        MessageService messageService = YamlMessageServiceInstance.getInstance();
        messageService.loadMessages("/security-client-log-messages.yml");
        messageService.loadMessages("/utility-log-messages.yml");
        messageService.loadMessages("/common-log-messages.yml");
        messageService.loadMessages("/security-common-log-messages.yml");
        messageService.loadMessages("/apicatalog-log-messages.yml");
        return messageService;
    }

    @Bean
    @Lazy
    public TransformService transformService(GatewayClient gatewayClient) {
        return new TransformService(gatewayClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public ApplicationInfo applicationInfo() {
        return ApplicationInfo.builder().isModulith(false).build();
    }

}
