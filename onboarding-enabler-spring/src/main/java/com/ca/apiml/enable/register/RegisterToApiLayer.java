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

//import com.ca.apiml.enable.config.ApiMediationServiceConfigBean;
//import com.ca.apiml.enable.config.OnboardingEnablerConfig;
//import com.ca.apiml.enable.config.SslConfigBean;
import com.ca.mfaas.eurekaservice.client.ApiMediationClient;
import com.ca.mfaas.eurekaservice.client.config.ApiMediationServiceConfig;
import com.ca.mfaas.eurekaservice.client.config.Ssl;
import com.ca.mfaas.eurekaservice.client.impl.ApiMediationClientImpl;

import com.ca.mfaas.exception.ServiceDefinitionException;
import com.ca.mfaas.message.log.ApimlLogger;
import com.ca.mfaas.product.logging.annotations.InjectApimlLogger;
//import com.ca.mfaas.product.registry.EurekaClientWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.context.properties.EnableConfigurationProperties;
//import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;

@Slf4j
@Component
@Configuration
//@Import(value = {EurekaClientWrapper.class})
//@EnableConfigurationProperties(value = {OnboardingEnablerConfig.class})
public class RegisterToApiLayer {

    private ApiMediationClient apiMediationClient = new ApiMediationClientImpl();

    @Autowired
    private ApiMediationServiceConfig config;

    @Autowired
    private Ssl ssl;

    @InjectApimlLogger
    private final ApimlLogger logger = ApimlLogger.empty();

//    @Autowired
//    private EurekaClientWrapper eurekaClientWrapper;

    public RegisterToApiLayer() { /*ApiMediationServiceConfig config, Ssl ssl) {
        this.config = config;
        this.ssl = ssl;*/
    }

    @Value("${apiml.enabled:false}")
    private boolean enabled;

    //@Bean
    @Singleton
    public ApiMediationClient apiMediationClient() {
        return apiMediationClient;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onContextRefreshedEventEvent() {
        if (enabled) {
            if (apiMediationClient.getEurekaClient() != null) {
                // TODO: Create appropriate error message
                logger.log("apiml.enabler.register.fail"
                    , config.getBaseUrl(), config.getServiceIpAddress(), config.getDiscoveryServiceUrls());
            } else {
                register(config, ssl);
            }
        }
    }

    @EventListener(ContextStoppedEvent.class)
    public void onContextStoppedEvent() {
        if (apiMediationClient.getEurekaClient() != null) {
            apiMediationClient.unregister();
        }
    }

    private void register(ApiMediationServiceConfig config, Ssl ssl) {
        config.setSsl(ssl);

        logger.log("apiml.enabler.register.successful",
            config.getBaseUrl(), config.getServiceIpAddress(), config.getDiscoveryServiceUrls());
        log.debug("Registering to API Mediation Layer with settings: {}", config.toString());

        try {
            apiMediationClient.register(config);

            // TODO: Substitute eurekaClientWrapper with apiMediatioNClient bean
//            eurekaClientWrapper.setEurekaClient(apiMediationClient.getEurekaClient());
        } catch (ServiceDefinitionException e) {
            logger.log("apiml.enabler.register.fail"
                , config.getBaseUrl(), config.getServiceIpAddress(), config.getDiscoveryServiceUrls(), e.toString());
            log.debug(String.format("Service %s registration to API ML failed: ", config.getBaseUrl()), e);
        }
    }

/*
    @Bean
    @ConfigurationProperties(prefix = "apiml.service")
    public ApiMediationServiceConfig apiMediationServiceConfig() {
        return new ApiMediationServiceConfig();
    }

    @Bean
    @ConfigurationProperties(prefix = "server.ssl")
    public Ssl ssl() {
        return new Ssl();
    }
*/
}
