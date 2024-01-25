/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.standalone;


import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.zowe.apiml.product.constants.CoreService;

@Configuration
@ConditionalOnProperty(value = "apiml.catalog.standalone.enabled", havingValue = "true")
@Slf4j
public class StandaloneSecurityConfig {

    @PostConstruct
    void init() {
        log.warn(CoreService.API_CATALOG.getServiceId() + " is running in standalone mode. Authentication is disabled. Do not use in production.");
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain permitAll(HttpSecurity http) throws Exception {
        return http
            .csrf(CsrfConfigurer::disable)   // NOSONAR
            .headers(httpSecurityHeadersConfigurer ->
                httpSecurityHeadersConfigurer.httpStrictTransportSecurity(HeadersConfigurer.HstsConfig::disable)
                    .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))

            .authorizeHttpRequests(matcherRegistry -> matcherRegistry.anyRequest().permitAll())
            .build();
    }

}
