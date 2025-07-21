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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRegisteredEvent;
import org.springframework.cloud.netflix.eureka.server.event.EurekaRegistryAvailableEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.compatibility.ApimlHealthCheckHandler;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.zaas.ZaasServiceAvailableEvent;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.springframework.boot.actuate.health.Status.DOWN;
import static org.springframework.boot.actuate.health.Status.UP;

/**
 * This class contributes the apiml component health indication to the main /application/health
 * controlled by class {@link ApimlHealthCheckHandler} in the common package.
 *
 * Note: Name is kept as GatewayHealthIndicator for backwards compatibility
 */
@Component
@RequiredArgsConstructor
public class GatewayHealthIndicator extends AbstractHealthIndicator {

    private final DiscoveryClient discoveryClient;

    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();

    @Value("${apiml.catalog.serviceId:}")
    private String apiCatalogServiceId;

    private AtomicBoolean discoveryAvailable = new AtomicBoolean(false);
    private AtomicBoolean zaasAvailable = new AtomicBoolean(false);
    private AtomicBoolean catalogAvailable = new AtomicBoolean(false);

    private AtomicBoolean startedInformationPublished = new AtomicBoolean(false);

    @Override
    protected void doHealthCheck(Builder builder) throws Exception {
        var anyCatalogIsAvailable = StringUtils.isNotBlank(apiCatalogServiceId);
        catalogAvailable.set(anyCatalogIsAvailable && !this.discoveryClient.getInstances(apiCatalogServiceId).isEmpty());

        // Keeping for backwards compatibility, in modulith the amount of gateways is the amount of authentication services available
        var gatewayCount = this.discoveryClient.getInstances(CoreService.GATEWAY.getServiceId()).size();
        var zaasCount = gatewayCount;

        builder.status(toStatus(discoveryAvailable.get() && zaasAvailable.get()))
            .withDetail(CoreService.DISCOVERY.getServiceId(), toStatus(discoveryAvailable.get()).getCode())
            .withDetail(CoreService.ZAAS.getServiceId(), toStatus(zaasAvailable.get()).getCode())
            .withDetail("gatewayCount", gatewayCount)
            .withDetail("zaasCount", zaasCount);

        if (anyCatalogIsAvailable) {
            builder.withDetail(CoreService.API_CATALOG.getServiceId(), toStatus(catalogAvailable.get()).getCode());
        }

        if (isFullyUp()) {
            onFullyUp();
        }
    }

    private boolean isFullyUp() {
        return !startedInformationPublished.get() && discoveryAvailable.get() && catalogAvailable.get() && zaasAvailable.get();
    }

    private void onFullyUp() {
        if (startedInformationPublished.compareAndSet(false, true)) {
            apimlLog.log("org.zowe.apiml.common.mediationLayerStarted");
        }
    }

    @EventListener
    public void onApplicationEvent(ZaasServiceAvailableEvent event) {
        zaasAvailable.set(true);
        if (isFullyUp()) {
            onFullyUp();
        }
    }

    @EventListener
    public void onApplicationEvent(EurekaRegistryAvailableEvent event) {
        discoveryAvailable.set(true);
        if (isFullyUp()) {
            onFullyUp();
        }
    }

    @EventListener
    public void onApplicationEvent(EurekaInstanceRegisteredEvent event) {
        var instanceInfo = event.getInstanceInfo();
        if (String.valueOf(instanceInfo.getAppName()).equalsIgnoreCase(apiCatalogServiceId)) {
            catalogAvailable.set(true);
        }
        if (isFullyUp()) {
            onFullyUp();
        }
    }

    boolean isStartedInformationPublished() {
        return startedInformationPublished.get();
    }

    private Status toStatus(boolean up) {
        return up ? UP : DOWN;
    }

}
