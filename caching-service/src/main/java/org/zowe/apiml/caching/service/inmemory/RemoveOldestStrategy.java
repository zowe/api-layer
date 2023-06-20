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

import java.util.Map;

@RequiredArgsConstructor
@Slf4j
public class RemoveOldestStrategy implements EvictionStrategy {
    private final Map<String, Map<String, KeyValue>> storage;

    @Override
    public void evict(String key) {
        KeyValue oldest = null;
        Map<String, KeyValue> mapStoringOldest = null;
        for (Map.Entry<String, Map<String, KeyValue>> serviceStorage: storage.entrySet()) {
            Map<String, KeyValue> services = serviceStorage.getValue();
            for (Map.Entry<String, KeyValue> specificValue: services.entrySet()) {
                KeyValue current = specificValue.getValue();
                if (oldest == null) {
                    oldest = current;
                    mapStoringOldest = services;
                    continue;
                }

                long oldestCreated = Long.parseLong(oldest.getCreated());
                long currentCreated = Long.parseLong(current.getCreated());

                if (oldestCreated > currentCreated) {
                    oldest = current;
                    mapStoringOldest = services;
                }
            }
        }

        if (oldest != null) {
            mapStoringOldest.remove(oldest.getKey());
        }
    }
}
