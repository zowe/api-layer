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

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.compatibility.ApimlHealthCheckHandler;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.springframework.boot.actuate.health.Status.DOWN;
import static org.springframework.boot.actuate.health.Status.UP;

/**
 * Gateway health information (/application/health)
 * This class contributes Gateway's information to {@link ApimlHealthCheckHandler}
 *
 */
@Component
@ConditionalOnMissingBean(name = "modulithConfig")
public class GatewayHealthIndicator extends AbstractHealthIndicator {

    protected final DiscoveryClient discoveryClient;
    private final String apiCatalogServiceId;
    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();

    private AtomicBoolean startedInformationPublished = new AtomicBoolean(false);

    public GatewayHealthIndicator(DiscoveryClient discoveryClient,
                                  @Value("${apiml.catalog.serviceId:}") String apiCatalogServiceId) {
        this.discoveryClient = discoveryClient;
        this.apiCatalogServiceId = apiCatalogServiceId;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        var anyCatalogIsAvailable = StringUtils.isNotBlank(apiCatalogServiceId);
        var apiCatalogUp = anyCatalogIsAvailable && !this.discoveryClient.getInstances(apiCatalogServiceId).isEmpty();

        // When DS goes 'down' after it was already 'up', the new status is not shown. This is probably feature of
        // Eureka client which caches the status of services. When DS is down the cache is not refreshed.
        var discoveryUp = !this.discoveryClient.getInstances(CoreService.DISCOVERY.getServiceId()).isEmpty();
        var zaasUp = !this.discoveryClient.getInstances(CoreService.ZAAS.getServiceId()).isEmpty();

        var gatewayCount = this.discoveryClient.getInstances(CoreService.GATEWAY.getServiceId()).size();
        var zaasCount = this.discoveryClient.getInstances(CoreService.ZAAS.getServiceId()).size();

        builder.status(toStatus(discoveryUp))
            .withDetail(CoreService.DISCOVERY.getServiceId(), toStatus(discoveryUp).getCode())
            .withDetail(CoreService.ZAAS.getServiceId(), toStatus(zaasUp).getCode())
            .withDetail("gatewayCount", gatewayCount)
            .withDetail("zaasCount", zaasCount);

        if (anyCatalogIsAvailable) {
            builder.withDetail(CoreService.API_CATALOG.getServiceId(), toStatus(apiCatalogUp).getCode());
        }

        if (discoveryUp && apiCatalogUp && zaasUp) {
            onFullyUp();
        }
    }

    private void onFullyUp() {
        if (startedInformationPublished.compareAndSet(false, true)) {
            apimlLog.log("org.zowe.apiml.common.mediationLayerStarted");
        }
    }

    boolean isStartedInformationPublished() {
        return startedInformationPublished.get();
    }

    private Status toStatus(boolean up) {
        return up ? UP : DOWN;
    }
}
