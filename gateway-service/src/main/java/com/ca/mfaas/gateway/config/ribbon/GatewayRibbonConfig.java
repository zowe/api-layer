/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.config.ribbon;

import com.netflix.client.config.IClientConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.cloud.netflix.ribbon.apache.RibbonLoadBalancingHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRibbonConfig {

    @Autowired
    private CloseableHttpClient secureHttpClient;

    @Bean
    @Autowired
    public RibbonLoadBalancingHttpClient ribbonLoadBalancingHttpClient(
            CloseableHttpClient secureHttpClient,
            IClientConfig config,
            ServerIntrospector serverIntrospector) {
        return new GatewayRibbonLoadBalancingHttpClient(secureHttpClient, config, serverIntrospector);
    }
}
