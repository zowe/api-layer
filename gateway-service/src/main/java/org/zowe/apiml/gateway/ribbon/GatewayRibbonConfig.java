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

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.*;
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
import org.zowe.apiml.gateway.ribbon.loadBalancer.InstanceInfoExtractor;
import org.zowe.apiml.gateway.ribbon.loadBalancer.LoadBalancerRuleAdapter;
import org.zowe.apiml.gateway.ribbon.loadBalancer.PredicateFactory;
import org.zowe.apiml.gateway.ribbon.loadBalancer.RequestAwarePredicate;
import org.zowe.apiml.gateway.ribbon.loadBalancer.predicate.StickyPredicate;

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

        InstanceInfoExtractor infoExtractor = new InstanceInfoExtractor(serverList.getInitialListOfServers());

        IRule rule = new LoadBalancerRuleAdapter(
            infoExtractor.getInstanceInfo().orElseThrow(() -> new IllegalStateException("Not able to retrieve InstanceInfo from server list, Load balancing is not available")),
            predicateFactory, config);

        return new ApimlLoadBalancer<>(config, rule, ping, serverList,
            serverListFilter, serverListUpdater, loadBalancerRegistry);
    }

    @Bean
    public PredicateFactory predicateFactory() {
        return new PredicateFactory(RibbonClientConfiguration.class, "ribbon", "ribbon.client.name");
    }

    @Bean
    public RequestAwarePredicate stickyPredicate() {
        return new StickyPredicate();
    }

}
