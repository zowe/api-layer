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

import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.Storage;

import java.util.HashMap;
import java.util.Map;

public class InMemoryStorage implements Storage {
    private Map<String, Map<String, KeyValue>> storage = new HashMap<>();

    public InMemoryStorage() {}

    protected InMemoryStorage(Map<String, Map<String, KeyValue>> storage) {
        this.storage = storage;
    }

    @Override
    public KeyValue create(String serviceId, KeyValue toCreate) {
        storage.computeIfAbsent(serviceId, k -> new HashMap<>());
        Map<String, KeyValue> serviceStorage = storage.get(serviceId);
        serviceStorage.put(toCreate.getKey(), toCreate);
        return toCreate;
    }

    @Override
    public KeyValue read(String serviceId, String key) {
        Map<String, KeyValue> serviceSpecificStorage = storage.get(serviceId);
        if (serviceSpecificStorage == null) {
            return null;
        }

        return serviceSpecificStorage.get(key);
    }

    @Override
    public KeyValue update(String serviceId, KeyValue toUpdate) {
        return create(serviceId, toUpdate);
    }

    @Override
    public KeyValue delete(String serviceId, String toDelete) {
        Map<String, KeyValue> serviceSpecificStorage = storage.get(serviceId);
        if (serviceSpecificStorage == null) {
            return null;
        }

        return serviceSpecificStorage.remove(toDelete);
    }

    @Override
    public Map<String, KeyValue> readForService(String serviceId) {
        return storage.get(serviceId);
    }
}
