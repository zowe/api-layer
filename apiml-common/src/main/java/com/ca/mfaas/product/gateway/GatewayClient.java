/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.product.gateway;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Container object for {@link GatewayConfigProperties}
 */
@Component
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("squid:S3077")
public class GatewayClient {

    private volatile GatewayConfigProperties gatewayConfigProperties;

    public void setGatewayConfigProperties(GatewayConfigProperties gatewayConfigProperties) {
        this.gatewayConfigProperties = gatewayConfigProperties;
    }

    /**
     * Retrieves GatewayConfigProperties object, which holds the Gateway url and schema
     * When GatewayConfigProperties is unknown (not discovered yet), GatewayNotFoundException is thrown
     *
     * @return GatewayConfigProperties object
     */
    public GatewayConfigProperties getGatewayConfigProperties() {
        if (gatewayConfigProperties == null) {
            throw new GatewayNotFoundException("No Gateway Instance is known at the moment");
        }

        return gatewayConfigProperties;
    }

    /**
     * Check whether the GatewayClient contains GatewayConfigProperties instance
     *
     * @return true when GatewayConfigProperties are initialized
     */
    public boolean isInitialized() {
        return gatewayConfigProperties != null;
    }
}
