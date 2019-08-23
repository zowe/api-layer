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
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Collections;

/**
 * Main class configuring Spring security for Discovery Service
 *
 * This configuration is applied if "https" Spring profile is active
 */
@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
@EnableApimlAuth
@Slf4j
@Profile("https")
public class WebSecurityConfigHttps {

    private final HandlerInitializer handlerInitializer;
    private final AuthConfigurationProperties securityConfigurationProperties;
    private final GatewayLoginProvider gatewayLoginProvider;
    private final GatewayTokenProvider gatewayTokenProvider;
    private static final String DISCOVERY_REALM = "API Mediation Discovery Service realm";

    /**
     * Filter chain for protecting endpoints with MF credentials (basic or token)
     */
    @Configuration
    @Order(1)
    public class HttpOne extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(AuthenticationManagerBuilder auth) {
            auth.authenticationProvider(gatewayLoginProvider);
            auth.authenticationProvider(gatewayTokenProvider);
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.requestMatchers().antMatchers(
                "/application/**",
                "/*",
                "/eureka/css/**",
                "/eureka/js/**",
                "/eureka/fonts/**",
                "/eureka/images/**")
                .and()
                .csrf().disable()
                .headers().httpStrictTransportSecurity().disable()
                .and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .addFilterBefore(basicFilter(authenticationManager()), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(cookieFilter(authenticationManager()), UsernamePasswordAuthenticationFilter.class)
                .authorizeRequests()
                    .antMatchers("/application/health", "/application/info").permitAll()
                    .antMatchers("/**").authenticated()
                .and()
                .httpBasic().realmName(DISCOVERY_REALM);
        }
    }

    /**
     * Filter chain for protecting endpoints with x509 certificate
     */
    @Configuration
    @Order(2)
    public class HttpTwo extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.antMatcher("/eureka/**")
                .csrf().disable()
                .headers().httpStrictTransportSecurity().disable()
                .and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests().anyRequest().authenticated()
                .and().x509().userDetailsService(x509UserDetailsService());
        }
    }

    /**
     * Filter chain for protecting endpoints with MF credentials (basic or token) or x509 certificate
     */
    @Configuration
    @Order(3)
    public class HttpThree extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure(AuthenticationManagerBuilder auth) {
            auth.authenticationProvider(gatewayLoginProvider);
            auth.authenticationProvider(gatewayTokenProvider);
        }
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.antMatcher("/discovery/**")
                .csrf().disable()
                .headers().httpStrictTransportSecurity().disable()
                .and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .addFilterBefore(basicFilter(authenticationManager()), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(cookieFilter(authenticationManager()), UsernamePasswordAuthenticationFilter.class)
                .httpBasic().realmName(DISCOVERY_REALM)
                .and()
                .authorizeRequests().anyRequest().authenticated()
                .and().x509().userDetailsService(x509UserDetailsService());
        }
    }

    private BasicContentFilter basicFilter(AuthenticationManager authenticationManager) {
        return new BasicContentFilter(authenticationManager, handlerInitializer.getAuthenticationFailureHandler(), handlerInitializer.getResourceAccessExceptionHandler());
    }
    private CookieContentFilter cookieFilter(AuthenticationManager authenticationManager) {
        return new CookieContentFilter(authenticationManager, handlerInitializer.getAuthenticationFailureHandler(), handlerInitializer.getResourceAccessExceptionHandler(), securityConfigurationProperties);
    }
    private UserDetailsService x509UserDetailsService() {
        return username -> new User("eurekaClient", "", Collections.emptyList());
    }
}
