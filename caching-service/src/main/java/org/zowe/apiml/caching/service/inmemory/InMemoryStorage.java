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

import org.apache.commons.lang3.NotImplementedException;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.Storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class InMemoryStorage implements Storage {

    @Override
    public KeyValue create(KeyValue toCreate) {
        throw new NotImplementedException("Not implemented yet");
    }

    @Override
    public List<KeyValue> read(String[] key) {
        List<KeyValue> currentList = new ArrayList<>();
        currentList.add(new KeyValue("key", "value"));
        return currentList;
    }

    @Override
    public Collection<KeyValue> readForService(String serviceId) {
        throw new NotImplementedException("Not implemented yet");
    }

    @Override
    public KeyValue update(KeyValue toUpdate) {
        throw new NotImplementedException("Not implemented yet");
    }

    @Override
    public KeyValue delete(String[] key) {
        throw new NotImplementedException("Not implemented yet");
    }
}
