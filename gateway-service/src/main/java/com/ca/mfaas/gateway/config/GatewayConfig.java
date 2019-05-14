/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.config;

import com.ca.mfaas.product.gateway.GatewayConfigProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This is the configuration class for gateway
 */
@Configuration
public class GatewayConfig {

    /**
     * Create gateway config Bean
     *
     * @param hostname gateway hostname
     * @param port     gateway port
     * @param scheme   gateway scheme
     * @return
     */
    @Bean
    public GatewayConfigProperties getGatewayConfigProperties(@Value("${apiml.gateway.hostname}") String hostname,
                                                              @Value("${apiml.service.port}") String port,
                                                              @Value("${apiml.service.scheme}") String scheme) {
        return GatewayConfigProperties.builder()
            .scheme(scheme)
            .hostname(hostname + ":" + port)
            .build();
    }
}
