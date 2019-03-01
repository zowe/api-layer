/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.sample.enable.v1;

import com.ca.mfaas.enable.EnableApiDiscovery;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"com.ca.mfaas.sample.enable", "com.ca.mfaas.enable"})
@EnableApiDiscovery
@EnableConfigurationProperties
public class EnablerV1SampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnablerV1SampleApplication.class, args);
    }

}
