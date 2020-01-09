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

import com.ca.apiml.enable.config.ApiMediationServiceConfigBean;
import com.ca.apiml.enable.config.SslConfigBean;
import com.ca.mfaas.eurekaservice.client.ApiMediationClient;
import com.ca.mfaas.eurekaservice.client.config.ApiMediationServiceConfig;
import com.ca.mfaas.eurekaservice.client.config.Ssl;
import com.ca.mfaas.eurekaservice.client.impl.ApiMediationClientImpl;

import com.ca.mfaas.exception.ServiceDefinitionException;
import com.ca.mfaas.message.log.ApimlLogger;
import com.ca.mfaas.product.logging.annotations.InjectApimlLogger;
import com.ca.mfaas.product.registry.EurekaClientWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Import(value = {EurekaClientWrapper.class})
@EnableConfigurationProperties(value = {ApiMediationServiceConfigBean.class, SslConfigBean.class}) //, EurekaClientWrapper.class
public class RegisterToApiLayer {
    private final ApiMediationServiceConfigBean config;
    private final SslConfigBean ssl;

    @InjectApimlLogger
    private final ApimlLogger logger = ApimlLogger.empty();

    @Autowired
    private EurekaClientWrapper eurekaClientWrapper;

    public RegisterToApiLayer(ApiMediationServiceConfigBean config, SslConfigBean ssl) {
        this.config = config;
        this.ssl = ssl;
    }

    @Value("${apiml.enabled:false}")
    private boolean enabled;

    @EventListener(ContextRefreshedEvent.class)
    public void onContextRefreshedEventEvent() {
        if (enabled) {
            register(config, ssl);
        }
    }

    private void register(ApiMediationServiceConfig config, Ssl ssl) {
        config.setSsl(ssl);

        logger.log("apiml.enabler.register.successful",
            config.getBaseUrl(), config.getServiceIpAddress(), config.getDiscoveryServiceUrls());
        log.debug("Registering to API Mediation Layer with settings: {}", config.toString());

        try {
            ApiMediationClient apiMediationClient = new ApiMediationClientImpl();
            apiMediationClient.register(config);

            eurekaClientWrapper.setEurekaClient(apiMediationClient.getEurekaClient());
        } catch (ServiceDefinitionException e) {
            logger.log("apiml.enabler.register.fail"
                , config.getBaseUrl(), config.getServiceIpAddress(), config.getDiscoveryServiceUrls(), e.toString());
            log.debug(String.format("Service %s registration to API ML failed: ", config.getBaseUrl()), e);
        }
    }
}
