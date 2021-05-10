/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.metrics;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.zowe.apiml.enable.EnableApiDiscovery;
import org.zowe.apiml.product.logging.annotations.EnableApimlLogger;
import org.zowe.apiml.product.service.ServiceStartupEventHandler;

import javax.annotation.Nonnull;

@SpringBootApplication
@EnableApiDiscovery
@EnableApimlLogger
@Slf4j
public class MetricsServiceApplication implements ApplicationListener<ApplicationReadyEvent> {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(MetricsServiceApplication.class);
        app.setLogStartupInfo(false);
        try {
            app.run(args);
        } catch (Exception ex) {
            log.info("Error: " + ex.getMessage());
        }
    }

    @Override
    public void onApplicationEvent(@Nonnull final ApplicationReadyEvent event) {
        new ServiceStartupEventHandler().onServiceStartup("Metrics Service",
            ServiceStartupEventHandler.DEFAULT_DELAY_FACTOR);
    }
}
