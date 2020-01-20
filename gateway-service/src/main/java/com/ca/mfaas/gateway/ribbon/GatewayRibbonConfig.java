/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.ribbon;

import com.ca.mfaas.gateway.cache.ServiceCacheEvictor;
import com.netflix.client.config.IClientConfig;
import com.netflix.discovery.EurekaClient;
import com.netflix.loadbalancer.*;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.ribbon.PropertiesFactory;
import org.springframework.cloud.netflix.ribbon.RibbonClientName;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.cloud.netflix.ribbon.apache.RibbonLoadBalancingHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRibbonConfig {

    @RibbonClientName
    private String ribbonClientName = "client";

    @Autowired
    private PropertiesFactory propertiesFactory;

    @Bean
    @Autowired
    public RibbonLoadBalancingHttpClient ribbonLoadBalancingHttpClient(
        CloseableHttpClient secureHttpClient,
        IClientConfig config,
        ServerIntrospector serverIntrospector,
        EurekaClient discoveryClient
    ) {
        return new GatewayRibbonLoadBalancingHttpClient(secureHttpClient, config, serverIntrospector, discoveryClient);
    }

    @Bean
    @Autowired
    public ILoadBalancer ribbonLoadBalancer(IClientConfig config,
                                            ServerList<Server> serverList, ServerListFilter<Server> serverListFilter,
                                            IRule rule, IPing ping, ServerListUpdater serverListUpdater,
                                            ServiceCacheEvictor serviceCacheEvictor) {
        if (this.propertiesFactory.isSet(ILoadBalancer.class, ribbonClientName)) {
            return this.propertiesFactory.get(ILoadBalancer.class, config, ribbonClientName);
        }
        return new ApimlZoneAwareLoadBalancer<>(config, rule, ping, serverList,
            serverListFilter, serverListUpdater, serviceCacheEvictor);
    }

}
