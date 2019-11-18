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
import com.ca.mfaas.eurekaservice.client.config.Eureka;
import com.ca.mfaas.eurekaservice.client.config.Ssl;
import com.ca.mfaas.eurekaservice.client.impl.ApiMediationClientImpl;

import com.ca.mfaas.message.core.MessageService;
import com.ca.mfaas.message.log.ApimlLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Registers service to a Discovery Service when an {@code ApplicationContext} gets initialized or refreshed.
 */
@Slf4j
@Component
@EnableConfigurationProperties(value = {ApiMediationServiceConfigBean.class, SslConfigBean.class})
public class RegisterToApiLayer {
    private final ApiMediationServiceConfigBean config;
    private final SslConfigBean ssl;
    private final ApimlLogger logger;

    public RegisterToApiLayer(ApiMediationServiceConfigBean config, SslConfigBean ssl, MessageService messageService) {
        this.config = config;
        this.ssl = ssl;
        this.logger = ApimlLogger.of(RegisterToApiLayer.class, messageService);
    }

    @Value("${apiml.service.ipAddress:127.0.0.1}")
    private String ipAddress;

    @Value("${apiml.enabled:false}")
    private boolean enabled;

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (enabled) {
            register(config, ssl);
        }
    }

    private void register(ApiMediationServiceConfig config, Ssl ssl) {
        ApiMediationClient apiMediationClient = new ApiMediationClientImpl();
        config.setSsl(ssl);
        config.setEureka(new Eureka(null, null, ipAddress));
        logger.log("apiml.enabler.register.successful",
            config.getBaseUrl(), config.getEureka().getIpAddress(), config.getDiscoveryServiceUrls());
        log.debug("Registering to API Mediation Layer with settings: {}", config.toString());
        apiMediationClient.register(config);
    }
}
