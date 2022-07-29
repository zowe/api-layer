/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.service.inmemory.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zowe.apiml.caching.service.Storage;
import org.zowe.apiml.caching.service.inmemory.InMemoryStorage;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.log.ApimlLogger;

@Configuration
@RequiredArgsConstructor
public class InMemoryConfiguration {
    private final InMemoryConfig inMemoryConfig;

    @ConditionalOnProperty(name = "caching.storage.mode", havingValue = "inMemory", matchIfMissing = true)
    @Bean
    public Storage inMemory(MessageService messageService) {
        ApimlLogger.of(InMemoryConfig.class, messageService).log("org.zowe.apiml.cache.usingInMemory");

        return new InMemoryStorage(inMemoryConfig, messageService);
    }
}
