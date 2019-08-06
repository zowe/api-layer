/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.apiml.security.config;

import com.ca.mfaas.error.ErrorService;
import com.ca.mfaas.error.impl.ErrorServiceImpl;
import com.ca.mfaas.product.gateway.GatewayLookupService;
import com.netflix.discovery.EurekaClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * General configuration of security client
 */
@Configuration
public class SecurityServiceConfiguration {

    @Bean
    public GatewayLookupService gatewayLookupService(
        @Qualifier("eurekaClient") EurekaClient eurekaClient) {
        return new GatewayLookupService(eurekaClient);
    }

    @Bean
    public ErrorService errorService() {
        return new ErrorServiceImpl("/security-service-messages.yml");
    }
}
