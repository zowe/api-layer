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
import com.netflix.discovery.EurekaClient;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.IPing;
import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.netflix.loadbalancer.ServerListFilter;
import com.netflix.loadbalancer.ServerListUpdater;
import com.netflix.niws.loadbalancer.DiscoveryEnabledNIWSServerList;
import lombok.RequiredArgsConstructor;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.context.named.NamedContextFactory;
import org.springframework.cloud.netflix.ribbon.PropertiesFactory;
import org.springframework.cloud.netflix.ribbon.RibbonClientName;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerContext;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.netflix.ribbon.apache.RibbonLoadBalancingHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.MapPropertySource;
import org.zowe.apiml.gateway.context.ConfigurableNamedContextFactory;
import org.zowe.apiml.gateway.metadata.service.LoadBalancerRegistry;
import org.zowe.apiml.gateway.ribbon.loadbalancer.InstanceInfoExtractor;
import org.zowe.apiml.gateway.ribbon.loadbalancer.LoadBalancerConstants;
import org.zowe.apiml.gateway.ribbon.loadbalancer.LoadBalancerRuleAdapter;


import java.util.HashMap;
import java.util.Map;

/**
 * Configuration of client side load balancing with Ribbon
 */
@Configuration
@RequiredArgsConstructor
public class GatewayRibbonConfig {
    private final PropertiesFactory propertiesFactory;

    @RibbonClientName
    private String ribbonClientName = "client";

    @Value("${ribbon.eureka.approximateZoneFromHostname:false}")
    private boolean approximateZoneFromHostname = false;

    @Bean
    public ApimlRibbonRetryFactory apimlRibbonRetryFactory(SpringClientFactory springClientFactory) {
        return new ApimlRibbonRetryFactory(springClientFactory);
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

    /**
     * Main place where load balancer is constructed. Here is where we plug in the {@link LoadBalancerRuleAdapter}
     * to act as a rule for selecting servers.
     * <p>
     * The factory for predicates is also initialized here with information fom InstanceInfo
     * <p>
     * Called at first request's invocation and bean is service-scoped (1 bean per serviceId)
     *
     * @param config               Ribbon's client config
     * @param serverList           List of available servers
     * @param serverListFilter     Not sure
     * @param ping                 Server probing mechanism (initialized as server list from Discovery)
     * @param serverListUpdater    Not sure
     * @param loadBalancerRegistry Keeps track of all load balancer beans
     * @param predicateFactory     Factory for creating predicates for server filtering
     * @return Initialized load balancer bean
     */
    @Bean
    @Primary
    @Autowired
    public ILoadBalancer ribbonLoadBalancer(IClientConfig config,
                                            ServerList<Server> serverList, ServerListFilter<Server> serverListFilter,
                                            IPing ping, ServerListUpdater serverListUpdater,
                                            LoadBalancerRegistry loadBalancerRegistry, ConfigurableNamedContextFactory<NamedContextFactory.Specification> predicateFactory) {
        if (this.propertiesFactory.isSet(ILoadBalancer.class, ribbonClientName)) {
            return this.propertiesFactory.get(ILoadBalancer.class, config, ribbonClientName);
        }

        InstanceInfoExtractor infoExtractor = new InstanceInfoExtractor(serverList.getInitialListOfServers());

        InstanceInfo randomInstanceInfo = infoExtractor.getInstanceInfo().orElseThrow(() -> new IllegalStateException("Not able to retrieve InstanceInfo from server list, Load balancing is not available"));

        Map<String, Object> metadataMap = new HashMap<>();
        randomInstanceInfo.getMetadata().forEach(
            (key, value) -> metadataMap.put(LoadBalancerConstants.getMetadataPrefix() + key, value)
        );

        predicateFactory.addInitializer(randomInstanceInfo.getAppName(), context ->
            context.getEnvironment().getPropertySources()
                .addFirst(new MapPropertySource("InstanceInfoMetadata", metadataMap))
        );

        IRule rule = new LoadBalancerRuleAdapter(
            randomInstanceInfo,
            predicateFactory, config);

        return new ApimlLoadBalancer<>(config, rule, ping, serverList,
            serverListFilter, serverListUpdater, loadBalancerRegistry);
    }

//    @Bean
//    @ConditionalOnMissingBean
//    public ServerList<?> ribbonServerList(IClientConfig config,
//                                          Provider<EurekaClient> eurekaClientProvider) {
//        if (this.propertiesFactory.isSet(ServerList.class, ribbonClientName)) {
//            return this.propertiesFactory.get(ServerList.class, config, ribbonClientName);
//        }
//        DiscoveryEnabledNIWSServerList discoveryServerList = new DiscoveryEnabledNIWSServerList(
//                config, eurekaClientProvider);
//        DomainExtractingServerList serverList = new DomainExtractingServerList(
//                discoveryServerList, config, this.approximateZoneFromHostname);
//        return serverList;
//    }

}
