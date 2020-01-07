/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.client;

import com.ca.mfaas.enable.EnableApiDiscovery;
import com.ca.mfaas.product.logging.annotations.EnableApimlLogger;
import com.ca.mfaas.product.monitoring.LatencyUtilsConfigInitializer;
import com.ca.mfaas.product.service.ServiceStartupEventHandler;
import com.ca.mfaas.product.version.BuildInfo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.hystrix.HystrixAutoConfiguration;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

@SpringBootApplication(exclude = HystrixAutoConfiguration.class)
@EnableApiDiscovery
@EnableConfigurationProperties
@EnableWebSocket
@EnableApimlLogger
@ComponentScan(value = {
    "com.ca.mfaas.client",
    "com.ca.mfaas.enable",
    "com.ca.mfaas.product.web" })
public class DiscoverableClientSampleApplication implements ApplicationListener<ApplicationReadyEvent> {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(DiscoverableClientSampleApplication.class);
        app.addInitializers(new LatencyUtilsConfigInitializer());
        app.setLogStartupInfo(false);
        new BuildInfo().logBuildInfo();
        app.run(args);
    }

    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        new ServiceStartupEventHandler().onServiceStartup("Discoverable Client Service",
                ServiceStartupEventHandler.DEFAULT_DELAY_FACTOR);
    }
}
