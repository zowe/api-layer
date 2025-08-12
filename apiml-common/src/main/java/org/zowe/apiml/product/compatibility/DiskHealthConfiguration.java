/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.compatibility;

import org.springframework.boot.actuate.autoconfigure.system.DiskSpaceHealthContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.system.DiskSpaceHealthIndicatorProperties;
import org.springframework.boot.actuate.system.DiskSpaceHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration class that replaces the default DiskSpaceHealthIndicator
 */
@Configuration
@AutoConfigureBefore(DiskSpaceHealthContributorAutoConfiguration.class)
public class DiskHealthConfiguration {
    /**
     * Replace the default DiskSpaceHealthIndicator with our custom implementation
     */
    @Bean
    @Primary
    //@ConditionalOnProperty(prefix = "management.health.diskspace", name = "enabled", matchIfMissing = true)
    public DiskSpaceHealthIndicator diskSpaceHealthIndicator(DiskSpaceHealthIndicatorProperties properties) {
        return new CustomDiskSpaceHealthIndicator(properties.getPath(), properties.getThreshold());
    }
}
