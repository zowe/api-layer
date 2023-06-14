/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.service.vsam.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zowe.apiml.caching.service.Storage;
import org.zowe.apiml.caching.service.vsam.EvictionStrategyProducer;
import org.zowe.apiml.caching.service.vsam.VsamInitializer;
import org.zowe.apiml.caching.service.vsam.VsamStorage;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.log.ApimlLogger;

@Configuration
@RequiredArgsConstructor
public class VsamConfiguration {
    private final VsamConfig vsamConfig;
    private final VsamInitializer vsamInitializer;

    @ConditionalOnProperty(name = "caching.storage.mode", havingValue = "vsam")
    @Bean
    public Storage vsam(MessageService messageService, EvictionStrategyProducer evictionStrategyProducer) {
        return new VsamStorage(vsamConfig, vsamInitializer, ApimlLogger.of(VsamStorage.class, messageService), evictionStrategyProducer);
    }
}
