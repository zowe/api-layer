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

import com.netflix.loadbalancer.*;

public abstract class RequestAwarePredicate extends AbstractServerPredicate {

    abstract boolean apply(LoadBalancingContext context, Server server);

    @Override
    public boolean apply(PredicateKey input) {
        return false;
    }

    public abstract String toString();
}
