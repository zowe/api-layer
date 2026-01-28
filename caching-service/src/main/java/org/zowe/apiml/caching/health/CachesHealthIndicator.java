/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.health;

import lombok.RequiredArgsConstructor;
import org.infinispan.spring.embedded.provider.SpringEmbeddedCacheManager;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "caching.storage.mode", havingValue = "infinispan")
public class CachesHealthIndicator extends AbstractHealthIndicator {

    private final CacheManager cacheManager;

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        boolean health = true;
        if (cacheManager instanceof SpringEmbeddedCacheManager springEmbeddedCacheManager) {
            var nativeCacheManager = springEmbeddedCacheManager.getNativeCacheManager();
            var status = nativeCacheManager.getStatus();

            var infinispan = new HashMap<String, Object>();
            infinispan.put("status", status);

            health &= status.allowInvocations();
            var caches = new HashMap<String, Object>();
            for (String cacheName : cacheManager.getCacheNames()) {
                var cacheStatus = nativeCacheManager.getCache(cacheName).getStatus();
                caches.put(cacheName, cacheStatus);
                health &= cacheStatus.allowInvocations();
            }
            infinispan.put("caches", caches);
            builder.withDetail("infinispan", infinispan);
        }

        builder.status(health ? Status.UP : Status.DOWN);
    }

}
