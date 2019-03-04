/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.service.security.test.integration.client;

import com.broadcom.apiml.library.service.security.test.integration.enable.EnableApiDiscovery;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.hystrix.HystrixAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

@SpringBootApplication(exclude = HystrixAutoConfiguration.class)
@EnableApiDiscovery
@EnableConfigurationProperties
@EnableWebSocket
@ComponentScan(value = {"com.ca.mfaas", "com.ca.mfaas.enable", "com.ca.mfaas.product.web"})
public class DiscoverableClientSampleApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(DiscoverableClientSampleApplication.class);
        app.run(args);
    }
}
