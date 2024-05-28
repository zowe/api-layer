/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.product.gateway.GatewayConfigProperties;

/**
 * Setup for caching service backed load balancing cache.
 */
@Configuration
@RequiredArgsConstructor
public class LoadBalancerCacheBeansConfig {

    private final GatewayConfigProperties gatewayConfigProperties;

    @Bean
    public CachingServiceClient cachingServiceClient(@Qualifier("restTemplateWithKeystore") RestTemplate restTemplate) {
        String gatewayUri = String.format("%s://%s", gatewayConfigProperties.getScheme(), gatewayConfigProperties.getHostname());
        return new CachingServiceClient(restTemplate, gatewayUri);
    }

    @Bean
    @ConditionalOnProperty(name = "apiml.loadBalancer.distribute", havingValue = "true")
    public LoadBalancerCache loadBalancerCacheWithRemoteCache(CachingServiceClient cachingServiceClient) {
        return new LoadBalancerCache(cachingServiceClient);
    }

    @Bean
    @ConditionalOnProperty(name = "apiml.loadBalancer.distribute", havingValue = "false", matchIfMissing = true)
    public LoadBalancerCache loadBalancerCacheOnlyLocalCache() {
        return new LoadBalancerCache(null);
    }
}
