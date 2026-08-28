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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRegisteredEvent;
import org.springframework.cloud.netflix.eureka.server.event.EurekaRegistryAvailableEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.zowe.apiml.apicatalog.ApiCatalogServiceAvailableEvent;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.compatibility.ApimlHealthCheckHandler;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.product.service.ServiceStartupEventHandler;
import org.zowe.apiml.zaas.ZaasServiceAvailableEvent;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
@Slf4j
public class GatewayHealthIndicator extends AbstractHealthIndicator implements InitializingBean {

    private final ApplicationContext applicationContext;
    private final ServiceStartupEventHandler serviceStartupEventHandler;

    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();

    private DiscoveryClient discoveryClient;

    @Value("${apiml.catalog.serviceId:}")
    private String apiCatalogServiceId;

    private AtomicBoolean discoveryAvailable = new AtomicBoolean(false);
    private AtomicBoolean zaasAvailable = new AtomicBoolean(false);
    private AtomicBoolean catalogAvailable = new AtomicBoolean(false);

    private AtomicBoolean startedInformationPublished = new AtomicBoolean(false);
    private AtomicBoolean startedHaInformationPublished = new AtomicBoolean(false);

    private AtomicInteger gatewayCount = new AtomicInteger(0);
    private AtomicInteger zaasCount = new AtomicInteger(0);

    private Integer expectedInstanceCount;

    @Override
    public void afterPropertiesSet() throws Exception {
        expectedInstanceCount = Optional.ofNullable(System.getenv("ZWE_DISCOVERY_SERVICES_LIST"))
            .map(discoveryServicesList -> discoveryServicesList.split(","))
            .map(i -> i.length)
            .orElse(1);

        discoveryClient = applicationContext.getBean(DiscoveryClient.class);
    }

    @Override
    protected void doHealthCheck(Builder builder) throws Exception {
        var anyCatalogIsAvailable = StringUtils.isNotBlank(apiCatalogServiceId);

        catalogAvailable.set(anyCatalogIsAvailable && !discoveryClient.getInstances(apiCatalogServiceId).isEmpty());

        refreshInstanceCounts();

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
        if (isFullyHaUp()) {
            onFullyHaUp();
        }
    }

    private void refreshInstanceCounts() {
        // Keeping for backwards compatibility, in modulith the amount of gateways is the amount of authentication services available
        gatewayCount.compareAndSet(expectedInstanceCount, this.discoveryClient.getInstances(CoreService.GATEWAY.getServiceId()).size());
        zaasCount.set(gatewayCount.get());
    }

    private boolean isFullyUp() {
        return !startedInformationPublished.get() && discoveryAvailable.get() && catalogAvailable.get() && zaasAvailable.get();
    }

    private void onFullyUp() {
        if (startedInformationPublished.compareAndSet(false, true)) {
            apimlLog.log("org.zowe.apiml.common.mediationLayerStarted");
        }
    }

    private boolean isFullyHaUp() {
        if (expectedInstanceCount > 1) {
            refreshInstanceCounts();
            return expectedInstanceCount == gatewayCount.get();
        }
        return false;
    }

    private void onFullyHaUp() {
        if (startedHaInformationPublished.compareAndSet(false, true)) {
            apimlLog.log("org.zowe.apiml.common.mediationLayerStartedHA");
        }
    }

    @EventListener
    public void onApplicationEvent(ZaasServiceAvailableEvent event) {
        zaasAvailable.set(true);
        if (isFullyUp()) {
            onFullyUp();
        }
        if (isFullyHaUp()) {
            onFullyHaUp();
        }
    }

    @EventListener
    public void onApplicationEvent(EurekaRegistryAvailableEvent event) {
        discoveryAvailable.set(true);
        if (isFullyUp()) {
            onFullyUp();
        }
        if (isFullyHaUp()) {
            onFullyHaUp();
        }
    }

    @EventListener
    public void onApplicationEvent(EurekaInstanceRegisteredEvent event) {
        var instanceInfo = event.getInstanceInfo();
        if (String.valueOf(instanceInfo.getAppName()).equalsIgnoreCase(apiCatalogServiceId) && catalogAvailable.compareAndSet(false, true)) {
            serviceStartupEventHandler.onServiceStartup("API Catalog Service", ServiceStartupEventHandler.DEFAULT_DELAY_FACTOR);
        }
        if (isFullyUp()) {
            onFullyUp();
        }
        if (isFullyHaUp()) {
            onFullyHaUp();
        }
    }

    @EventListener
    public void onApplicationEvent(ApiCatalogServiceAvailableEvent event) {
        if (catalogAvailable.compareAndSet(false, true)) {
            serviceStartupEventHandler.onServiceStartup("API Catalog Service", ServiceStartupEventHandler.DEFAULT_DELAY_FACTOR);
        }
        if (isFullyUp()) {
            onFullyUp();
        }
        if (isFullyHaUp()) {
            onFullyHaUp();
        }
    }

    boolean isStartedInformationPublished() {
        return startedInformationPublished.get();
    }

    private Status toStatus(boolean up) {
        return up ? UP : DOWN;
    }

}
