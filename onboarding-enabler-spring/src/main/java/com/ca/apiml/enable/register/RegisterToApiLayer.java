/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.apiml.enable.register;

import com.ca.mfaas.eurekaservice.client.ApiMediationClient;
import com.ca.mfaas.eurekaservice.client.config.ApiMediationServiceConfig;
import com.ca.mfaas.eurekaservice.client.config.Ssl;

import com.ca.mfaas.exception.ServiceDefinitionException;
import com.ca.mfaas.message.log.ApimlLogger;
import com.ca.mfaas.product.logging.annotations.InjectApimlLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@Configuration
public class RegisterToApiLayer {

    /**
     * {@link ApiMediationClient} is a wrapper and initializer for a {@link com.netflix.discovery.EurekaClient} instance.
     * It also provides methods for registering and unregistering API ML services with Eureka server.
     * RegisterToApiLayer class provides the apiMediationClient as a spring bean to be used in code where EurekaClient
     * is needed.
     */
    @Autowired
    private ApiMediationClient apiMediationClient;

    @Autowired
    private ApiMediationServiceConfig _config;
    private ApiMediationServiceConfig  config;

    @Autowired
    private Ssl _ssl;
    private Ssl  ssl;

    @Autowired
    private Boolean apimlEnabled;

    @InjectApimlLogger
    private final ApimlLogger logger = ApimlLogger.empty();

    public RegisterToApiLayer() {
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onContextRefreshedEventEvent() {
        if (Boolean.TRUE.equals(apimlEnabled)) {

            if (apiMediationClient.getEurekaClient() != null) {
                if (config != null) {
                    logger.log("apiml.enabler.registration.renew"
                        , config.getBaseUrl(), config.getServiceIpAddress(), config.getDiscoveryServiceUrls()
                        , _config.getBaseUrl(), _config.getServiceIpAddress(), _config.getDiscoveryServiceUrls()
                    );
                }

                unregister();
            } else {
                logger.log("apiml.enabler.registration.initial"
                    , _config.getBaseUrl(), _config.getServiceIpAddress(), _config.getDiscoveryServiceUrls()
                );
            }

            register(_config, _ssl);
        }
    }

    @EventListener(ContextStoppedEvent.class)
    public void onContextStoppedEvent() {
        if (apiMediationClient.getEurekaClient() != null) {
            unregister();
        }
    }

    private void unregister() {
        apiMediationClient.unregister();
    }

    private void register(ApiMediationServiceConfig config, Ssl ssl) {

        config.setSsl(ssl);

        try {
            apiMediationClient.register(config);

            // TODO: Deep copy
            config = _config;
            ssl = _ssl;

            logger.log("apiml.enabler.registration.successful",
                config.getBaseUrl(), config.getServiceIpAddress(), config.getDiscoveryServiceUrls());
            log.debug("Registering to API Mediation Layer with settings: {}", config.toString());
        } catch (ServiceDefinitionException e) {
            logger.log("apiml.enabler.registration.fail"
                , config.getBaseUrl(), config.getServiceIpAddress(), config.getDiscoveryServiceUrls(), e.toString());
            log.debug(String.format("Service %s registration to API ML failed: ", config.getBaseUrl()), e);
        }
    }
}
