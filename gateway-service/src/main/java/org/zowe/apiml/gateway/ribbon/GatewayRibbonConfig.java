/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.ribbon;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.*;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;
import lombok.RequiredArgsConstructor;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.netflix.ribbon.*;
import org.springframework.cloud.netflix.ribbon.apache.RibbonLoadBalancingHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.zowe.apiml.gateway.metadata.service.LoadBalancerRegistry;
import org.zowe.apiml.gateway.ribbon.loadBalancer.LoadBalancerRuleAdapter;
import org.zowe.apiml.gateway.ribbon.loadBalancer.PredicateFactory;

/**
 * Configuration of client side load balancing with Ribbon
 */
@Configuration
@RequiredArgsConstructor
public class GatewayRibbonConfig {
    private final PropertiesFactory propertiesFactory;

    @RibbonClientName
    private String ribbonClientName = "client";

    @Bean
    public ApimlRibbonRetryFactory apimlRibbonRetryFactory(SpringClientFactory springClientFactory) {
        AbortingRetryListener retryListener = new AbortingRetryListener();
        return new ApimlRibbonRetryFactory(springClientFactory, retryListener);
    }

    @Bean
    @Primary
    @Autowired
    public RibbonLoadBalancingHttpClient ribbonLoadBalancingHttpClient(
        @Qualifier("httpClientProxy") CloseableHttpClient httpClientProxy,
        IClientConfig config,
        ServerIntrospector serverIntrospector,
        ApimlRibbonRetryFactory retryFactory,
        RibbonLoadBalancerContext ribbonContext
    ) {
        ApimlRetryableClient client = new ApimlRetryableClient(
            httpClientProxy, config, serverIntrospector, retryFactory);
        client.setRibbonLoadBalancerContext(ribbonContext);
        return client;
    }

    @Bean
    @Primary
    @Autowired
    public ILoadBalancer ribbonLoadBalancer(IClientConfig config,
                                            ServerList<Server> serverList, ServerListFilter<Server> serverListFilter,
                                            IPing ping, ServerListUpdater serverListUpdater,
                                            LoadBalancerRegistry loadBalancerRegistry, PredicateFactory predicateFactory) {
        if (this.propertiesFactory.isSet(ILoadBalancer.class, ribbonClientName)) {
            return this.propertiesFactory.get(ILoadBalancer.class, config, ribbonClientName);
        }

        //TODO registry has instanceInfo

        //TODO build balancing rule with context here
        Server server = serverList.getInitialListOfServers().get(0);
        IRule rule;
        if (server instanceof DiscoveryEnabledServer) {
            InstanceInfo instanceInfo = ((DiscoveryEnabledServer) server).getInstanceInfo();
            rule = new LoadBalancerRuleAdapter(instanceInfo, predicateFactory);
        } else {
            throw new IllegalStateException("Server is not an instance of DiscoveryEnabledServer and is not possible to provide Load Balancing");
        }
        return new ApimlLoadBalancer<>(config, rule, ping, serverList,
            serverListFilter, serverListUpdater, loadBalancerRegistry);
    }

    @Bean
    public PredicateFactory predicateFactory() {
        return new PredicateFactory(RibbonClientConfiguration.class, "ribbon", "ribbon.client.name");
    }


    // adapter will know about rules, from where? Default or Instance config


    // Predicate has to know about:
    // InstanceInfo, Request, SecurityContextHolder


}
