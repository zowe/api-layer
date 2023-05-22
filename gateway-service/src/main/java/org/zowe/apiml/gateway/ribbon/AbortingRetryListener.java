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

import com.netflix.client.ClientException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryContext;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.zowe.apiml.gateway.ribbon.http.RequestAbortException;

/**
 * Listener that aborts the Ribbon retrying on specific exceptions
 */
public class AbortingRetryListener implements RetryListener {
    @Override
    public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
        return true;
    }

    @Override
    public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        // do nothing
    }

    /**
     * This detects behaviour of new Eureka. It tries to make first attempt without set instance. It is set in the error
     * handeling.
     * @param context Retry context
     * @return true if handler was called after the first attempt without any server
     */
    private boolean isFirstAttemptWithoutServer(RetryContext context) {
        if (context instanceof LoadBalancedRetryContext) {
            return context.getRetryCount() == 1 && ((LoadBalancedRetryContext) context).getServiceInstance() != null;
        }
        return false;
    }

    @Override
    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        // Exception from retry logic
        if (throwable instanceof RequestAbortException) {
            context.setExhaustedOnly();
        }
        // Exception from load balancer having no servers
        if (
            !isFirstAttemptWithoutServer(context) &&
            (throwable instanceof ClientException && StringUtils.startsWith(throwable.getMessage(), "Load balancer does not have available server for client"))
        ) {
            context.setExhaustedOnly();
        }
    }
}
