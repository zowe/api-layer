/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.service.security.service.gateway.security;

import com.broadcom.apiml.library.service.security.service.security.config.SecurityConfigurationProperties;
import com.broadcom.apiml.library.service.security.service.security.handler.FailedAuthenticationHandler;
import com.broadcom.apiml.library.service.security.service.security.handler.UnauthorizedHandler;
import com.broadcom.apiml.library.service.security.service.security.login.LoginAuthenticationProvider;
import com.broadcom.apiml.library.service.security.service.security.login.LoginFilter;
import com.broadcom.apiml.library.service.security.service.security.login.SuccessfulLoginHandler;
import com.broadcom.apiml.library.service.security.service.security.token.CookieFilter;
import com.broadcom.apiml.library.service.security.service.security.token.TokenAuthenticationProvider;
import com.broadcom.apiml.library.service.security.service.security.token.TokenFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableWebSecurity
@ComponentScan("com.ca.mfaas.security")
@Import(ComponentsConfiguration.class)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    private final ObjectMapper securityObjectMapper;
    private final UnauthorizedHandler unAuthorizedHandler;
    private final SuccessfulLoginHandler successfulLoginHandler;
    private final FailedAuthenticationHandler authenticationFailureHandler;
    private final LoginAuthenticationProvider loginAuthenticationProvider;
    private final TokenAuthenticationProvider tokenAuthenticationProvider;
    private final SecurityConfigurationProperties securityConfigurationProperties;

    public SecurityConfiguration(
        UnauthorizedHandler unAuthorizedHandler,
        SuccessfulLoginHandler successfulLoginHandler,
        FailedAuthenticationHandler authenticationFailureHandler,
        LoginAuthenticationProvider loginAuthenticationProvider,
        TokenAuthenticationProvider tokenAuthenticationProvider,
        ObjectMapper securityObjectMapper,
        SecurityConfigurationProperties securityConfigurationProperties) {
        this.unAuthorizedHandler = unAuthorizedHandler;
        this.successfulLoginHandler = successfulLoginHandler;
        this.authenticationFailureHandler = authenticationFailureHandler;
        this.loginAuthenticationProvider = loginAuthenticationProvider;
        this.tokenAuthenticationProvider = tokenAuthenticationProvider;
        this.securityObjectMapper = securityObjectMapper;
        this.securityConfigurationProperties = securityConfigurationProperties;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) {
        auth.authenticationProvider(loginAuthenticationProvider);
        auth.authenticationProvider(tokenAuthenticationProvider);
    }

    @Override
    public void configure(WebSecurity web) {
        // skip web filters matchers
        String[] noSecurityAntMatchers = {
            "/**",
            "/images/**",
            "/favicon.ico",
        };
        web.ignoring().antMatchers(noSecurityAntMatchers);
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
            .addFilterBefore(loginFilter(securityConfigurationProperties.getLoginPath()), UsernamePasswordAuthenticationFilter.class)
            .authorizeRequests()

            // allow login to pass through filters
            .antMatchers(HttpMethod.POST, securityConfigurationProperties.getLoginPath()).permitAll()

            // filters
            .and()
            .addFilterBefore(cookieTokenFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(headerTokenFilter(), UsernamePasswordAuthenticationFilter.class)
            .authorizeRequests();
    }

    private TokenFilter headerTokenFilter() throws Exception {
        return new TokenFilter(authenticationManager(), authenticationFailureHandler, securityConfigurationProperties);
    }

    private LoginFilter loginFilter(String loginEndpoint) throws Exception {
        return new LoginFilter(loginEndpoint, successfulLoginHandler, authenticationFailureHandler,
            securityObjectMapper, authenticationManager());
    }

    private CookieFilter cookieTokenFilter() throws Exception {
        return new CookieFilter(authenticationManager(), authenticationFailureHandler, securityConfigurationProperties);
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
}
