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

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    @ConditionalOnProperty(value = "okta.oauth2.issuer")
    public SecurityFilterChain filterChainOath(HttpSecurity http) throws Exception {
        return http.csrf().disable() // NOSONAR
            .authorizeRequests()
            .antMatchers("/ws/**").authenticated()
            .antMatchers("/**").permitAll()
            .antMatchers("/whoami").authenticated().and().oauth2ResourceServer().jwt().and()

            .and().build();
    }

    @Bean
    @ConditionalOnMissingBean(value = SecurityFilterChain.class)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http.csrf().disable() // NOSONAR
            .authorizeRequests()
            .antMatchers("/ws/**").authenticated()
            .antMatchers("/**").permitAll()
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

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web ->
            web.ignoring().antMatchers("/api/**");
    }
}
