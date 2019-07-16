/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.gateway;

import com.ca.mfaas.product.gateway.GatewayConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration of the Gateway Lookup Service which obtains Gateway url
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class GatewayConfig {

    private final GatewayLookupService gatewayLookupService;

    @Bean
    public GatewayConfigProperties getGatewayConfigProperties() {
        return gatewayLookupService.getGatewayConfigProperties();
    }
}
