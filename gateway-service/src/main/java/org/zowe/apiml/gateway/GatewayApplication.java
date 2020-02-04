/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway;

import org.zowe.apiml.enable.EnableApiDiscovery;
import org.zowe.apiml.gateway.ribbon.GatewayRibbonConfig;
import org.zowe.apiml.product.monitoring.LatencyUtilsConfigInitializer;
import org.zowe.apiml.product.service.ServiceStartupEventHandler;
import org.zowe.apiml.product.version.BuildInfo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.hystrix.HystrixAutoConfiguration;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

import javax.annotation.Nonnull;

@EnableZuulProxy
@EnableWebSecurity
@SpringBootApplication(exclude = HystrixAutoConfiguration.class)
@ComponentScan(
    value = {
        "org.zowe.apiml.gateway",
        "org.zowe.apiml.product",
        "org.zowe.apiml.enable",
        "org.zowe.apiml.security.common"},
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*RibbonConfig")
    }
)
@RibbonClients(defaultConfiguration = GatewayRibbonConfig.class)
@EnableEurekaClient
@EnableWebSocket
@EnableApiDiscovery
public class GatewayApplication implements ApplicationListener<ApplicationReadyEvent> {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(GatewayApplication.class);
        app.addInitializers(new LatencyUtilsConfigInitializer());
        app.setLogStartupInfo(false);
        new BuildInfo().logBuildInfo();
        app.run(args);
    }

    @Override
    public void onApplicationEvent(@Nonnull final ApplicationReadyEvent event) {
        new ServiceStartupEventHandler().onServiceStartup("Gateway Service",
            ServiceStartupEventHandler.DEFAULT_DELAY_FACTOR);
    }
}
