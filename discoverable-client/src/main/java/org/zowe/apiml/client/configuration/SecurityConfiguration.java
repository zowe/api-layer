/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.client.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Configuration
    @Order(1)
    class Oauth2Sec {

        @Bean
        public SecurityFilterChain filterChainOath(HttpSecurity http) throws Exception {
            return http.csrf().disable().requestMatchers().antMatchers("/whoami").and() // NOSONAR
                .authorizeRequests()
                .anyRequest().authenticated()
                .and().oauth2ResourceServer().jwt().and()
                .and().build();
        }
    }

    @Configuration
    @Order(2)
    class WebSocketSec {
        @Bean
        public SecurityFilterChain filterChainWebSocket(HttpSecurity http) throws Exception {
            return http.csrf().disable().requestMatchers().antMatchers("/ws/**").and() // NOSONAR
                .authorizeRequests()
                .anyRequest().authenticated()
                .and().httpBasic()
                .and().build();
        }

        @Bean
        public InMemoryUserDetailsManager userDetailsService() {
            UserDetails user = User.withDefaultPasswordEncoder() // NOSONAR deprecated only to indicate not acceptable for production
                .username("user")
                .password("pass")
                .roles("ADMIN")
                .build();
            return new InMemoryUserDetailsManager(user);
        }
    }

    @Configuration
    @Order(3)
    class DefaultSec {

        @Bean
        public SecurityFilterChain filterChainAllowAll(HttpSecurity http) throws Exception {
            return http.csrf().disable().requestMatchers().antMatchers("/api/**").and() // NOSONAR
                .authorizeRequests()
                .anyRequest().permitAll()
                .and().build();
        }
    }


}
