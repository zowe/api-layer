/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.client.configuration;

import com.ca.mfaas.enable.EnableApiDiscovery;
import com.ca.mfaas.error.ErrorService;
import com.ca.mfaas.error.impl.ErrorServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(value = { "com.ca.mfaas.client", "com.ca.mfaas.enable", "com.ca.mfaas.product.web" })
@EnableApiDiscovery
public class ApplicationConfiguration {
    @Bean
    public ErrorService errorService() {
        return new ErrorServiceImpl("/messages.yml");
    }
}
