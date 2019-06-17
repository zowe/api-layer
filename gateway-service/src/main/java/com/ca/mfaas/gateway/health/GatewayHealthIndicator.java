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
import com.ca.mfaas.gateway.security.login.LoginProvider;
import com.ca.mfaas.product.constants.CoreService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.stereotype.Component;

/**
 * Gateway health information (/application/health)
 */
@Component
public class GatewayHealthIndicator extends AbstractHealthIndicator {
    private final DiscoveryClient discoveryClient;
    private final SecurityConfigurationProperties securityConfigurationProperties;

    @Autowired
    public GatewayHealthIndicator(DiscoveryClient discoveryClient,
                                  SecurityConfigurationProperties securityConfigurationProperties) {
        this.discoveryClient = discoveryClient;
        this.securityConfigurationProperties = securityConfigurationProperties;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        boolean apiCatalogUp = !this.discoveryClient.getInstances(CoreService.API_CATALOG.getServiceId()).isEmpty();

        // When DS goes 'down' after it was already 'up', the new status is not shown. This is probably feature of
        // Eureka client which caches the status of services. When DS is down the cache is not refreshed.
        boolean discoveryUp = !this.discoveryClient.getInstances(CoreService.DISCOVERY.getServiceId()).isEmpty();

        boolean authUp = true;
        if (!securityConfigurationProperties.getProvider().equalsIgnoreCase(LoginProvider.DUMMY.toString())) {
            try {
                authUp = !this.discoveryClient.getInstances(securityConfigurationProperties.validatedZosmfServiceId()).isEmpty();
            } catch (AuthenticationServiceException ex) {
                System.exit(-1);
            }
        }

        int gatewayCount = this.discoveryClient.getInstances(CoreService.GATEWAY.getServiceId()).size();

        builder.status(toStatus(discoveryUp && authUp))
            .withDetail(CoreService.API_CATALOG.getServiceId(), toStatus(apiCatalogUp).getCode())
            .withDetail(CoreService.DISCOVERY.getServiceId(), toStatus(discoveryUp).getCode())
            .withDetail(CoreService.AUTH.getServiceId(), toStatus(authUp).getCode())
            .withDetail("gatewayCount", gatewayCount);
    }

    private Status toStatus(boolean up) {
        return up ? UP : DOWN;
    }
}
