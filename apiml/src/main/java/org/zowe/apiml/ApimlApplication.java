/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.client.reactive.ReactiveOAuth2ClientAutoConfiguration;
import org.springframework.cloud.netflix.eureka.server.EurekaController;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.zowe.apiml.enable.EnableApiDiscovery;
import org.zowe.apiml.enable.config.EnableApiDiscoveryConfig;
import org.zowe.apiml.enable.register.RegisterToApiLayer;
import org.zowe.apiml.gateway.config.GatewayHealthIndicator;

@SpringBootApplication(exclude = {ReactiveOAuth2ClientAutoConfiguration.class})
@ComponentScan(
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = ".*Application"
        ),
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = {EnableApiDiscoveryConfig.class, EurekaController.class, RegisterToApiLayer.class, GatewayHealthIndicator.class}
        ),
        @ComponentScan.Filter(
            type = FilterType.ANNOTATION,
            classes = EnableApiDiscovery.class
        )
    },
    basePackages = {
        "org.zowe.apiml.product.web"
    }
)
public class ApimlApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApimlApplication.class, args);
    }
}
