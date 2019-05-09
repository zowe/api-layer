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

import com.ca.apiml.security.config.SecurityConfigurationProperties;
import com.ca.mfaas.product.constants.CoreService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

/**
 * Gateway health information (/application/health)
 */
@Component
public class ApimlHealthIndicator extends AbstractHealthIndicator {
    private final DiscoveryClient discoveryClient;
    private final SecurityConfigurationProperties securityConfigurationProperties;

    @Autowired
    public ApimlHealthIndicator(DiscoveryClient discoveryClient,
                                SecurityConfigurationProperties securityConfigurationProperties) {
        this.discoveryClient = discoveryClient;
        this.securityConfigurationProperties = securityConfigurationProperties;
    }

    private Status toStatus(boolean up) {
        return up ? UP : DOWN;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        int gatewayCount = this.discoveryClient.getInstances(CoreService.GATEWAY.getServiceId()).size();
        boolean apiCatalogUp = !this.discoveryClient.getInstances(CoreService.API_CATALOG.getServiceId()).isEmpty();
        boolean discoveryUp = !this.discoveryClient.getInstances(CoreService.DISCOVERY.getServiceId()).isEmpty();
        boolean authUp = true;
        if (!securityConfigurationProperties.getProvider().equalsIgnoreCase("dummy")) {
            authUp = !this.discoveryClient.getInstances(securityConfigurationProperties.validatedZosmfServiceId()).isEmpty();
        }
        boolean apimlUp = discoveryUp && authUp;
        builder.status(toStatus(apimlUp)).withDetail("apicatalog", toStatus(apiCatalogUp).getCode())
            .withDetail("discovery", toStatus(discoveryUp).getCode())
            .withDetail("auth", toStatus(authUp).getCode())
            .withDetail("gatewayCount", gatewayCount);
    }
}
