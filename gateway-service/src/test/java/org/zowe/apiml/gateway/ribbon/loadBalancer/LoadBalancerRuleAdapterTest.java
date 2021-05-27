/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.ribbon.loadBalancer;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.zuul.context.RequestContext;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

class LoadBalancerRuleAdapterTest {

    @Test
    void name() {

        LoadBalancerRuleAdapter underTest = new LoadBalancerRuleAdapter(mock(InstanceInfo.class));

        RequestContext ctx = mock(RequestContext.class);

        underTest.choose("key");

    }

    private static class testPredicate implements RequestAwarePredicate {

        @Override
        public boolean apply(LoadBalancingContext context) {
            return false;
        }
    }
}
