/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.discovery.config;

import com.ca.apiml.security.client.EnableApimlAuth;
import com.ca.apiml.security.client.login.GatewayLoginProvider;
import com.ca.apiml.security.client.token.GatewayTokenProvider;
import com.ca.apiml.security.common.config.AuthConfigurationProperties;
import com.ca.apiml.security.common.config.HandlerInitializer;
import com.ca.apiml.security.common.content.BasicContentFilter;
import com.ca.apiml.security.common.content.CookieContentFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Collections;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableApimlAuth
@Order(1)
@Profile("https")
public class HttpsEurekaSecurityConfiguration extends WebSecurityConfigurerAdapter {

    private final HandlerInitializer handlerInitializer;
    private final AuthConfigurationProperties securityConfigurationProperties;
    private final GatewayLoginProvider gatewayLoginProvider;
    private final GatewayTokenProvider gatewayTokenProvider;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) {
        auth.authenticationProvider(gatewayLoginProvider);
        auth.authenticationProvider(gatewayTokenProvider);
    }


    @Override
    public void configure(WebSecurity web) {
        // skip security filters matchers
        String[] noSecurityAntMatchers = {

            "/static/**",
            "/favicon.ico"

        };
        web.ignoring().antMatchers(noSecurityAntMatchers);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
        .headers().httpStrictTransportSecurity().disable()
        .and()
        .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)


        .and()
        .addFilterBefore(basicFilter(), UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(cookieFilter(), UsernamePasswordAuthenticationFilter.class)
        .authorizeRequests()

        .anyRequest().authenticated()

        .and()
        .httpBasic()

            .and()
            .x509()
        .userDetailsService(username -> new User("eurekaClient", "", Collections.emptyList()))
        ;


    }

    /**
     * Secures content with a basic authentication
     */
    private BasicContentFilter basicFilter() throws Exception {
        return new BasicContentFilter(authenticationManager(), handlerInitializer.getAuthenticationFailureHandler(), handlerInitializer.getResourceAccessExceptionHandler());
    }

    /**
     * Secures content with a token stored in a cookie
     */
    private CookieContentFilter cookieFilter() throws Exception {
        return new CookieContentFilter(authenticationManager(), handlerInitializer.getAuthenticationFailureHandler(), handlerInitializer.getResourceAccessExceptionHandler(), securityConfigurationProperties);
    }
}
