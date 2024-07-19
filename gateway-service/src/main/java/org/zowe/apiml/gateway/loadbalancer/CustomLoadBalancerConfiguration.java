/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.loadbalancer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.zowe.apiml.gateway.caching.LoadBalancerCache;

/**
 * Configuration class for setting up the DeterministicRoutingListSupplierBuilder and StickySessionRoutingListSupplierBuilder
 * based on Gateway configuration.
 */
public class CustomLoadBalancerConfiguration {

    /**
     * Creates a ServiceInstanceListSupplier bean configured with deterministic routing.
     *
     * @param context the application context
     * @return the configured ServiceInstanceListSupplier
     */
    @ConditionalOnProperty(name = "apiml.routing.instanceIdHeader", havingValue = "true")
    @Bean
    public ServiceInstanceListSupplier discoveryClientServiceInstanceListSupplier(ConfigurableApplicationContext context) {
        return new DeterministicRoutingListSupplierBuilder(ServiceInstanceListSupplier.builder()
            .withDiscoveryClient())
            .withDeterministicRouting()
            .build(context);
    }

    /**
     * Creates a ServiceInstanceListSupplier bean configured with sticky session routing.
     *
     * @param context the application context
     * @return the configured ServiceInstanceListSupplier
     */
    @Bean
    public ServiceInstanceListSupplier discoveryClientCachedServiceInstanceListSupplier(
        ConfigurableApplicationContext context, LoadBalancerCache cache,
        @Value("${instance.metadata.apiml.lb.cacheRecordExpirationTimeInHours:8}") int expirationTime) {
        return new StickySessionRoutingListSupplierBuilder(ServiceInstanceListSupplier.builder()
            .withDiscoveryClient())
            .withStickySessionRouting(cache, expirationTime)
            .build(context);
    }
}
