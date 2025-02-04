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
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@EnableEurekaServer
@SpringBootApplication(
    scanBasePackages = {
        "org.zowe.apiml",

        "org.zowe.apiml.security.common",
        "org.zowe.apiml.gateway.security.login",

        "com.netflix.eureka",
        "org.springframework"
    }
)
@ComponentScan(
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = ".*Application"
    )
)
public class ApimlApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApimlApplication.class, args);
    }

}
