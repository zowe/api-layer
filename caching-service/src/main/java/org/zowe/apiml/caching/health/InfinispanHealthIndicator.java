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

import org.apache.commons.lang3.StringUtils;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.remoting.transport.Address;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "caching.storage.mode", havingValue = "infinispan")
public class InfinispanHealthIndicator extends AbstractHealthIndicator {

    @Value("${caching.storage.infinispan.initialHosts:}")
    private String initialHosts;

    private final AtomicReference<DefaultCacheManager> cacheManager = new AtomicReference<>();

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        DefaultCacheManager cm = cacheManager.get();
        if (cm == null) {
            builder.unknown();
            return;
        }

        boolean health = true;

        Map<String, Object> infinispan = new HashMap<>();

        ComponentStatus status = cm.getStatus();
        infinispan.put("status", status);

        health &= status.allowInvocations();
        Map<String, Object> caches = new HashMap<>();
        for (String cacheName : cm.getCacheNames()) {
            ComponentStatus cacheStatus = cm.getCache(cacheName).getStatus();
            caches.put(cacheName, cacheStatus);
            health &= cacheStatus.allowInvocations();
        }
        infinispan.put("caches", caches);

        String[] initialHostsArray = StringUtils.split(initialHosts, ",");
        boolean allMembers = initialHostsArray.length <= cm.getMembers().size();
        Map<String, Object> cluster = new HashMap<>();
        cluster.put("status", allMembers ? Status.UP : Status.DOWN);
        cluster.put("address", cm.getAddress().toString());
        cluster.put("initialHosts", initialHostsArray);
        cluster.put("members", cm.getMembers().stream().map(Address::toString).collect(Collectors.toList()));
        infinispan.put("cluster", cluster);
        health &= allMembers;

        builder.withDetail("infinispan", infinispan);

        builder.status(health ? Status.UP : Status.DOWN);
    }

    @EventListener
    public void onApplicationEvent(ApplicationReadyEvent event) {
        ConfigurableApplicationContext context = event.getApplicationContext();
        cacheManager.set(context.getBean(DefaultCacheManager.class));
    }

}
