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

import io.jsonwebtoken.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplierBuilder;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.zowe.apiml.gateway.caching.LoadBalancerCache;

@RequiredArgsConstructor
public class StickySessionRoutingListSupplierBuilder {

    private final ServiceInstanceListSupplierBuilder builder;

    public ServiceInstanceListSupplierBuilder withStickySessionRouting(LoadBalancerCache cache, int expirationTime, Clock clock) {
        ServiceInstanceListSupplierBuilder.DelegateCreator creator = (context, delegate) -> {
            LoadBalancerClientFactory loadBalancerClientFactory = context.getBean(LoadBalancerClientFactory.class);
            return new StickySessionLoadBalancer(delegate, loadBalancerClientFactory, cache, clock, expirationTime);
        };
        builder.with(creator);
        return builder;
    }
}
