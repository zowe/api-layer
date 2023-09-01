/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.config;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.discovery.AbstractDiscoveryClientOptionalArgs;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClient;
import org.springframework.cloud.util.ProxyUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.zowe.apiml.gateway.discovery.ApimlDiscoveryClient;

import java.util.*;

/**
 * This configuration override bean EurekaClient with custom ApimlDiscoveryClient. This bean offer additional method
 * fetchRegistry. User can call this method to asynchronously fetch new data from discovery service. There is no time
 * to fetching.
 * <p>
 * Configuration also add listeners to call other beans waiting for fetch new registry. It speed up distribution of
 * changes in whole gateway.
 */
@Configuration
@RequiredArgsConstructor
public class DiscoveryClientConfig {
    private final ApplicationContext context;
    private final AbstractDiscoveryClientOptionalArgs<?> optionalArgs;

    @Bean
    @RefreshScope
    @Qualifier("primaryApimlEurekaClient")
    public ApimlDiscoveryClient primaryApimlEurekaClient(ApplicationInfoManager manager,
                                             EurekaClientConfig config,
                                             @Autowired(required = false) HealthCheckHandler healthCheckHandler
    ) {
        ApplicationInfoManager appManager;
        if (AopUtils.isAopProxy(manager)) {
            appManager = ProxyUtils.getTargetObject(manager);
        } else {
            appManager = manager;
        }
        final ApimlDiscoveryClient discoveryClientClient = new ApimlDiscoveryClient(appManager, config, this.optionalArgs, this.context);
        discoveryClientClient.registerHealthCheck(healthCheckHandler);

        return discoveryClientClient;
    }

    @Bean(destroyMethod = "shutdown")
    @RefreshScope
    public DiscoveryClientWrapper secondaryApimlEurekaClient(@Qualifier("primaryApimlEurekaClient") ApimlDiscoveryClient client1, ApplicationInfoManager manager,
                                                         EurekaClientConfig config,
                                                         @Autowired(required = false) HealthCheckHandler healthCheckHandler
    ) {
        ApplicationInfoManager appManager;
        if (AopUtils.isAopProxy(manager)) {
            appManager = ProxyUtils.getTargetObject(manager);
        } else {
            appManager = manager;
        }
        Map<String,String> urls = new HashMap<>();
        urls.put("defaultZone","https://localhost:10021/eureka/");
        ((EurekaClientConfigBean)config).setServiceUrl(urls);
        final ApimlDiscoveryClient discoveryClientClient = new ApimlDiscoveryClient(appManager, config, this.optionalArgs, this.context);
        discoveryClientClient.registerHealthCheck(healthCheckHandler);
        return new DiscoveryClientWrapper(Arrays.asList(discoveryClientClient, client1));
    }
    @Bean
    @Qualifier("primaryDiscoveryClient")
    public EurekaDiscoveryClient discoveryClient(@Qualifier("primaryApimlEurekaClient") EurekaClient client,
                                                 EurekaClientConfig clientConfig) {
        return new EurekaDiscoveryClient(client, clientConfig);
    }


}
