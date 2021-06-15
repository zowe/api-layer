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

/**
 * This class configures the load balancer's composition in terms of what predicates will be
 * active and when.
 *
 * The predicates are constructed in per-serviceId Named Context where the service's metadata are
 * available in the environment, all metadata keys are prefixed with `instance.metadata.`.
 *
 * Class names ending with `RibbonConfig` are excluded from Gateway's component scan to prevent beans
 * being created in main app's context.
 */
@Configuration
public class LoadBalancingPredicatesRibbonConfig {

    @Bean
    @ConditionalOnProperty(name = "instance.metadata.apiml.lb.instanceIdHeader", havingValue = "enabled")
    public RequestAwarePredicate headerPredicate() {
        return new RequestHeaderPredicate();
    }
}
