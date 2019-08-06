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

import com.ca.apiml.security.config.AuthConfigurationProperties;
import com.ca.apiml.security.config.HandlerInitializer;
import com.ca.apiml.security.content.BasicContentFilter;
import com.ca.apiml.security.content.CookieContentFilter;
import com.ca.apiml.security.login.LoginFilter;
import com.ca.mfaas.gateway.security.query.QueryFilter;
import com.ca.mfaas.gateway.security.query.SuccessfulQueryHandler;
import com.ca.mfaas.gateway.security.service.AuthenticationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {
    // List of endpoints protected by content filters
    private static final String[] PROTECTED_ENDPOINTS = {
        "/api/v1/gateway",
        "/application"
    };

    private final ObjectMapper securityObjectMapper;
    private final AuthenticationService authenticationService;
    private final AuthConfigurationProperties authConfigurationProperties;
    private final HandlerInitializer handlerInitializer;
    private final SuccessfulQueryHandler successfulQueryHandler;
    private final AuthProviderInitializer authProviderInitializer;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) {
        authProviderInitializer.configure(auth);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .headers()
            .httpStrictTransportSecurity().disable()
            .frameOptions().disable()
            .and()
            .exceptionHandling().authenticationEntryPoint(handlerInitializer.getBasicAuthUnauthorizedHandler())

            .and()
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)

            // login endpoint
            .and()
            .authorizeRequests()
            .antMatchers(HttpMethod.POST, authConfigurationProperties.getGatewayLoginEndpoint()).permitAll()

            // endpoint protection
            .and()
            .authorizeRequests()
            .antMatchers("/application/health", "/application/info").permitAll()
            .antMatchers("/application/**").authenticated()

            // add filters - login + query
            .and()
            .addFilterBefore(loginFilter(authConfigurationProperties.getGatewayLoginEndpoint()), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(queryFilter(authConfigurationProperties.getGatewayQueryEndpoint()), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(basicFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(cookieFilter(), UsernamePasswordAuthenticationFilter.class);
    }

    /**
     * Processes /login requests
     */
    private LoginFilter loginFilter(String loginEndpoint) throws Exception {
        return new LoginFilter(
            loginEndpoint,
            handlerInitializer.getSuccessfulLoginHandler(),
            handlerInitializer.getAuthenticationFailureHandler(),
            securityObjectMapper,
            authenticationManager(),
            handlerInitializer.getResourceAccessExceptionHandler());
    }

    /**
     * Processes /query requests
     */
    private QueryFilter queryFilter(String queryEndpoint) throws Exception {
        return new QueryFilter(
            queryEndpoint,
            successfulQueryHandler,
            handlerInitializer.getAuthenticationFailureHandler(),
            authenticationService,
            authenticationManager());
    }

    /**
     * Secures content with a basic authentication
     */
    private BasicContentFilter basicFilter() throws Exception {
        return new BasicContentFilter(
            authenticationManager(),
            handlerInitializer.getAuthenticationFailureHandler(),
            handlerInitializer.getResourceAccessExceptionHandler(),
            PROTECTED_ENDPOINTS);
    }

    /**
     * Secures content with a token stored in a cookie
     */
    private CookieContentFilter cookieFilter() throws Exception {
        return new CookieContentFilter(
            authenticationManager(),
            handlerInitializer.getAuthenticationFailureHandler(),
            handlerInitializer.getResourceAccessExceptionHandler(),
            authConfigurationProperties,
            PROTECTED_ENDPOINTS);
    }
}
