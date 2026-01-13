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

import com.netflix.discovery.shared.Application;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.infinispan.spring.embedded.provider.SpringEmbeddedCacheManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.zowe.apiml.eurekaservice.client.ApiMediationClient;
import org.zowe.apiml.product.constants.CoreService;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Caching service health information (/cachingservice/application/health)
 */
@Component
@RequiredArgsConstructor
@ConditionalOnMissingBean(name = "modulithConfig")
public class CachingHealthIndicator extends AbstractHealthIndicator implements ApplicationListener<ApplicationReadyEvent> {

    private final AtomicReference<Boolean> serviceUp = new AtomicReference<>(false);

    @Value("${caching.storage.mode:inMemory}")
    private String storageMode;

    private final ApiMediationClient apiMediationClient;
    private final CacheManager cacheManager;

    private boolean updateInfinispanHealth(Health.Builder builder, SpringEmbeddedCacheManager springEmbeddedCacheManager) {
        boolean health = true;

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

        return health;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        var eurekaClient = apiMediationClient.getEurekaClient();
        boolean gatewayUp = Optional.ofNullable(eurekaClient.getApplication(CoreService.GATEWAY.getServiceId())).map(Application::getInstances).map(i -> !i.isEmpty()).orElse(false);
        boolean health = serviceUp.get() && gatewayUp;

        if (
            "infinispan".equals(storageMode) &&
            cacheManager instanceof SpringEmbeddedCacheManager springEmbeddedCacheManager
        ) {
            updateInfinispanHealth(builder, springEmbeddedCacheManager);
        }

        Status healthStatus = health ? Status.UP : Status.DOWN;
        builder
            .status(healthStatus)
            .withDetail(CoreService.GATEWAY.getServiceId(), healthStatus.getCode());
    }

    @Override
    public void onApplicationEvent(@Nonnull final ApplicationReadyEvent event) {
        serviceUp.set(true);
    }

}
