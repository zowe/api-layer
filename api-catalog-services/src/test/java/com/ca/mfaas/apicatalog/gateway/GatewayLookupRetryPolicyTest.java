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


import com.ca.mfaas.apicatalog.instance.InstanceInitializationException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.retry.RetryContext;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GatewayLookupRetryPolicyTest {

    private GatewayLookupRetryPolicy gatewayLookupRetryPolicy;

    @Before
    public void setup() {
        gatewayLookupRetryPolicy = new GatewayLookupRetryPolicy();
    }

    @Test(expected = GatewayLookupException.class)
    public void whenLastThorawableExceptionIsGatewayLookupException_thenThrowIt() {
        RetryContext retryContext = mock(RetryContext.class);
        when(retryContext.getLastThrowable()).thenReturn(new GatewayLookupException("GatewayLookupException"));

        gatewayLookupRetryPolicy.canRetry(retryContext);
    }


    @Test
    public void whenLastThorawableExceptionIsNotGatewayLookupException_thenReturnAlwaysTrue() {
        RetryContext retryContext = mock(RetryContext.class);
        when(retryContext.getLastThrowable()).thenReturn(new InstanceInitializationException("InstanceInitializationException"));

        assertTrue(gatewayLookupRetryPolicy.canRetry(retryContext));
    }


}
