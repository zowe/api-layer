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

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryContext;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicy;
import org.springframework.cloud.client.loadbalancer.ServiceInstanceChooser;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancedRetryFactory;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.retry.RetryListener;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Allows adding RetryListeners to Ribbon Retry
 */
public class ApimlRibbonRetryFactory extends RibbonLoadBalancedRetryFactory {

    private final AtomicReference<ServiceInstanceChooser> serviceInstanceChooser = new AtomicReference<>();

    public ApimlRibbonRetryFactory(SpringClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public LoadBalancedRetryPolicy createRetryPolicy(String service, ServiceInstanceChooser serviceInstanceChooser) {
        this.serviceInstanceChooser.set(serviceInstanceChooser);
        return new LoadBalancedRetryPolicyFix(super.createRetryPolicy(service, serviceInstanceChooser));
    }

    @Override
    public RetryListener[] createRetryListeners(String service) {
        return new RetryListener[] {
            new InitializingRetryListener(this.serviceInstanceChooser.get()),
            new AbortingRetryListener()
        };
    }

    @RequiredArgsConstructor
    private static class LoadBalancedRetryPolicyFix implements LoadBalancedRetryPolicy {

        @Delegate(excludes = CanRetryNextServer.class)
        private final LoadBalancedRetryPolicy original;

        @Override
        public boolean canRetryNextServer(LoadBalancedRetryContext context) {
            return original.canRetryNextServer(context) || context.getRetryCount() == 0;
        }

        interface CanRetryNextServer {
            boolean canRetryNextServer(LoadBalancedRetryContext context);
        }

    }

}
