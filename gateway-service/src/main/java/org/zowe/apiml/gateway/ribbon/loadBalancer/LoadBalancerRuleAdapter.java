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
import com.netflix.loadbalancer.AbstractServerPredicate;
import com.netflix.loadbalancer.PredicateBasedRule;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LoadBalancerRuleAdapter extends PredicateBasedRule {

    private final InstanceInfo info;


    // default
    //TODO named context

    // custom

    @Override
    public AbstractServerPredicate getPredicate() {
        return null;
    }

}
