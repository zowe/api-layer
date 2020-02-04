/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.security.client.config;

import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.product.gateway.GatewayInstanceInitializer;
import org.zowe.apiml.product.instance.lookup.InstanceLookupExecutor;
import com.netflix.discovery.EurekaClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * General configuration of security client
 */
@Configuration
@ComponentScan({"org.zowe.apiml.security", "org.zowe.apiml.product.gateway"})
public class SecurityServiceConfiguration {

    @Bean
    public GatewayInstanceInitializer gatewayInstanceInitializer(
        @Qualifier("eurekaClient") EurekaClient eurekaClient,
        ApplicationEventPublisher applicationEventPublisher,
        GatewayClient gatewayClient) {


        return new GatewayInstanceInitializer(
            new InstanceLookupExecutor(eurekaClient),
            applicationEventPublisher,
            gatewayClient);
    }
}
