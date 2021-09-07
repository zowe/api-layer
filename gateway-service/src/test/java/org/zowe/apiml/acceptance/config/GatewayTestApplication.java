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

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.hystrix.HystrixAutoConfiguration;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.zowe.apiml.acceptance.config.ribbon.RibbonTestConfiguration;
import org.zowe.apiml.gateway.config.DiscoveryClientConfig;
import org.zowe.apiml.gateway.routing.ApimlRoutingConfig;

@EnableZuulProxy
@EnableWebSecurity
@SpringBootApplication(exclude = HystrixAutoConfiguration.class)
@ComponentScan(
    value = {
        "org.zowe.apiml.gateway",
        "org.zowe.apiml.product",
        "org.zowe.apiml.security.common"},
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*RibbonConfig"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*GatewayApplication"),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = DiscoveryClientConfig.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ApimlRoutingConfig.class)
    }
)
@RibbonClients(defaultConfiguration = RibbonTestConfiguration.class)
@EnableEurekaClient
@EnableWebSocket
@EnableAspectJAutoProxy
public class GatewayTestApplication {
}
