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

import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryContext;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicy;
import org.springframework.cloud.client.loadbalancer.ServiceInstanceChooser;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancedRetryFactory;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancedRetryPolicy;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerContext;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.retry.RetryListener;

import java.lang.reflect.InvocationTargetException;
import java.net.ConnectException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Allows adding RetryListeners to Ribbon Retry
 */
public class ApimlRibbonRetryFactory extends RibbonLoadBalancedRetryFactory {

    private final SpringClientFactory clientFactory;
    private final AtomicReference<ServiceInstanceChooser> serviceInstanceChooser = new AtomicReference<>();

    public ApimlRibbonRetryFactory(SpringClientFactory clientFactory) {
        super(clientFactory);
        this.clientFactory = clientFactory;
    }

    @Override
    public LoadBalancedRetryPolicy createRetryPolicy(String service, ServiceInstanceChooser serviceInstanceChooser) {
        this.serviceInstanceChooser.set(serviceInstanceChooser);
        RibbonLoadBalancerContext lbContext = this.clientFactory.getLoadBalancerContext(service);
        return new RibbonLoadBalancedRetryPolicy(service, lbContext, serviceInstanceChooser, clientFactory.getClientConfig(service)) {

            @Override
            public boolean canRetry(LoadBalancedRetryContext context) {
                return super.canRetry(context) || isConnectionRefused(context.getLastThrowable());
            }

            @Override
            public boolean canRetryNextServer(LoadBalancedRetryContext context) {
                return super.canRetryNextServer(context) || context.getRetryCount() == 0;
            }

            private boolean isConnectionRefused(Throwable t) {
                if (t instanceof InvocationTargetException) {
                    return isConnectionRefused(((InvocationTargetException) t).getTargetException());
                }

                return t instanceof ConnectException;
            }

        };
    }

    @Override
    public RetryListener[] createRetryListeners(String service) {
        return new RetryListener[]{
                new InitializingRetryListener(this.serviceInstanceChooser.get()),
                new AbortingRetryListener()
        };
    }

}
