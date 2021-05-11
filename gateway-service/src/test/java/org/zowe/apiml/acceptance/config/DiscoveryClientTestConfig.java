/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.acceptance.config;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.discovery.AbstractDiscoveryClientOptionalArgs;
import com.netflix.discovery.CacheRefreshedEvent;
import com.netflix.discovery.EurekaClientConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.netflix.zuul.filters.RefreshableRouteLocator;
import org.springframework.cloud.netflix.zuul.web.ZuulHandlerMapping;
import org.springframework.cloud.util.ProxyUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.zowe.apiml.acceptance.netflix.ApimlDiscoveryClientStub;
import org.zowe.apiml.acceptance.netflix.ApplicationRegistry;

import java.util.List;

/**
 * This configuration override bean EurekaClient with custom ApimlDiscoveryClient. This bean offer additional method
 * fetchRegistry. User can call this method to asynchronously fetch new data from discovery service. There is no time
 * to fetching.
 * <p>
 * Configuration also add listeners to call other beans waiting for fetch new registry. It speed up distribution of
 * changes in whole gateway.
 */
@TestConfiguration
@RequiredArgsConstructor
public class DiscoveryClientTestConfig {
    private final ApplicationContext context;
    private final AbstractDiscoveryClientOptionalArgs<?> optionalArgs;
    private final List<RefreshableRouteLocator> refreshableRouteLocators;
    private final ZuulHandlerMapping zuulHandlerMapping;

    @Bean(destroyMethod = "shutdown")
    @RefreshScope
    public ApimlDiscoveryClientStub eurekaClient(ApplicationInfoManager manager,
                                                 EurekaClientConfig config,
                                                 EurekaInstanceConfig instance,
                                                 @Autowired(required = false) HealthCheckHandler healthCheckHandler,
                                                 ApplicationRegistry applicationRegistry
    ) {
        ApplicationInfoManager appManager;
        if (AopUtils.isAopProxy(manager)) {
            appManager = ProxyUtils.getTargetObject(manager);
        } else {
            appManager = manager;
        }

        final ApimlDiscoveryClientStub discoveryClient = new ApimlDiscoveryClientStub(appManager, config, this.optionalArgs, this.context, applicationRegistry);
        discoveryClient.registerHealthCheck(healthCheckHandler);

        discoveryClient.registerEventListener(event -> {
            if (event instanceof CacheRefreshedEvent) {
                refreshableRouteLocators.forEach(RefreshableRouteLocator::refresh);
                zuulHandlerMapping.setDirty(true);
            }
        });
        return discoveryClient;
    }
}
