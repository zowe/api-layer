/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.client;

import org.zowe.apiml.enable.EnableApiDiscovery;
import org.zowe.apiml.product.logging.annotations.EnableApimlLogger;
import org.zowe.apiml.product.monitoring.LatencyUtilsConfigInitializer;
import org.zowe.apiml.product.service.ServiceStartupEventHandler;
import org.zowe.apiml.product.version.BuildInfo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.netflix.hystrix.dashboard.EnableHystrixDashboard;

@SpringBootApplication
@EnableApiDiscovery
@EnableWebSocket
@EnableApimlLogger
@EnableCircuitBreaker
@EnableHystrixDashboard
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
