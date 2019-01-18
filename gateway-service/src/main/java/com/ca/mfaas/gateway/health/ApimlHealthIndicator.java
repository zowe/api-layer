/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.health;

import static org.springframework.boot.actuate.health.Status.DOWN;
import static org.springframework.boot.actuate.health.Status.UP;

import com.ca.mfaas.product.constants.CoreService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

@Component
public class ApimlHealthIndicator extends AbstractHealthIndicator {
    private final DiscoveryClient discoveryClient;

    @Autowired
    public ApimlHealthIndicator(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    private Status toStatus(boolean up) {
        return up ? UP : DOWN;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        boolean apiCatalogUp = this.discoveryClient.getInstances(CoreService.API_CATALOG.getServiceId()).size() > 0;
        boolean discoveryUp = this.discoveryClient.getInstances(CoreService.DISCOVERY.getServiceId()).size() > 0;
        boolean authUp = this.discoveryClient.getInstances(CoreService.API_CATALOG.getServiceId()).size() > 0;
        boolean apimlUp = discoveryUp && authUp;
        builder.status(toStatus(apimlUp)).withDetail("apicatalog", toStatus(apiCatalogUp).getCode())
                .withDetail("discovery", toStatus(discoveryUp).getCode())
                .withDetail("auth", toStatus(authUp).getCode());
    }
}
