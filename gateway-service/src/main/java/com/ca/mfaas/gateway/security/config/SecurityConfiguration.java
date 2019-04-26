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

import com.ca.apiml.security.config.SecurityConfigurationProperties;
import com.ca.apiml.security.content.BasicFilter;
import com.ca.apiml.security.content.CookieFilter;
import com.ca.mfaas.gateway.security.handler.FailedAuthenticationHandler;
import com.ca.mfaas.gateway.security.handler.UnauthorizedHandler;
import com.ca.mfaas.gateway.security.login.LoginFilter;
import com.ca.mfaas.gateway.security.login.SuccessfulLoginHandler;
import com.ca.mfaas.gateway.security.query.QueryFilter;
import com.ca.mfaas.gateway.security.query.SuccessfulQueryHandler;
import com.ca.mfaas.gateway.security.service.AuthenticationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for Gateway
 * <p>
 * 1. Adds Login and Query endpoints
 * 2. Allows basic and token (cookie) authentication
 */
@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {
    private final ObjectMapper securityObjectMapper;
    private final AuthenticationService authenticationService;
    private final SecurityConfigurationProperties securityConfigurationProperties;
    private final SuccessfulLoginHandler successfulLoginHandler;
    private final SuccessfulQueryHandler successfulQueryHandler;
    private final FailedAuthenticationHandler authenticationFailureHandler;
    private final AuthProviderInitializer authProviderInitializer;
    private final UnauthorizedHandler unAuthorizedHandler;

    public SecurityConfiguration(
        ObjectMapper securityObjectMapper,
        AuthenticationService authenticationService,
        SecurityConfigurationProperties securityConfigurationProperties,
        SuccessfulLoginHandler successfulLoginHandler,
        SuccessfulQueryHandler successfulQueryHandler,
        FailedAuthenticationHandler authenticationFailureHandler,
        AuthProviderInitializer authProviderInitializer,
        UnauthorizedHandler unAuthorizedHandler) {
        this.securityObjectMapper = securityObjectMapper;
        this.authenticationService = authenticationService;
        this.securityConfigurationProperties = securityConfigurationProperties;
        this.successfulLoginHandler = successfulLoginHandler;
        this.successfulQueryHandler = successfulQueryHandler;
        this.authenticationFailureHandler = authenticationFailureHandler;
        this.authProviderInitializer = authProviderInitializer;
        this.unAuthorizedHandler = unAuthorizedHandler;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) {
        authProviderInitializer.configure(auth);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .headers().disable()
            .exceptionHandling()
            .authenticationEntryPoint(unAuthorizedHandler)

            .and()
            .headers().httpStrictTransportSecurity().disable()
            .and()
            .httpBasic()

            .and()
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)

            // login endpoint
            .and()
            .authorizeRequests()
            .antMatchers(HttpMethod.POST, securityConfigurationProperties.getLoginPath()).permitAll()

            // endpoint protection
            .and()
            .authorizeRequests()
            .antMatchers("/application/health", "/application/info").permitAll()
            .antMatchers("/application/**").authenticated()

            // add filters - login + query
            .and()
            .addFilterBefore(queryFilter(securityConfigurationProperties.getQueryPath()), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(loginFilter(securityConfigurationProperties.getLoginPath()), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(basicFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(cookieFilter(), UsernamePasswordAuthenticationFilter.class);
    }

    private LoginFilter loginFilter(String loginEndpoint) throws Exception {
        return new LoginFilter(loginEndpoint, successfulLoginHandler, authenticationFailureHandler, securityObjectMapper,
            authenticationManager());
    }

    private QueryFilter queryFilter(String queryEndpoint) throws Exception {
        return new QueryFilter(queryEndpoint, successfulQueryHandler, authenticationFailureHandler, authenticationService,
            authenticationManager());
    }

    private BasicFilter basicFilter() throws Exception {
        return new BasicFilter(authenticationManager(), authenticationFailureHandler);
    }

    private CookieFilter cookieFilter() throws Exception {
        return new CookieFilter(authenticationManager(), authenticationFailureHandler, securityConfigurationProperties);
    }
}
