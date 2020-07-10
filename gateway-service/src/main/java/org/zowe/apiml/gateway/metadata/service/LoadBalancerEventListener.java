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
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Order(10)
public class LoadBalancerEventListener extends RefreshEventListener {

    private Map<String, DynamicServerListLoadBalancer> loadBalancerRegistry = new ConcurrentHashMap<>();

    public void registerLoadBalancer(DynamicServerListLoadBalancer loadBalancer) {
        String loadBalancerName = loadBalancer.getName();
        loadBalancerRegistry.put(loadBalancerName, loadBalancer);
    }

    @Override
    public void refresh() {
        loadBalancerRegistry.values().forEach(DynamicServerListLoadBalancer::updateListOfServers);
    }

}
