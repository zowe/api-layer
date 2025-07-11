/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.caching;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.zowe.apiml.cache.Storage;
import org.zowe.apiml.caching.model.KeyValue;
import reactor.core.publisher.Mono;

import static reactor.core.publisher.Mono.empty;

/**
 * {@code CachingServiceClientApi} is the internal implementation of {@link CachingServiceClient}
 * <p>
 * Unlike {@code CachingServiceClientRest}, which makes HTTP requests to the Caching service, it directly uses the storage methods.
 * </p>
 * <p>
 * This bean is only active when {@code modulithConfig} is present in the Spring context.
 * </p>
 *
 * @see CachingServiceClient
 * @see CachingServiceClientRest
 */
@Component
@ConditionalOnBean(name = "modulithConfig")
@Slf4j
@RequiredArgsConstructor
public class CachingServiceClientApi implements CachingServiceClient {
    private final Storage storage;

    @Override
    public Mono<Void> create(ApiKeyValue keyValue) {
        String serviceId = extractServiceId(keyValue.getKey());
        storage.create(serviceId, mapToApiKeyValue(keyValue));
        return empty();
    }

    @Override
    public Mono<Void> update(ApiKeyValue keyValue) {
        String serviceId = extractServiceId(keyValue.getKey());
        storage.update(serviceId, mapToApiKeyValue(keyValue));
        return empty();
    }

    @Override
    public Mono<ApiKeyValue> read(String key) {

        String serviceId = extractServiceId(key);
        KeyValue stored = storage.read(serviceId, key);
        if (stored != null) {
            return Mono.just(new ApiKeyValue(stored.getKey(), stored.getValue()));
        } else {
            return empty();
        }

    }

    @Override
    public Mono<Void> delete(String key) {
        String serviceId = extractServiceId(key);
        storage.delete(serviceId, key);
        return empty();

    }

    private KeyValue mapToApiKeyValue(ApiKeyValue apiValue) {
        return new KeyValue(apiValue.getKey(), apiValue.getValue());
    }

    /**
     * Extracts serviceId from the full key
     */
    private String extractServiceId(String key) {
        if (!key.startsWith(LoadBalancerCache.LOAD_BALANCER_KEY_PREFIX)) {
            throw new IllegalArgumentException("Missing prefix in key: " + key);
        }
        String prefixRemoved = key.substring(LoadBalancerCache.LOAD_BALANCER_KEY_PREFIX.length());
        int colonPos = prefixRemoved.indexOf(':');
        if (colonPos == -1) {
            throw new IllegalArgumentException("Invalid key format, cannot extract serviceId: " + key);
        }
        return prefixRemoved.substring(colonPos + 1);
    }

}
