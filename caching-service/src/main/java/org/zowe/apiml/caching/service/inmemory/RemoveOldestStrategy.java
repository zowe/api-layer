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
import org.zowe.apiml.caching.service.inmemory.config.InMemoryConfig;

import java.util.Map;

@RequiredArgsConstructor
@Slf4j
public class RemoveOldestStrategy implements EvictionStrategy {
    private final Map<String, Map<String, KeyValue>> storage;
    private final InMemoryConfig inMemoryConfig;

    @Override
    public void evict(String key) {
        // Find the oldest key.
        // I need to store the information about the age

        // It needs to be in metadata.
        // Is there any way to get this information for VSAM?
    }
}
