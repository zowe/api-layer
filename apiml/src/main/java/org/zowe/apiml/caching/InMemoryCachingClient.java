/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.zowe.apiml.caching.service.Storage;
import org.zowe.apiml.zaas.cache.CachingClient;
import org.zowe.apiml.zaas.cache.CachingServiceClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class InMemoryCachingClient implements CachingClient {

    private final Storage storage;
    @Override
    public void create(CachingServiceClient.KeyValue kv) {
//            storage.create()
    }

    @Override
    public void appendList(String mapKey, CachingServiceClient.KeyValue kv) {

    }

    @Override
    public Map<String, Map<String, String>> readAllMaps() {
        return Map.of();
    }

    @Override
    public void evictTokens(String key) {

    }

    @Override
    public void evictRules(String key) {

    }

    @Override
    public CachingServiceClient.KeyValue read(String key) {
        return null;
    }

    @Override
    public void update(CachingServiceClient.KeyValue kv) {

    }

    @Override
    public void delete(String key) {

    }
}
