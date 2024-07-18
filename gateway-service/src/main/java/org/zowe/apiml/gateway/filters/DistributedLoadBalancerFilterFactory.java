/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import org.zowe.apiml.gateway.caching.LoadBalancerCache;
import org.zowe.apiml.gateway.caching.LoadBalancerCache.LoadBalancerCacheRecord;
import org.zowe.apiml.gateway.loadbalancer.StickySessionLoadBalancer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * A custom gateway filter factory that integrates with Eureka and uses a load balancer cache to manage sticky sessions.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DistributedLoadBalancerFilterFactory extends AbstractGatewayFilterFactory<DistributedLoadBalancerFilterFactory.Config> {
    private final LoadBalancerCache cache;
    private final EurekaClient eurekaClient;

    /**
     * Store the user and service information into the cache,
     * in case the service metadata load balancer type is set to "authentication".
     *
     * @param config the filter configuration
     * @return the gateway filter
     */
    @Override
    public GatewayFilter apply(DistributedLoadBalancerFilterFactory.Config config) {
        return (exchange, chain) -> Mono.fromCallable(() -> eurekaClient.getInstancesById(config.serviceId))
            .flatMap(instance -> {
                if (!(instance instanceof InstanceInfo instanceInfo)) {
                    log.error("Instance not found or not of type InstanceInfo for service: {}", config.serviceId);
                    return Mono.error(new IllegalStateException("Instance not found or not of type InstanceInfo"));
                }

                if (!lbTypeIsAuthentication(instanceInfo)) {
                    return chain.filter(exchange);
                }

                return StickySessionLoadBalancer.getPrincipal().flatMapMany(user -> {
                    if (user.isEmpty()) {
                        log.debug("No authentication present on request, not filtering the service: {}", config.getServiceId());
                        return Flux.empty();
                    } else {
                        LoadBalancerCacheRecord loadBalancerCacheRecord = new LoadBalancerCacheRecord(config.getInstanceId());
                        return cache.store(user, config.getServiceId(), loadBalancerCacheRecord);
                    }
                }).then();
            });
    }

    /**
     * Checks if the load balancer type for the instance defined in the metadata is "authentication".
     *
     * @param selectedInstance the selected instance
     * @return true if the load balancer type is "authentication", false otherwise
     */
    private boolean lbTypeIsAuthentication(InstanceInfo selectedInstance) {
        Map<String, String> metadata = selectedInstance.getMetadata();
        if (metadata != null) {
            String lbType = metadata.get("apiml.lb.type");
            return lbType != null && lbType.equals("authentication");
        }
        return false;
    }

    @Data
    public static class Config {

        private String serviceId;
        private String instanceId;
    }
}
