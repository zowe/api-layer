/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.gateway;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.zowe.apiml.product.instance.ServiceAddress;

/**
 * Container object for {@link ServiceAddress}
 */
@Component
@SuppressWarnings("squid:S3077")
public class GatewayClient {

    @Setter
    private volatile ServiceAddress gatewayConfigProperties;

    public GatewayClient(@Autowired(required = false) ServiceAddress gatewayConfigProperties) {
        this.gatewayConfigProperties = gatewayConfigProperties;
    }

    /**
     * Retrieves GatewayConfigProperties object, which holds the Gateway url and schema
     * When GatewayConfigProperties is not available (or not discovered yet), GatewayNotAvailableException is thrown
     *
     * @return GatewayConfigProperties object
     */
    public ServiceAddress getGatewayConfigProperties() {
        if (gatewayConfigProperties == null) {
            throw new GatewayNotAvailableException("No Gateway Instance is available at the moment");
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
