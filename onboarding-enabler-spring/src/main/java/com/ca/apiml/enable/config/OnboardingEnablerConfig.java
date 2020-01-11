/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package com.ca.apiml.enable.config;

import com.ca.mfaas.eurekaservice.client.config.ApiMediationServiceConfig;
import com.ca.mfaas.eurekaservice.client.config.Ssl;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OnboardingEnablerConfig {

    @ConfigurationProperties(prefix = "apiml.service")
    @Bean
    public ApiMediationServiceConfig apiMediationServiceConfig() {
        return new ApiMediationServiceConfig();
    }

    @ConfigurationProperties(prefix = "server.ssl")
    @Bean
    public Ssl ssl() {
        return new Ssl();
    }
}
