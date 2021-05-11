/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.acceptance.config;

import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.netflix.zuul.filters.SimpleRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.discovery.ServiceRouteMapper;
import org.springframework.cloud.netflix.zuul.filters.discovery.SimpleServiceRouteMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.zowe.apiml.acceptance.netflix.ApplicationRegistry;

import java.util.HashMap;

@TestConfiguration
public class GatewayOverrideConfig {
    @Bean
    @Primary
    public ServiceRouteMapper serviceRouteMapper() {
        return new SimpleServiceRouteMapper();
    }

    @MockBean
    @Qualifier("mockProxy")
    public CloseableHttpClient mockProxy;

    @Bean
    public SimpleRouteLocator simpleRouteLocator() {
        ZuulProperties properties = new ZuulProperties();
        properties.setRoutes(new HashMap<>());

        return new SimpleRouteLocator("", properties);
    }

    @Bean
    public ApplicationRegistry registry() {
        return new ApplicationRegistry();
    }
}
