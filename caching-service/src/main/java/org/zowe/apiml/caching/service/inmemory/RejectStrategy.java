/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.caching.service.inmemory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.EvictionStrategy;
import org.zowe.apiml.caching.service.Messages;
import org.zowe.apiml.caching.service.StorageException;
import org.zowe.apiml.caching.service.inmemory.config.InMemoryConfig;

import java.util.Map;

@RequiredArgsConstructor
@Slf4j
public class RejectStrategy implements EvictionStrategy {
    private final Map<String, Map<String, KeyValue>> storage;
    private final InMemoryConfig inMemoryConfig;

    @Override
    public boolean aboveThreshold() {
        int currentSize = 0;
        for (Map.Entry<String, Map<String, KeyValue>> serviceStorage: storage.entrySet()) {
            currentSize += serviceStorage.getValue().size();
        }

        log.info("Current Size {}.", currentSize);

        return currentSize >= inMemoryConfig.getMaxDataSize();
    }

    @Override
    public void evict(String key) {
        throw new StorageException(Messages.INSUFFICIENT_STORAGE.getKey(), Messages.INSUFFICIENT_STORAGE.getStatus(), key);
    }
}
