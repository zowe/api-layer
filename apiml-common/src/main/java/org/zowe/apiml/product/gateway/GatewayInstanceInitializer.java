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

import org.springframework.cloud.client.ServiceInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.product.instance.InstanceInitializationException;
import org.zowe.apiml.product.instance.ServiceAddress;
import org.zowe.apiml.product.instance.lookup.InstanceLookupExecutor;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;

import java.net.URI;

/**
 * GatewayInstanceInitializer takes care about starting the lookup for Gateway instance after the context is started
 * Its meant to be created as a bean, as it is for example by SecurityServiceConfiguration in security-service-client-spring
 */
@Slf4j
@RequiredArgsConstructor // TODO remove this once modulith is complete and fix for microservice setup
public class GatewayInstanceInitializer {

    private final InstanceLookupExecutor instanceLookupExecutor;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final GatewayClient gatewayClient;

    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();

    /**
     * Derives the Gateway's address from the discovered instance.
     * <p>
     * Uses {@link ServiceInstance#getUri()} rather than parsing the Eureka {@code homePageUrl}. This method only
     * ever took the scheme, host and port out of that URL, and {@code getUri()} is exactly those three - so the
     * result is unchanged while the Netflix type disappears. It also no longer depends on a registrant having set
     * a home-page URL at all.
     */
    private ServiceAddress process(ServiceInstance instance) {
        try {
            URI uri = instance.getUri();
            log.debug("Gateway instance URI: {}", uri);
            return ServiceAddress.builder()
                .scheme(uri.getScheme())
                .hostname(uri.getHost() + ":" + uri.getPort())
                .build();
        } catch (Exception e) {
            throw new InstanceInitializationException(e.getMessage());
        }

    }

    @EventListener({HeartbeatEvent.class, ApplicationReadyEvent.class})
    public void init() {

        log.debug("GatewayInstanceInitializer starting asynchronous initialization of Gateway configuration");

        instanceLookupExecutor.run(
            CoreService.GATEWAY.getServiceId(),
            instance -> {
                ServiceAddress foundGatewayConfigProperties = process(instance);

                log.debug(
                    "GatewayInstanceInitializer has been initialized with Gateway instance on url: {}://{}",
                    foundGatewayConfigProperties.getScheme(),
                    foundGatewayConfigProperties.getHostname()
                );

                gatewayClient.setGatewayConfigProperties(foundGatewayConfigProperties);
                applicationEventPublisher.publishEvent(new GatewayLookupCompleteEvent(this));
            },
            (exception, isStopped) -> {
                if (Boolean.TRUE.equals(isStopped)) {
                    apimlLog.log("org.zowe.apiml.common.gatewayInstanceInitializerStopped", exception.getMessage());
                }
            }
        );
    }
}
