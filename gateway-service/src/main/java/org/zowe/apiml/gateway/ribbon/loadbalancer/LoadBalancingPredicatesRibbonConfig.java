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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zowe.apiml.gateway.cache.LoadBalancerCache;
import org.zowe.apiml.gateway.ribbon.loadbalancer.predicate.AuthenticationBasedPredicate;
import org.zowe.apiml.gateway.ribbon.loadbalancer.predicate.RequestHeaderPredicate;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.security.service.HttpAuthenticationService;

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
    @Value("${instance.metadata.apiml.lb.cacheRecordExpirationTimeInHours:8}")
    private int expirationTime;

    @Bean
    @ConditionalOnProperty(name = "instance.metadata.apiml.lb.type", havingValue = "headerRequest")
    public RequestAwarePredicate headerPredicate() {
        return new RequestHeaderPredicate();
    }

    @Bean
    @ConditionalOnProperty(name = "instance.metadata.apiml.lb.type", havingValue = "authentication")
    public AuthenticationBasedPredicate authenticationBasedPredicate(AuthenticationService authenticationService, LoadBalancerCache cache) {
        return new AuthenticationBasedPredicate(
            new HttpAuthenticationService(authenticationService),
            cache,
            expirationTime
        );
    }
}
