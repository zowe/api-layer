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

import org.springframework.stereotype.Component;

@Component
public class GatewayClient {

    private GatewayConfigProperties gatewayConfigProperties;

    public GatewayClient() {
    }

    public GatewayClient(GatewayConfigProperties gatewayConfigProperties) {
        this.gatewayConfigProperties = gatewayConfigProperties;
    }

    public void setGatewayConfigProperties(GatewayConfigProperties gatewayConfigProperties) {
        this.gatewayConfigProperties = gatewayConfigProperties;
    }

    public GatewayConfigProperties getGatewayConfigProperties() {
        if (gatewayConfigProperties == null) {
            throw new GatewayNotFoundException("No Gateway Instance is known at the moment");
        }

        return gatewayConfigProperties;
    }

    public boolean isInitialized() {
        return gatewayConfigProperties != null;
    }
}
