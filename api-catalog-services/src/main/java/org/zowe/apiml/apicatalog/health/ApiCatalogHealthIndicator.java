/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.apicatalog.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.zowe.apiml.product.constants.CoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;

import java.util.Map;

/**
 * Api Catalog health information (/application/health)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiCatalogHealthIndicator extends AbstractHealthIndicator {
    private static String AUTHENTICATION_SERVICE_ID = "apiml.authorizationService.zosmfServiceId";
    private static String AUTHENTICATION_SERVICE_PROVIDER = "apiml.authorizationService.provider";

    private final DiscoveryClient discoveryClient;
    private final AuthConfigurationProperties authConfigurationProperties;

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        String gatewayServiceId = CoreService.GATEWAY.getServiceId();
        String authServiceId = this.discoveryClient.getInstances(gatewayServiceId).get(0).getMetadata().get(AUTHENTICATION_SERVICE_ID);
        String authServiceProvider = this.discoveryClient.getInstances(gatewayServiceId).get(0).getMetadata().get(AUTHENTICATION_SERVICE_PROVIDER);
        boolean authServiceUp = false;
        try {
            if (authServiceProvider.equalsIgnoreCase("zosmf")) {
                authServiceUp = !this.discoveryClient.getInstances(authServiceId).isEmpty();
            }
        } catch (AuthenticationServiceException ex) {
            log.debug("The parameter 'zosmfServiceId' is not configured.", ex);
        }

        boolean gatewayUp = !this.discoveryClient.getInstances(gatewayServiceId).isEmpty();
        Status healthStatus = (gatewayUp && authServiceUp) ? Status.UP : Status.DOWN;

        builder
            .status(healthStatus)
            .withDetail(gatewayServiceId, healthStatus.getCode());
    }
}
