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

import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.logging.OpenTelemetryLoggingAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.opentelemetry.OpenTelemetryAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.zowe.apiml.product.logging.annotations.EnableApimlLogger;
import org.zowe.apiml.product.monitoring.LatencyUtilsConfigInitializer;
import org.zowe.apiml.product.service.ServiceStartupEventHandler;
import org.zowe.apiml.security.common.config.SafSecurityConfigurationProperties;

@SpringBootApplication(
    exclude = {
        OpenTelemetryAutoConfiguration.class,
        OpenTelemetryLoggingAutoConfiguration.class
    }
)
@ComponentScan({
    "org.zowe.apiml.discovery",
    // MetadataFilterService lives here. The package name is a leftover - the class itself no longer
    // knows anything about Eureka. Renaming the package is a later cleanup.
    "org.zowe.apiml.product.eureka.web",
    "org.zowe.apiml.product.config",
    "org.zowe.apiml.product.security",
    "org.zowe.apiml.product.web",
    "org.zowe.apiml.product.service",
})
@EnableApimlLogger
@EnableWebSecurity
@EnableScheduling
@EnableConfigurationProperties(SafSecurityConfigurationProperties.class)
public class DiscoveryServiceApplication implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private ServiceStartupEventHandler startupEventHandler;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(DiscoveryServiceApplication.class);
        app.addInitializers(new LatencyUtilsConfigInitializer());
        app.setLogStartupInfo(false);
        app.run(args);
    }

    @Override
    public void onApplicationEvent(@Nonnull final ApplicationReadyEvent event) {
        startupEventHandler.onServiceStartup("Discovery Service", ServiceStartupEventHandler.DEFAULT_DELAY_FACTOR);
    }

}
