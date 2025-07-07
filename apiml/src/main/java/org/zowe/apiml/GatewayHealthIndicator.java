/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.server.event.EurekaRegistryAvailableEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.message.yaml.YamlMessageServiceInstance;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.zaas.ZaasServiceAvailableEvent;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.springframework.boot.actuate.health.Status.DOWN;
import static org.springframework.boot.actuate.health.Status.UP;

/**
 * This class contributes the apiml component health indication to the main /application/health
 * controlled by class {@link ApimlHealthCheckHandler} in the common package.
 *
 * This is a new structure in the /application/health response
 */
@Component
@RequiredArgsConstructor
public class GatewayHealthIndicator extends AbstractHealthIndicator {

    private static final ApimlLogger apimlLog = ApimlLogger.of(GatewayHealthIndicator.class, YamlMessageServiceInstance.getInstance());
    private final DiscoveryClient discoveryClient;

    @Value("${apiml.catalog.serviceId:}")
    private String apiCatalogServiceId;

    private AtomicBoolean discoveryAvailable = new AtomicBoolean(false);
    private AtomicBoolean zaasAvailable = new AtomicBoolean(false);

    boolean startedInformationPublished = false;

    @Override
    protected void doHealthCheck(Builder builder) throws Exception {
        var anyCatalogIsAvailable = apiCatalogServiceId != null && !apiCatalogServiceId.isEmpty();
        var apiCatalogUp = !this.discoveryClient.getInstances(apiCatalogServiceId).isEmpty();

        // When DS goes 'down' after it was already 'up', the new status is not shown. This is probably feature of
        // Eureka client which caches the status of services. When DS is down the cache is not refreshed.

        // Keeping for backwards compatibility, in modulith the amount of gateways is the amount of authentication services available
        int gatewayCount = this.discoveryClient.getInstances(CoreService.GATEWAY.getServiceId()).size();
        int zaasCount = this.discoveryClient.getInstances(CoreService.GATEWAY.getServiceId()).size();

        builder.status(toStatus(discoveryAvailable.get()))
            .withDetail(CoreService.DISCOVERY.getServiceId(), toStatus(discoveryAvailable.get()).getCode())
            .withDetail(CoreService.ZAAS.getServiceId(), toStatus(zaasAvailable.get()).getCode())
            .withDetail("gatewayCount", gatewayCount)
            .withDetail("zaasCount", zaasCount);

        if (anyCatalogIsAvailable) {
            builder.withDetail(CoreService.API_CATALOG.getServiceId(), toStatus(apiCatalogUp).getCode());
        }

        if (!startedInformationPublished && discoveryAvailable.get() && apiCatalogUp && zaasAvailable.get()) {
            apimlLog.log("org.zowe.apiml.common.mediationLayerStarted");
            startedInformationPublished = true;
        }
    }

    @EventListener
    public void onApplicationEvent(ZaasServiceAvailableEvent event) {
        zaasAvailable.set(true);
    }

    @EventListener
    public void onApplicationEvent(EurekaRegistryAvailableEvent event) {
        discoveryAvailable.set(true);
    }

    private Status toStatus(boolean up) {
        return up ? UP : DOWN;
    }

}
