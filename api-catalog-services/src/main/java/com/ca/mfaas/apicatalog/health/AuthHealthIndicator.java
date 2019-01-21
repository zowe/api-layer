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

import static org.springframework.boot.actuate.health.Status.DOWN;
import static org.springframework.boot.actuate.health.Status.UP;

import com.ca.mfaas.security.config.SecurityConfigurationProperties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

@Component
public class AuthHealthIndicator extends AbstractHealthIndicator {
    private final DiscoveryClient discoveryClient;
    private final SecurityConfigurationProperties securityConfigurationProperties;

    @Autowired
    public AuthHealthIndicator(DiscoveryClient discoveryClient,
            SecurityConfigurationProperties securityConfigurationProperties) {
        this.discoveryClient = discoveryClient;
        this.securityConfigurationProperties = securityConfigurationProperties;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        boolean authUp = this.discoveryClient.getInstances(securityConfigurationProperties.validatedZosmfServiceId())
                .size() > 0;
        builder.status(authUp ? UP : DOWN).withDetail("auth", authUp ? UP.getCode() : DOWN.getCode());
    }
}
