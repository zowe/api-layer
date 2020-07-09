/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.metadata.service;

import com.netflix.discovery.CacheRefreshedEvent;
import com.netflix.discovery.EurekaEvent;
import com.netflix.discovery.EurekaEventListener;
import com.netflix.loadbalancer.DynamicServerListLoadBalancer;
import lombok.AllArgsConstructor;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.cloud.netflix.zuul.filters.RefreshableRouteLocator;
import org.springframework.cloud.netflix.zuul.web.ZuulHandlerMapping;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@DependsOn({"ribbonMetadataProcessor"})
public class DiscoveryClientEventListener implements ApplicationListener<ApplicationEvent>, EurekaEventListener {

    private Map<String, DynamicServerListLoadBalancer> loadBalancerRegistry = new ConcurrentHashMap<>();

    public void registerLoadBalancer(DynamicServerListLoadBalancer loadBalancer) {
        String loadBalancerName = loadBalancer.getName();
        loadBalancerRegistry.put(loadBalancerName, loadBalancer);
    }

    @Override
    public void onEvent(EurekaEvent event) {
        loadBalancerRegistry.values().forEach(DynamicServerListLoadBalancer::updateListOfServers);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof HeartbeatEvent) {
            HeartbeatEvent e = (HeartbeatEvent) event;
            loadBalancerRegistry.values().forEach(DynamicServerListLoadBalancer::updateListOfServers);
        }
    }

}
