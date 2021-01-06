/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.caching.service.vsam.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zowe.apiml.caching.service.Storage;
import org.zowe.apiml.caching.service.vsam.VsamStorage;

@Configuration
@RequiredArgsConstructor
public class VsamConfiguration {
    private final VsamConfig vsamConfig;

    @ConditionalOnProperty(name = "caching.storage.mode", havingValue = "vsam")
    @Bean
    public Storage vsam() {
        return new VsamStorage(vsamConfig);
    }


}
