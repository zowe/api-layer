/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.security.common.auth.saf;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Configuration
@Slf4j
public class SafResourceAccessConfig {

    @Value("${apiml.security.safEndpoint.enabled:false}")
    private boolean endpointEnabled;

    @Bean
    public SafResourceAccessVerifying safResourceAccessVerifying(RestTemplate restTemplate) throws IOException {
        // TODO: SAF using JZOS - another PR

        if (endpointEnabled) {
            return new SafResourceAccessEndpoint(restTemplate);
        }

        return new SafResourceAccessDummy();
    }

}
