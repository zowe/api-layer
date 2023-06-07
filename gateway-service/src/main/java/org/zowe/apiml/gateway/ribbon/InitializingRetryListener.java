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
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryContext;
import org.springframework.cloud.client.loadbalancer.ServiceInstanceChooser;
import org.springframework.cloud.netflix.ribbon.support.ContextAwareRequest;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;

@RequiredArgsConstructor
public class InitializingRetryListener implements RetryListener {

    private final ServiceInstanceChooser serviceInstanceChooser;

    @Override
    public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
        if (context instanceof LoadBalancedRetryContext) {
            LoadBalancedRetryContext loadBalancedRetryContext = (LoadBalancedRetryContext) context;
            String serviceId = null;
            if (loadBalancedRetryContext.getRequest() instanceof ContextAwareRequest) {
                serviceId = ((ContextAwareRequest) loadBalancedRetryContext.getRequest()).getContext().getServiceId();
            }
            loadBalancedRetryContext.setServiceInstance(serviceInstanceChooser.choose(serviceId));
        }
        return true;
    }

    @Override
    public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {

    }

    @Override
    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {

    }
}
