/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.services;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zowe.apiml.eurekaservice.client.util.EurekaMetadataParser;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.product.routing.transform.TransformService;
import org.zowe.apiml.registry.RegistryView;

@Configuration
public class ServerInfoConfig {

    @Bean
    public EurekaMetadataParser getEurekaMetadataParser() {
        return new EurekaMetadataParser();
    }

    @Bean
    public ServicesInfoService servicesInfoService(
        RegistryView registry,
        EurekaMetadataParser eurekaMetadataParser,
        GatewayClient gatewayClient
    ) {
        return new ServicesInfoService(registry, eurekaMetadataParser, gatewayClient, new TransformService(gatewayClient));
    }

}
