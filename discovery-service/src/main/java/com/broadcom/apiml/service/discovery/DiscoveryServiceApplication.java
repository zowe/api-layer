/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.service.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.cloud.netflix.hystrix.HystrixAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@EnableEurekaServer
@SpringBootApplication(exclude = HystrixAutoConfiguration.class)
@EnableConfigurationProperties
@ComponentScan({"com.ca.mfaas.discovery", "com.ca.mfaas.product.config", "com.ca.mfaas.product.discovery", "com.ca.mfaas.product.web"})
public class DiscoveryServiceApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(DiscoveryServiceApplication.class);
        app.run(args);
    }
}
