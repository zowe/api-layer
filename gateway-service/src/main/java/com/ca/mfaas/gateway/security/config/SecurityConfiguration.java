/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.security.config;

import com.ca.mfaas.gateway.security.handler.FailedAuthenticationHandler;
import com.ca.mfaas.gateway.security.handler.UnauthorizedHandler;
import com.ca.mfaas.gateway.security.login.LoginFilter;
import com.ca.mfaas.gateway.security.login.SuccessfulLoginHandler;
import com.ca.mfaas.gateway.security.login.ZosmfAuthenticationProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    private final ObjectMapper securityObjectMapper;
    private final UnauthorizedHandler unAuthorizedHandler;
    private final SuccessfulLoginHandler successfulLoginHandler;
    private final FailedAuthenticationHandler authenticationFailureHandler;
    private final ZosmfAuthenticationProvider loginAuthenticationProvider;
    private final SecurityConfigurationProperties securityConfigurationProperties;

    public SecurityConfiguration(
        UnauthorizedHandler unAuthorizedHandler,
        SuccessfulLoginHandler successfulLoginHandler,
        FailedAuthenticationHandler authenticationFailureHandler,
        ZosmfAuthenticationProvider loginAuthenticationProvider,
        ObjectMapper securityObjectMapper,
        SecurityConfigurationProperties securityConfigurationProperties) {
        this.unAuthorizedHandler = unAuthorizedHandler;
        this.successfulLoginHandler = successfulLoginHandler;
        this.authenticationFailureHandler = authenticationFailureHandler;
        this.loginAuthenticationProvider = loginAuthenticationProvider;
        this.securityObjectMapper = securityObjectMapper;
        this.securityConfigurationProperties = securityConfigurationProperties;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) {
        auth.authenticationProvider(loginAuthenticationProvider);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .httpBasic().disable()
            .headers().disable()
            .exceptionHandling().authenticationEntryPoint(unAuthorizedHandler)

            .and()
            .headers().httpStrictTransportSecurity().disable()

            .and()
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)

            // login endpoint
            .and()
            .addFilterBefore(loginFilter(securityConfigurationProperties.getLoginPath()), UsernamePasswordAuthenticationFilter.class);
    }

    private LoginFilter loginFilter(String loginEndpoint) throws Exception {
        return new LoginFilter(loginEndpoint, successfulLoginHandler, authenticationFailureHandler,
            securityObjectMapper, authenticationManager());
    }
}
