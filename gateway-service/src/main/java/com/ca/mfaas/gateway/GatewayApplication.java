/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway;

import com.ca.mfaas.enable.EnableApiDiscovery;
import com.ca.mfaas.gateway.ribbon.GatewayRibbonConfig;
import com.ca.mfaas.product.service.BuildInfo;
import com.ca.mfaas.product.service.ServiceStartupEventHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.hystrix.HystrixAutoConfiguration;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.slf4j.MDC;

@Configuration
@EnableZuulProxy
@EnableWebSecurity
@SpringBootApplication(exclude = HystrixAutoConfiguration.class)
@EnableConfigurationProperties
@ComponentScan(value = {"com.ca.mfaas.gateway", "com.ca.mfaas.product", "com.ca.mfaas.enable"}, excludeFilters = {
    @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*RibbonConfig")})
@RibbonClients(defaultConfiguration = GatewayRibbonConfig.class)
@EnableEurekaClient
@EnableWebSocket
@EnableApiDiscovery
public class GatewayApplication implements ApplicationListener<ApplicationReadyEvent> {
    public static void main(String[] args) {
        MDC.put("userid", System.getProperty("user.name"));
        SpringApplication app = new SpringApplication(GatewayApplication.class);
        app.setLogStartupInfo(false);
        new BuildInfo().logBuildInfo();
        app.run(args);
    }

    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        new ServiceStartupEventHandler().onServiceStartup("Gateway Service",
            ServiceStartupEventHandler.DEFAULT_DELAY_FACTOR);
    }
}
