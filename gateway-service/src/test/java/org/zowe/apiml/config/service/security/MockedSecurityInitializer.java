/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.config.service.security;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.zowe.apiml.gateway.security.service.JwtSecurityInitializer;

@SpringBootConfiguration
public class MockedSecurityInitializer {
    @Bean
    public JwtSecurityInitializer jwtSecurityInitializer() {
        return new JwtSecurityInitializer();
    }
}
