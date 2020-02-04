/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.discovery.config;

import org.zowe.apiml.discovery.ApimlInstanceRegistry;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.resources.ServerCodecs;
import org.springframework.cloud.netflix.eureka.server.InstanceRegistryProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration to rewrite default Eureka's implementation with custom one
 */
@Configuration
public class EurekaConfig {

    @Bean
    @Primary
    public ApimlInstanceRegistry getApimlInstanceRegistry(
        EurekaServerConfig serverConfig,
        EurekaClientConfig clientConfig,
        ServerCodecs serverCodecs,
        EurekaClient eurekaClient,
        InstanceRegistryProperties instanceRegistryProperties)
    {
        eurekaClient.getApplications(); // force initialization
        return new ApimlInstanceRegistry(serverConfig, clientConfig, serverCodecs, eurekaClient, instanceRegistryProperties);
    }

}
