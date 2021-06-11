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

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zowe.apiml.gateway.ribbon.loadbalancer.predicate.RequestHeaderPredicate;

@Configuration
public class LoadBalancingPredicatesRibbonConfig {

    @Bean
    @ConditionalOnProperty(name = "instance.metadata.apiml.lb.header", havingValue = "enabled")
    public RequestAwarePredicate headerPredicate() {
        return new RequestHeaderPredicate();
    }
}
