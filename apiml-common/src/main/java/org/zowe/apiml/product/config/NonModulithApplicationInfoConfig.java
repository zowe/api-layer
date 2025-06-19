/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zowe.apiml.config.ApplicationInfo;
import org.zowe.apiml.product.constants.CoreService;

@Configuration
@ConditionalOnMissingBean(name = "modulithConfig")
public class NonModulithApplicationInfoConfig {

    @Bean
    ApplicationInfo applicationInfo() {
        return ApplicationInfo.builder()
            .isModulith(false)
            .authServiceId(CoreService.ZAAS.getServiceId()).build();
    }
}
