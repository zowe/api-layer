/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.config;

import com.ca.mfaas.product.gateway.GatewayConfigProperties;

import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.route.SimpleHostRoutingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public GatewayConfigProperties getGatewayConfigProperties(@Value("${apiml.gateway.hostname}") String hostname,
            @Value("${apiml.service.port}") String port, @Value("${apiml.service.scheme}") String scheme) {
        return GatewayConfigProperties.builder().scheme(scheme).hostname(hostname + ":" + port).build();
    }

    @Bean
    @Autowired
    public SimpleHostRoutingFilter simpleHostRoutingFilter2(ProxyRequestHelper helper, ZuulProperties zuulProperties,
            @Qualifier("secureHttpClientWithoutKeystore") CloseableHttpClient secureHttpClientWithoutKeystore) {
        return new SimpleHostRoutingFilter(helper, zuulProperties, secureHttpClientWithoutKeystore);
    }
}
