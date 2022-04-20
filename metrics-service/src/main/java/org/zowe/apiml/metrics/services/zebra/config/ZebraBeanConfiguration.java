/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.metrics.services.zebra.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.metrics.services.zebra.ZebraMetricsService;

@RequiredArgsConstructor
@Configuration
@ConditionalOnProperty(name = "metrics.system.service", havingValue = "zebra")
public class ZebraBeanConfiguration {

    private final ZebraConfiguration zebraConfiguration;

    @Bean
    public ZebraMetricsService zebraMetricsService(RestTemplate restTemplate) {
        return new ZebraMetricsService(zebraConfiguration.getBaseUrl(), restTemplate);
    }
}
