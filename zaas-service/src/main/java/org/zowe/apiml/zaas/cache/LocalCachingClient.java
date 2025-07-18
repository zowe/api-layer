/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.cache;

import lombok.RequiredArgsConstructor;
import org.zowe.apiml.cache.Storage;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.security.HttpsConfig;

import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class LocalCachingClient implements CachingClient {

    private final Storage storage;

    private final HttpsConfig httpsConfig;

    @Override
    public void create(CachingServiceClient.KeyValue kv) {

        var serviceId = getServiceId();
        storage.create(serviceId, new KeyValue(kv.getKey(), kv.getValue()));
    }

    @Override
    public void appendList(String mapKey, CachingServiceClient.KeyValue kv) {
        storage.storeMapItem(getServiceId(), mapKey, convert(kv));
    }

    @Override
    public Map<String, Map<String, String>> readAllMaps() {
        return storage.getAllMaps(getServiceId());
    }

    @Override
    public void evictTokens(String key) {
        storage.removeNonRelevantTokens(getServiceId(), key);
    }

    @Override
    public void evictRules(String key) {
        storage.removeNonRelevantRules(getServiceId(), key);
    }

    @Override
    public CachingServiceClient.KeyValue read(String key) {
        return convert(storage.read(getServiceId(), key));
    }

    @Override
    public void update(CachingServiceClient.KeyValue kv) {
        storage.update(getServiceId(), convert(kv));
    }

    @Override
    public void delete(String key) {
        storage.delete(getServiceId(), key);
    }

    KeyValue convert(CachingServiceClient.KeyValue kv) {
        return new KeyValue(kv.getKey(), kv.getValue());
    }

    CachingServiceClient.KeyValue convert(KeyValue kv) {
        return new CachingServiceClient.KeyValue(kv.getKey(), kv.getValue());
    }

    String getServiceId() {
        return Optional.ofNullable(httpsConfig.getCertificate())
            .map(certificate -> certificate.getSubjectX500Principal().getName())
            .orElse("apiml service");
    }
}
