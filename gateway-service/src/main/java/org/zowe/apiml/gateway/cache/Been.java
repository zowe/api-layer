/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.cache;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

//TODO give this a name
@Configuration
public class Been {

    //TODO find the gateway
    @Bean
    public CachingServiceClient cachingServiceClient(@Qualifier("restTemplateWithKeystore") RestTemplate restTemplate) {
        return new CachingServiceClient(restTemplate, "https://localhost:10010");
    }

    //TODO loadBalancerCache is depending on configs from HttpConfig because it's using restTemplateWithKeystore.
    @Bean
    public LoadBalancerCache loadBalancerCache(CachingServiceClient cachingServiceClient) {
        return new LoadBalancerCache(cachingServiceClient);
    }
}
