/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.context.ApplicationListener;
import org.springframework.retry.annotation.EnableRetry;
import org.zowe.apiml.enable.EnableApiDiscovery;
import org.zowe.apiml.product.logging.annotations.EnableApimlLogger;
import org.zowe.apiml.product.service.ServiceStartupEventHandler;

import javax.annotation.Nonnull;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

@SpringBootApplication
@EnableCircuitBreaker
@EnableApiDiscovery
@EnableRetry
@EnableApimlLogger
public class CachingService extends SpringBootServletInitializer implements ApplicationListener<ApplicationReadyEvent> {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(CachingService.class);
        app.setLogStartupInfo(false);

        app.run(args);
    }

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        Config config = ConfigProvider.getConfig();
        servletContext.setInitParameter("spring.config.additional-location", config.getValue("configYmlLocation", String.class));
        super.onStartup(servletContext);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(CachingService.class);
    }

    @Override
    public void onApplicationEvent(@Nonnull final ApplicationReadyEvent event) {
        new ServiceStartupEventHandler().onServiceStartup("Caching Service",
            ServiceStartupEventHandler.DEFAULT_DELAY_FACTOR);
    }
}
