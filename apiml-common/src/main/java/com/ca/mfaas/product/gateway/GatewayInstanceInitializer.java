/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.product.gateway;

import com.ca.mfaas.product.constants.CoreService;
import com.ca.mfaas.product.lookup.InstanceLookupExecutor;
import com.netflix.appinfo.InstanceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;

import java.net.URI;

@Slf4j
public class GatewayInstanceInitializer {

    private final InstanceLookupExecutor instanceLookupExecutor;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final GatewayClient gatewayClient;

    public GatewayInstanceInitializer(
        InstanceLookupExecutor instanceLookupExecutor,
        ApplicationEventPublisher applicationEventPublisher,
        GatewayClient gatewayClient) {
        this.instanceLookupExecutor = instanceLookupExecutor;
        this.applicationEventPublisher = applicationEventPublisher;
        this.gatewayClient = gatewayClient;
    }

    private GatewayConfigProperties process(InstanceInfo instanceInfo) {
        try {
            String gatewayHomePage = instanceInfo.getHomePageUrl();
            URI uri = new URI(gatewayHomePage);

            return GatewayConfigProperties.builder()
                .scheme(uri.getScheme())
                .hostname(uri.getHost() + ":" + uri.getPort())
                .build();
        } catch (Exception e) {
            String msg = "An unexpected error occurred while retrieving Gateway instance from Discovery service";
            log.warn(msg, e);
            throw new RuntimeException(msg, e);
        }

    }

    @EventListener
    private void init(ApplicationReadyEvent applicationReadyEvent) {
        if (gatewayClient.isInitialized()) {
            return;
        }

        log.info("GatewayLookupService starting asynchronous initialization of Gateway configuration");

        instanceLookupExecutor.run(
            CoreService.GATEWAY.getServiceId(),
            instance -> {
                GatewayConfigProperties foundGatewayConfigProperties = process(instance);

                log.info(
                    "GatewayLookupService has been initialized with Gateway instance on url: {}://{}",
                    foundGatewayConfigProperties.getScheme(),
                    foundGatewayConfigProperties.getHostname()
                );

                gatewayClient.setGatewayConfigProperties(foundGatewayConfigProperties);
                applicationEventPublisher.publishEvent(new GatewayLookupCompleteEvent(this));
            });
    }
}
