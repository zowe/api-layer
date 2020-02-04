/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.product.gateway;

import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.product.instance.InstanceInitializationException;
import org.zowe.apiml.product.instance.lookup.InstanceLookupExecutor;
import com.netflix.appinfo.InstanceInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;

import java.net.URI;

/**
 * GatewayInstanceInitializer takes care about starting the lookup for Gateway instance after the context is started
 * Its meant to be created as a bean, as it is for example by SecurityServiceConfiguration in security-service-client-spring
 */
@Slf4j
@RequiredArgsConstructor
public class GatewayInstanceInitializer {

    private final InstanceLookupExecutor instanceLookupExecutor;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final GatewayClient gatewayClient;

    @InjectApimlLogger
    private ApimlLogger apimlLog = ApimlLogger.empty();

    private GatewayConfigProperties process(InstanceInfo instanceInfo) {
        try {
            String gatewayHomePage = instanceInfo.getHomePageUrl();
            URI uri = new URI(gatewayHomePage);
            log.debug("Gateway homePageUrl: " + gatewayHomePage);
            return GatewayConfigProperties.builder()
                .scheme(uri.getScheme())
                .hostname(uri.getHost() + ":" + uri.getPort())
                .build();
        } catch (Exception e) {
            throw new InstanceInitializationException(e.getMessage());
        }

    }

    /**
     * EventListener method that starts the lookup for Gateway
     * Listens for {@link ApplicationReadyEvent} to start the {@link InstanceLookupExecutor} and provides the processing logic for the executor
     */
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        if (gatewayClient.isInitialized()) {
            return;
        }

        log.info("GatewayInstanceInitializer starting asynchronous initialization of Gateway configuration");

        instanceLookupExecutor.run(
            CoreService.GATEWAY.getServiceId(),
            instance -> {
                GatewayConfigProperties foundGatewayConfigProperties = process(instance);

                log.info(
                    "GatewayInstanceInitializer has been initialized with Gateway instance on url: {}://{}",
                    foundGatewayConfigProperties.getScheme(),
                    foundGatewayConfigProperties.getHostname()
                );

                gatewayClient.setGatewayConfigProperties(foundGatewayConfigProperties);
                applicationEventPublisher.publishEvent(new GatewayLookupCompleteEvent(this));
            },
            (exception, isStopped) -> {
                if (Boolean.TRUE.equals(isStopped)) {
                    apimlLog.log("apiml.common.gatewayInstanceInitializerStopped", exception.getMessage());
                }
            }
        );
    }
}
