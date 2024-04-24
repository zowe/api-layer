/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.client.reactive.ReactiveOAuth2ClientAutoConfiguration;
import org.springframework.cloud.gateway.config.GatewayReactiveOAuth2AutoConfiguration;

@SpringBootApplication(exclude = {ReactiveOAuth2ClientAutoConfiguration.class, GatewayReactiveOAuth2AutoConfiguration.class})
public class CloudGatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudGatewayServiceApplication.class, args);
    }

}
