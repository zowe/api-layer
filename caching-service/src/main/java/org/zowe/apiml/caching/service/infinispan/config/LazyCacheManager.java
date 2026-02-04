/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.service.infinispan.config;

import lombok.RequiredArgsConstructor;
import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class LazyCacheManager implements CacheContainer {

    private final Supplier<CacheContainer> cacheContainerSupplier;

    private final AtomicReference<CacheContainer> cacheContainer = new AtomicReference<>();

    private CacheContainer getCacheContainer() {
        var container = cacheContainer.get();
        if (container == null) {
            synchronized (this) {
                container = cacheContainer.get();
                if (container == null) {
                    container = cacheContainerSupplier.get();
                    cacheContainer.set(container);
                }
            }
            if (container == null) {
                throw new IllegalStateException("Cache container is not initialized yet");
            }
        }
        return container;
    }

    @Override
    public <K, V> Cache<K, V> getCache() {
        return getCacheContainer().getCache();
    }

    @Override
    public <K, V> Cache<K, V> getCache(String cacheName) {
        return getCacheContainer().getCache(cacheName);
    }

    @Override
    public Set<String> getCacheNames() {
        return getCacheContainer().getCacheNames();
    }

    @Override
    public void start() {
        getCacheContainer().start();
    }

    @Override
    public void stop() {
        getCacheContainer().stop();
    }

    public <T extends CacheContainer> T getOriginal() {
        return (T) getCacheContainer();
    }

}
