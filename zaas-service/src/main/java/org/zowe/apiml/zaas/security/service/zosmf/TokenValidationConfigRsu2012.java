/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service.zosmf;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration of token validation that can handle zOSMF in RSU2012 version
 */
@Configuration
public class TokenValidationConfigRsu2012 {

    @Bean
    @Order(30)
    TokenValidationStrategy authenticateValidationStrategy(@Qualifier("restTemplateWithoutKeystore") RestTemplate restTemplateWithoutKeystore) {
        return new AuthenticatedEndpointStrategy(restTemplateWithoutKeystore, "/zosmf/services/authenticate", HttpMethod.POST);
    }

    @Bean
    @Order(50)
    TokenValidationStrategy authenticateValidationStrategy2(@Qualifier("restTemplateWithoutKeystore") RestTemplate restTemplateWithoutKeystore) {
        return new AuthenticatedEndpointStrategy(restTemplateWithoutKeystore, "/zosmf/notifications/inbox", HttpMethod.GET);
    }

}
