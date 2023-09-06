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

import com.netflix.discovery.EurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zowe.apiml.eurekaservice.client.util.EurekaMetadataParser;
import org.zowe.apiml.product.gateway.GatewayConfigProperties;
import org.zowe.apiml.product.routing.transform.TransformService;

@Configuration
public class ServerInfoConfig {

    @Bean
    public EurekaMetadataParser getEurekaMetadataParser() {
        return new EurekaMetadataParser();
    }

    @Bean
    public ServicesInfoService servicesInfoService(EurekaClient eurekaClient,
                                                   EurekaMetadataParser eurekaMetadataParser, GatewayConfigProperties gatewayConfigProperties, TransformService transformService) {
        return new ServicesInfoService(eurekaClient, eurekaMetadataParser, gatewayConfigProperties, transformService);
    }

}
