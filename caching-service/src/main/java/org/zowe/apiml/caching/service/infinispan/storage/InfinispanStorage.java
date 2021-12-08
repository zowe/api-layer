/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.caching.service.infinispan.storage;

import lombok.RequiredArgsConstructor;
import org.infinispan.manager.DefaultCacheManager;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.Storage;

import java.util.Map;


@RequiredArgsConstructor
public class InfinispanStorage implements Storage {

    private final DefaultCacheManager defaultCacheManager;

    @Override
    public KeyValue create(String serviceId, KeyValue toCreate) {

        Object o = defaultCacheManager.getCache("myCache").putIfAbsent(toCreate.getKey(), toCreate);
        return toCreate;
    }

    @Override
    public KeyValue read(String serviceId, String key) {
        return (KeyValue) defaultCacheManager.getCache("myCache").get(key);
    }

    @Override
    public KeyValue update(String serviceId, KeyValue toUpdate) {
        defaultCacheManager.getCache("myCache").put(toUpdate.getKey(), toUpdate);
        return

            null;
    }

    @Override
    public KeyValue delete(String serviceId, String toDelete) {
        return (KeyValue) defaultCacheManager.getCache("myCache").remove(toDelete);
    }

    @Override
    public Map<String, KeyValue> readForService(String serviceId) {
        return null;
    }

    @Override
    public void deleteForService(String serviceId) {

    }
}
