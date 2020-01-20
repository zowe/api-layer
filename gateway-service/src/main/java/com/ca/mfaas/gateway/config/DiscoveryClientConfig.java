/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.config;

import com.ca.mfaas.gateway.discovery.ApimlDiscoveryClient;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.discovery.AbstractDiscoveryClientOptionalArgs;
import com.netflix.discovery.CacheRefreshedEvent;
import com.netflix.discovery.EurekaClientConfig;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.netflix.zuul.filters.RefreshableRouteLocator;
import org.springframework.cloud.netflix.zuul.web.ZuulHandlerMapping;
import org.springframework.cloud.util.ProxyUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * This configuration override bean EurekaClient with custom ApimlDiscoveryClient. This bean offer additional method
 * fetchRegistry. User can call this method to asynchronously fetch new data from discovery service. There is no time
 * to fetching.
 *
 * Configuration also add listeners to call other beans waiting for fetch new registry. It speed up distribution of
 * changes in whole gateway.
 */
@Configuration
public class DiscoveryClientConfig {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private AbstractDiscoveryClientOptionalArgs<?> optionalArgs;

    @Autowired
    private List<RefreshableRouteLocator> refreshableRouteLocators;

    @Autowired
    private ZuulHandlerMapping zuulHandlerMapping;

    @Bean(destroyMethod = "shutdown")
    @RefreshScope
    public ApimlDiscoveryClient eurekaClient(ApplicationInfoManager manager,
                                             EurekaClientConfig config,
                                             EurekaInstanceConfig instance,
                                             @Autowired(required = false) HealthCheckHandler healthCheckHandler
    ) {
        ApplicationInfoManager appManager;
        if (AopUtils.isAopProxy(manager)) {
            appManager = ProxyUtils.getTargetObject(manager);
        }
        else {
            appManager = manager;
        }
        final ApimlDiscoveryClient discoveryClientClient = new ApimlDiscoveryClient(appManager, config, this.optionalArgs, this.context);
        discoveryClientClient.registerHealthCheck(healthCheckHandler);

        discoveryClientClient.registerEventListener(event -> {
            if (event instanceof CacheRefreshedEvent) {
                refreshableRouteLocators.forEach(x -> x.refresh());
                zuulHandlerMapping.setDirty(true);
            }
        });
        return discoveryClientClient;
    }
}
