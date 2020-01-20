/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.discovery;

import org.zowe.apiml.product.logging.annotations.EnableApimlLogger;
import org.zowe.apiml.product.monitoring.LatencyUtilsConfigInitializer;
import org.zowe.apiml.product.service.ServiceStartupEventHandler;
import org.zowe.apiml.product.version.BuildInfo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.cloud.netflix.hystrix.HystrixAutoConfiguration;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ComponentScan;

import javax.annotation.Nonnull;

@EnableEurekaServer
@SpringBootApplication(exclude = HystrixAutoConfiguration.class)
@ComponentScan({
    "org.zowe.apiml.discovery",
    "org.zowe.apiml.product.security",
    "org.zowe.apiml.product.web"
})
@EnableApimlLogger
public class DiscoveryServiceApplication implements ApplicationListener<ApplicationReadyEvent> {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(DiscoveryServiceApplication.class);
        app.addInitializers(new LatencyUtilsConfigInitializer());
        app.setLogStartupInfo(false);
        new BuildInfo().logBuildInfo();
        app.run(args);
    }

    @Override
    public void onApplicationEvent(@Nonnull final ApplicationReadyEvent event) {
        new ServiceStartupEventHandler().onServiceStartup("Discovery Service", ServiceStartupEventHandler.DEFAULT_DELAY_FACTOR);
    }
}
