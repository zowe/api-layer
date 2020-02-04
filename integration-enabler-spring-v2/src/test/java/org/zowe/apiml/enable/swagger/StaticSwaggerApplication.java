/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.enable.swagger;

import org.zowe.apiml.enable.EnableApiDiscovery;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableApiDiscovery
@EnableConfigurationProperties
@ComponentScan({"org.zowe.apiml.enable"})
public class StaticSwaggerApplication {
    public static void main(String[] args) {
        SpringApplication.run(StaticSwaggerApplication.class, args);
    }
}
