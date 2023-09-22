/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.preauth.x509.SubjectDnX509PrincipalExtractor;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.util.ArrayList;

@Configuration
public class WebSecurity {

    @Value("${apiml.security.x509.serviceList.allowedUsers:-}")
     private ArrayList usersList = new ArrayList<>();
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {


        SubjectDnX509PrincipalExtractor principalExtractor = new SubjectDnX509PrincipalExtractor();

        ReactiveAuthenticationManager authenticationManager = authentication -> {
            authentication.setAuthenticated(usersList.contains(authentication.getName()));
            return Mono.just(authentication);
        };

        http.x509(x509 ->
            x509
                .principalExtractor(principalExtractor)
                .authenticationManager(authenticationManager)).authorizeExchange().pathMatchers("/registry/**").authenticated();

        return http.build();
    }

    @Bean
    @Primary
    ReactiveUserDetailsService userDetailsService() {

        return username -> Mono.just(User.withUsername(username).password("")
           .authorities("user") // should be enhanced for user roles
           .build());
    }

}
