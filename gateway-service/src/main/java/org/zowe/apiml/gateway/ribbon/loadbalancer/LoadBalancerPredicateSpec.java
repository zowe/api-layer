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

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.context.named.NamedContextFactory;

@RequiredArgsConstructor
public class LoadBalancerPredicateSpec implements NamedContextFactory.Specification {

    private final String name;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<?>[] getConfiguration() {
        return new Class[] {LoadBalancingPredicatesRibbonConfig.class};
    }
}
