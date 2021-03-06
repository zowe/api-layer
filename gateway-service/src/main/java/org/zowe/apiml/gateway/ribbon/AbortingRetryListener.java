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
import org.springframework.retry.*;
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

    @Override
    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        // Exception from retry logic
        if (throwable instanceof RequestAbortException) {
            context.setExhaustedOnly();
        }
        // Exception from load balancer having no servers
        if (throwable instanceof ClientException && StringUtils.startsWith(throwable.getMessage(), "Load balancer does not have available server for client")) {
            context.setExhaustedOnly();
        }
    }
}
