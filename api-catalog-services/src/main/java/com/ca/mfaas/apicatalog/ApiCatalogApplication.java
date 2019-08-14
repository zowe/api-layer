/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog;

import com.ca.mfaas.enable.EnableApiDiscovery;
import com.ca.mfaas.product.monitoring.LatencyUtilsConfigInitializer;
import com.ca.mfaas.product.service.BuildInfo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.hystrix.HystrixAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.config.EnableWebFlux;

@SpringBootApplication(exclude = HystrixAutoConfiguration.class)
@EnableEurekaClient
@EnableWebFlux
@EnableApiDiscovery
@ComponentScan({
    "com.ca.mfaas.enable",
    "com.ca.mfaas.apicatalog",
    "com.ca.mfaas.product.security",
    "com.ca.mfaas.product.web",
    "com.ca.mfaas.product.gateway"
})
@EnableScheduling
@EnableRetry
@EnableAsync
public class ApiCatalogApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ApiCatalogApplication.class);
        app.addInitializers(new LatencyUtilsConfigInitializer());
        app.setLogStartupInfo(false);
        new BuildInfo().logBuildInfo();
        app.run(args);
    }
}
