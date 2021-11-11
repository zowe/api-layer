/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.ribbon.loadbalancer;

import com.netflix.loadbalancer.AbstractServerPredicate;
import com.netflix.loadbalancer.PredicateKey;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;

import javax.annotation.Nullable;

/**
 * Prototype of predicate for server selection. See {@link LoadBalancerRuleAdapter} how the
 * predicates are used.
 */
public abstract class RequestAwarePredicate extends AbstractServerPredicate {

    public abstract boolean apply(LoadBalancingContext context, DiscoveryEnabledServer server);

    @Override
    public boolean apply(@Nullable PredicateKey input) {
        throw new UnsupportedOperationException("This method is not supported");
    }

    public abstract String toString();
}
