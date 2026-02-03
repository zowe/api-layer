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

import org.infinispan.spring.embedded.provider.SpringEmbeddedCacheManager;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

@Component
@ConditionalOnProperty(name = "caching.storage.mode", havingValue = "infinispan")
public class CachesHealthIndicator extends AbstractHealthIndicator {

    private final AtomicReference<CacheManager> cacheManager = new AtomicReference<>();

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        var cm = cacheManager.get();
        if (cm == null) {
            builder.unknown();
            return;
        }

        boolean health = true;
        if (cm instanceof SpringEmbeddedCacheManager springEmbeddedCacheManager) {
            var nativeCacheManager = springEmbeddedCacheManager.getNativeCacheManager();
            var status = nativeCacheManager.getStatus();

            var infinispan = new HashMap<String, Object>();
            infinispan.put("status", status);

            health &= status.allowInvocations();
            var caches = new HashMap<String, Object>();
            for (String cacheName : cm.getCacheNames()) {
                var cacheStatus = nativeCacheManager.getCache(cacheName).getStatus();
                caches.put(cacheName, cacheStatus);
                health &= cacheStatus.allowInvocations();
            }
            infinispan.put("caches", caches);
            builder.withDetail("infinispan", infinispan);
        }

        builder.status(health ? Status.UP : Status.DOWN);
    }

    @EventListener
    public void onApplicationEvent(ApplicationReadyEvent event) {
        var context = event.getApplicationContext();
        cacheManager.set(context.getBean(CacheManager.class));
    }

}
