/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.discovery;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.discovery.AbstractDiscoveryClientOptionalArgs;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClientConfig;
import org.springframework.cloud.netflix.eureka.CloudEurekaClient;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Custom implementation of Eureka client. It support additional feature:
 * - fetchRegistry - invoke asynchronous task to update registry from discovery client immediatelly
 */
public class ApimlDiscoveryClient extends CloudEurekaClient {

    protected ScheduledExecutorService scheduler;

    protected Runnable cacheRefresh;

    public ApimlDiscoveryClient(
        ApplicationInfoManager applicationInfoManager,
        EurekaClientConfig config,
        AbstractDiscoveryClientOptionalArgs<?> args,
        ApplicationEventPublisher publisher
    ) {
        super(applicationInfoManager, config, args, publisher);
        init();
    }

    public void init() {
        try {
            // take scheduler and save in local variable to work with in the future
            Field schedulerField = DiscoveryClient.class.getDeclaredField("scheduler");
            schedulerField.setAccessible(true);
            scheduler = (ScheduledExecutorService) schedulerField.get(this);

            // find class with process to fetch from discovery server and construct instance for call to fetch
            Optional<Class<?>> cacheRefreshClass = Arrays.stream(DiscoveryClient.class.getDeclaredClasses())
                .filter(x -> "CacheRefreshThread".equals(x.getSimpleName())).findFirst();
            if (!cacheRefreshClass.isPresent()) throw new NoSuchMethodException();
            Constructor cacheRefreshConstructor = cacheRefreshClass.get().getDeclaredConstructor(DiscoveryClient.class);
            cacheRefreshConstructor.setAccessible(true);
            cacheRefresh = (Runnable) cacheRefreshConstructor.newInstance(this);
        } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException | NoSuchFieldException e) {
            throw new IllegalArgumentException("Implementation of discovery client was changed. Please review implementation of manual registry fetching");
        }
    }

    public void fetchRegistry() {
        scheduler.schedule(cacheRefresh, 0, TimeUnit.NANOSECONDS);
    }

}
