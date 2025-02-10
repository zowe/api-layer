/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.zowe.apiml.security.common.config.SafSecurityConfigurationProperties;
import org.zowe.apiml.zaas.security.config.CompoundAuthManager;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(SafSecurityConfigurationProperties.class)
public class WebSecurity {
    public static final  String LOGIN = "/gateway/api/v1/auth/login";

    private final CompoundAuthManager compoundAuthManager;
    @Bean
    @Order(1)
    public SecurityWebFilterChain loginSecurityWebFilterChain(ServerHttpSecurity http) {
        return defaultSecurityConfig(http)
            .securityMatcher(ServerWebExchangeMatchers.pathMatchers(
                LOGIN
            ))
            .authorizeExchange(authorizeExchangeSpec ->
                authorizeExchangeSpec
                    .anyExchange().authenticated()
            )
            .authenticationManager(compoundAuthManager)
            .build();
    }



    public ServerHttpSecurity defaultSecurityConfig(ServerHttpSecurity http) {

        return http
            .headers(customizer -> customizer.frameOptions(ServerHttpSecurity.HeaderSpec.FrameOptionsSpec::disable))

            .csrf(ServerHttpSecurity.CsrfSpec::disable);
    }
}
