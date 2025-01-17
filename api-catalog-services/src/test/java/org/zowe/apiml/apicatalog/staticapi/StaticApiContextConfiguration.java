/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.staticapi;

import org.springframework.context.annotation.Bean;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;

import static org.mockito.Mockito.mock;

public class StaticApiContextConfiguration {

    @Bean
    public StaticAPIService staticAPIService() {
        return mock(StaticAPIService.class);
    }

    @Bean
    public MessageService messageService() {
        return new YamlMessageService("/apicatalog-log-messages.yml");
    }

    @Bean
    public StaticAPIRefreshControllerExceptionHandler staticAPIRefreshControllerExceptionHandler(MessageService messageService) {
        return new StaticAPIRefreshControllerExceptionHandler(messageService);
    }

    @Bean
    public StaticAPIRefreshController apiCatalogController(StaticAPIService staticAPIService) {
        return new StaticAPIRefreshController(staticAPIService);
    }

    @Bean
    public StaticDefinitionGenerator staticDefinitionGenerator() {
        return mock(StaticDefinitionGenerator.class);
    }

    @Bean
    public StaticDefinitionControllerExceptionHandler staticDefinitionControllerExceptionHandler(MessageService messageService) {
        return new StaticDefinitionControllerExceptionHandler(messageService);
    }

    @Bean
    public StaticDefinitionController staticAPIRefreshController(StaticDefinitionGenerator staticDefinitionGenerator) {
        return new StaticDefinitionController(staticDefinitionGenerator);
    }
}
