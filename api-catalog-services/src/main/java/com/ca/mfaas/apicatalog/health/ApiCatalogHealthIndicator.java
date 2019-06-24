/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.health;

import com.ca.mfaas.product.constants.CoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

@Component
public class ApiCatalogHealthIndicator extends AbstractHealthIndicator {
    private final DiscoveryClient discoveryClient;

    @Autowired
    public ApiCatalogHealthIndicator(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        String gatewayServiceId = CoreService.GATEWAY.getServiceId();

        boolean gatewayUp = !this.discoveryClient.getInstances(gatewayServiceId).isEmpty();
        Status healthStatus = gatewayUp ? Status.UP : Status.DOWN;

        builder
            .status(healthStatus)
            .withDetail(gatewayServiceId, healthStatus.getCode());
    }
}
