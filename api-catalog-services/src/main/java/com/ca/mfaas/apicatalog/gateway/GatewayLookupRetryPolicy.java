/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.gateway;

import org.springframework.retry.RetryContext;
import org.springframework.retry.policy.AlwaysRetryPolicy;

/**
 * Gateway Lookup retry policy
 * <p>
 * Always retry unless there is unexpected error
 */
public class GatewayLookupRetryPolicy extends AlwaysRetryPolicy {

    @Override
    public boolean canRetry(RetryContext context) {
        if (context.getLastThrowable() instanceof GatewayLookupException) {
            throw (GatewayLookupException) context.getLastThrowable();
        }

        return true;
    }
}
