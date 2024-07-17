/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.routing;

import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * Configuration class for setting up the DeterministicLoadBalancer.
 */
public class DeterministicLoadBalancerConfiguration {

    /**
     * Creates a ServiceInstanceListSupplier bean configured with deterministic routing.
     *
     * @param context the application context
     * @return the configured ServiceInstanceListSupplier
     */
    @Bean
    public ServiceInstanceListSupplier discoveryClientServiceInstanceListSupplier(ConfigurableApplicationContext context) {
        return new DeterministicRoutingListSupplierBuilder(ServiceInstanceListSupplier.builder()
            .withDiscoveryClient())
            .withDeterministicRouting()
            .build(context);
    }
}
