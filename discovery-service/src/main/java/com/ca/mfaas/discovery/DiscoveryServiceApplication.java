/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.discovery;

import com.ca.mfaas.product.logging.annotations.EnableApimlLogger;
import com.ca.mfaas.product.monitoring.LatencyUtilsConfigInitializer;
import com.ca.mfaas.product.service.ServiceStartupEventHandler;
import com.ca.mfaas.product.version.BuildInfo;
import org.springframework.boot.Banner;
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
    "com.ca.mfaas.discovery",
    "com.ca.mfaas.product.security",
    "com.ca.mfaas.product.web"
})
@EnableApimlLogger
public class DiscoveryServiceApplication implements ApplicationListener<ApplicationReadyEvent> {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(DiscoveryServiceApplication.class);
        app.addInitializers(new LatencyUtilsConfigInitializer());
        app.setLogStartupInfo(false);
        app.setBannerMode(Banner.Mode.OFF);
        new BuildInfo().logBuildInfo();
        app.run(args);
    }

    @Override
    public void onApplicationEvent(@Nonnull final ApplicationReadyEvent event) {
        new ServiceStartupEventHandler().onServiceStartup("Discovery Service", ServiceStartupEventHandler.DEFAULT_DELAY_FACTOR);
    }
}
