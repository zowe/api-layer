/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.message.yaml.YamlMessageServiceInstance;
import org.zowe.apiml.product.constants.CoreService;

import static org.springframework.boot.actuate.health.Status.DOWN;
import static org.springframework.boot.actuate.health.Status.UP;

/**
 * Gateway health information (/application/health)
 */
@Component
public class HealthIndicator extends AbstractHealthIndicator {
    private final DiscoveryClient discoveryClient;
    private String apiCatalogServiceId;

    private final ApimlLogger apimlLog = ApimlLogger.of(HealthIndicator.class,
            YamlMessageServiceInstance.getInstance());
    boolean startedInformationPublished = false;

    public HealthIndicator(DiscoveryClient discoveryClient,
                           @Value("${apiml.catalog.serviceId:}") String apiCatalogServiceId) {
        this.discoveryClient = discoveryClient;
        this.apiCatalogServiceId = apiCatalogServiceId;
    }


    @Override
    protected void doHealthCheck(Health.Builder builder) {
        boolean anyCatalogIsAvailable = apiCatalogServiceId != null && !apiCatalogServiceId.isEmpty();
        boolean apiCatalogUp = !this.discoveryClient.getInstances(apiCatalogServiceId).isEmpty();

        // When DS goes 'down' after it was already 'up', the new status is not shown. This is probably feature of
        // Eureka client which caches the status of services. When DS is down the cache is not refreshed.
        boolean discoveryUp = !this.discoveryClient.getInstances(CoreService.DISCOVERY.getServiceId()).isEmpty();

        boolean authUp = true;

        int gatewayCount = this.discoveryClient.getInstances(CoreService.GATEWAY.getServiceId()).size();

        builder.status(toStatus(discoveryUp))
            .withDetail(CoreService.DISCOVERY.getServiceId(), toStatus(discoveryUp).getCode())
            .withDetail(CoreService.AUTH.getServiceId(), toStatus(authUp).getCode())
            .withDetail("gatewayCount", gatewayCount);

        if (anyCatalogIsAvailable) {
            builder.withDetail(CoreService.API_CATALOG.getServiceId(), toStatus(apiCatalogUp).getCode());
        }

        if (!startedInformationPublished && discoveryUp && apiCatalogUp && authUp) {
            apimlLog.log("org.zowe.apiml.common.mediationLayerStarted");
            startedInformationPublished = true;
        }
    }

    private Status toStatus(boolean up) {
        return up ? UP : DOWN;
    }
}
