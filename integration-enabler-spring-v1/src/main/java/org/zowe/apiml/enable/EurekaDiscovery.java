/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.enable;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "mfaas.discovery", value = "enabled", havingValue = "true", matchIfMissing = true)
@EnableEurekaClient
public class EurekaDiscovery {

    @Value("${mfaas.discovery.enabled:true}")
    private String discoveryEnabled;

    @PostConstruct
    public void init() {
        if (Boolean.valueOf(discoveryEnabled)) {
            log.info("This service is discoverable by Eureka.");
        }
    }
}
