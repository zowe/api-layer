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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    private final GatewayConfigInitializer gatewayConfigInitializer;

    public GatewayConfig(GatewayConfigInitializer gatewayConfigInitializer) {
        this.gatewayConfigInitializer = gatewayConfigInitializer;
    }


    @Bean
    public GatewayConfigProperties getGatewayConfigProperties() throws GatewayConfigInitializerException {
        return gatewayConfigInitializer.getGatewayConfigProperties();
    }
}
