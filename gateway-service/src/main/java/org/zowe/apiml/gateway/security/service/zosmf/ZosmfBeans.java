/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.service.zosmf;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Config for zOSMF beans
 */
@Configuration
public class ZosmfBeans {

    @Bean
    @ConditionalOnProperty(prefix = "apiml.security.zosmf", name = "tokenValidation", havingValue = "authenticate-endpoint")
    TokenValidationStrategy authenticateValidationStrategy(@Qualifier("restTemplateWithoutKeystore") RestTemplate restTemplateWithoutKeystore) {
        return new AuthenticateEndpointStrategy(restTemplateWithoutKeystore);
    }

    @Bean
    @ConditionalOnProperty(prefix = "apiml.security.zosmf", name = "tokenValidation", havingValue = "generic-endpoint")
    TokenValidationStrategy genericValidationStrategy(@Qualifier("restTemplateWithoutKeystore") RestTemplate restTemplateWithoutKeystore) {
        return new GenericEndpointStrategy(restTemplateWithoutKeystore);
    }
}
