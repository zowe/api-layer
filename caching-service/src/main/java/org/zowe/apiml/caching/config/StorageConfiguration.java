/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.caching.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zowe.apiml.caching.service.Storage;
import org.zowe.apiml.caching.service.inmemory.InMemoryStorage;
import org.zowe.apiml.caching.service.vsam.VsamStorage;

@Configuration
@RequiredArgsConstructor
public class StorageConfiguration {

    private final VsamConfig vsamConfig;

    @ConditionalOnProperty(name = "caching.storage.mode", havingValue = "vsam")
    @Bean
    public Storage vsam() {
        return new VsamStorage(vsamConfig,false);
    }

    @ConditionalOnMissingBean(Storage.class)
    @Bean
    public Storage inMemory() {
        return new InMemoryStorage();
    }
}
