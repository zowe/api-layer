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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.preauth.x509.SubjectDnX509PrincipalExtractor;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.zowe.apiml.product.constants.CoreService;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class WebSecurity {

    @Value("${apiml.security.x509.registry.allowedUsers:}")
    private List<String> usersAllowList = new ArrayList<>();

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {

        SubjectDnX509PrincipalExtractor principalExtractor = new SubjectDnX509PrincipalExtractor();

        ReactiveAuthenticationManager authenticationManager = authentication -> {
            authentication.setAuthenticated(usersAllowList.contains(authentication.getName()));
            return Mono.just(authentication);
        };

        http.x509(x509 ->
                x509
                    .principalExtractor(principalExtractor)
                    .authenticationManager(authenticationManager)).authorizeExchange()
            .pathMatchers("/" + CoreService.CLOUD_GATEWAY.getServiceId() + "/api/v1/registry/**").authenticated()
            .and().csrf().disable()
            .authorizeExchange().anyExchange().permitAll();

        return http.build();
    }

    @Bean
    @Primary
    ReactiveUserDetailsService userDetailsService() {

        return username -> {
            List<GrantedAuthority> authorities = new ArrayList<>();
            if (usersAllowList.stream().anyMatch(allowedUser -> allowedUser != null && allowedUser.equalsIgnoreCase(username))) {
                authorities.add(new SimpleGrantedAuthority("REGISTRY"));
            }
            UserDetails userDetails = User.withUsername(username).authorities(authorities).password("pass").build();
            return Mono.just(userDetails);
        };
    }

}
