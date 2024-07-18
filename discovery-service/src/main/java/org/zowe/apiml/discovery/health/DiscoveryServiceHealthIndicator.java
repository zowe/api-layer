/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.health;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import org.zowe.apiml.product.constants.CoreService;

/**
 * Discovery service health information (/application/health)
 */
@Component
@RequiredArgsConstructor
public class DiscoveryServiceHealthIndicator extends AbstractHealthIndicator {

    private final DiscoveryClient discoveryClient;

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        String zaasServiceId = CoreService.ZAAS.getServiceId();
        boolean gatewayDown = this.discoveryClient.getInstances(zaasServiceId).isEmpty();
        builder
            .status(gatewayDown ? new Status("PARTIAL", "Authenticated endpoints not available.") : Status.UP)
            .withDetail(zaasServiceId, gatewayDown ? Status.DOWN : Status.UP);
    }
}
