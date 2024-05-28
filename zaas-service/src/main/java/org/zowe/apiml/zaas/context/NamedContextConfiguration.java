/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.context;

import org.springframework.cloud.context.named.NamedContextFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zowe.apiml.zaas.ribbon.loadbalancer.LoadBalancerConstants;
import org.zowe.apiml.zaas.ribbon.loadbalancer.LoadBalancingPredicatesRibbonConfig;

@Configuration
public class NamedContextConfiguration {

    /**
     * Factory for load balancer predicates
     *
     * Takes in {@link LoadBalancingPredicatesRibbonConfig} which specifies the composition of load
     * balancer predicates.
     *
     * This is static now, but in the future can be wired in as a bean to allow broader extensibility
     * There should be only one instance of this factory in main application context
     */
    @Bean
    public ConfigurableNamedContextFactory<NamedContextFactory.Specification> predicateFactory() {
        return new ConfigurableNamedContextFactory<>(LoadBalancingPredicatesRibbonConfig.class, "contextConfiguration",
            LoadBalancerConstants.INSTANCE_KEY + LoadBalancerConstants.SERVICEID_KEY);
    }
}
