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

import com.netflix.loadbalancer.DynamicServerListLoadBalancer;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@DependsOn({"zuulRefreshRoutesListener"})
public class LoadBalancerEventListener implements ApplicationListener<ApplicationEvent> {

    private Map<String, DynamicServerListLoadBalancer> loadBalancerRegistry = new ConcurrentHashMap<>();

    public void registerLoadBalancer(DynamicServerListLoadBalancer loadBalancer) {
        String loadBalancerName = loadBalancer.getName();
        loadBalancerRegistry.put(loadBalancerName, loadBalancer);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof HeartbeatEvent) {
            loadBalancerRegistry.values().forEach(DynamicServerListLoadBalancer::updateListOfServers);
        }
    }

}
