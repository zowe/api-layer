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

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplierBuilder;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;

@RequiredArgsConstructor
public class DeterministicRoutingListSupplierBuilder {
    private final ServiceInstanceListSupplierBuilder builder;

    public ServiceInstanceListSupplierBuilder withDeterministicRouting() {
        ServiceInstanceListSupplierBuilder.DelegateCreator creator = (context, delegate) -> {
            LoadBalancerClientFactory loadBalancerClientFactory = context.getBean(LoadBalancerClientFactory.class);
            System.out.println("Creating Deterministic Load Balancer");
            return new DeterministicLoadBalancer(delegate, loadBalancerClientFactory);
        };
        builder.with(creator);
        return builder;
    }
}
