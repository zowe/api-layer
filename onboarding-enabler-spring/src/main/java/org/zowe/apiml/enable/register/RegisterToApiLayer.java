/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.enable.register;

import org.zowe.apiml.eurekaservice.client.ApiMediationClient;
import org.zowe.apiml.eurekaservice.client.config.ApiMediationServiceConfig;

import org.zowe.apiml.exception.ServiceDefinitionException;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@RequiredArgsConstructor
public class RegisterToApiLayer {

    /**
     * {@link ApiMediationClient} is a wrapper and initializer for a {@link com.netflix.discovery.EurekaClient} instance.
     * It also provides methods for registering and unregistering API ML services with Eureka server.
     * RegisterToApiLayer class provides the apiMediationClient as a spring bean to be used in code where EurekaClient
     * is needed.
     */
    private final ApiMediationClient apiMediationClient;

    private final ApiMediationServiceConfig newConfig;
    private ApiMediationServiceConfig  config;

    @Value("${apiml.enabled:true}")
    private boolean apimlEnabled;

    @InjectApimlLogger
    private final ApimlLogger logger = ApimlLogger.empty();

    @EventListener(ContextRefreshedEvent.class)
    public void onContextRefreshedEventEvent() {
        if (apimlEnabled) {
            if (apiMediationClient.getEurekaClient() != null) {
                if (config != null) {
                    logger.log( "org.zowe.apiml.enabler.registration.renew"
                        , config.getBaseUrl(), config.getServiceIpAddress(), config.getDiscoveryServiceUrls()
                        , newConfig.getBaseUrl(), newConfig.getServiceIpAddress(), newConfig.getDiscoveryServiceUrls()
                    );
                }

                unregister();
            } else {
                logger.log("org.zowe.apiml.enabler.registration.initial"
                    , newConfig.getBaseUrl(), newConfig.getServiceIpAddress(), newConfig.getDiscoveryServiceUrls()
                );
            }

            register(newConfig);
        }
    }

    @EventListener(ContextClosedEvent.class)
    public void onContextClosedEvent() {
        if (apiMediationClient.getEurekaClient() != null) {
            unregister();
        }
    }

    private void unregister() {
        apiMediationClient.unregister();
    }

    private void register(ApiMediationServiceConfig newConfig) {

        this.config = newConfig;

        try {
            apiMediationClient.register(config);

            logger.log("org.zowe.apiml.enabler.registration.successful",
                config.getBaseUrl(), config.getServiceIpAddress(), config.getDiscoveryServiceUrls());
            log.debug("Registering to API Mediation Layer with settings: {}", config.toString());
        } catch (ServiceDefinitionException e) {
            logger.log("org.zowe.apiml.enabler.registration.fail"
                , config.getBaseUrl(), config.getServiceIpAddress(), config.getDiscoveryServiceUrls(), e.toString());
            log.debug(String.format("Service %s registration to API ML failed: ", config.getBaseUrl()), e);
        }
    }
}
