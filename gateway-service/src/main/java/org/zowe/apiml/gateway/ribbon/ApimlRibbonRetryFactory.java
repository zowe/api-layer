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

import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancedRetryFactory;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.retry.RetryListener;

/**
 * Allows adding RetryListeners to Ribbon Retry
 */
public class ApimlRibbonRetryFactory extends RibbonLoadBalancedRetryFactory {

    private final RetryListener[] listeners;

    public ApimlRibbonRetryFactory(SpringClientFactory clientFactory, RetryListener... listeners) {
        super(clientFactory);
        this.listeners = listeners;
    }

    @Override
    public RetryListener[] createRetryListeners(String service) {
        return listeners;
    }
}
