/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.security;

import com.ca.mfaas.security.config.SecurityConfigurationProperties;
import com.ca.mfaas.security.handler.FailedAuthenticationHandler;
import com.ca.mfaas.security.handler.UnauthorizedHandler;
import com.ca.mfaas.security.login.LoginFilter;
import com.ca.mfaas.security.login.SuccessfulLoginHandler;
import com.ca.mfaas.security.login.ZosmfAuthenticationProvider;
import com.ca.mfaas.security.query.QueryFilter;
import com.ca.mfaas.security.query.SuccessfulQueryHandler;
import com.ca.mfaas.security.token.CookieFilter;
import com.ca.mfaas.security.token.TokenAuthenticationProvider;
import com.ca.mfaas.security.token.TokenFilter;
import com.ca.mfaas.security.token.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

@Configuration
@EnableWebSecurity
@ComponentScan("com.ca.mfaas.security")
@Import(ComponentsConfiguration.class)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {
    private final ObjectMapper securityObjectMapper;
    private final TokenService tokenService;
    private final UnauthorizedHandler unAuthorizedHandler;
    private final SuccessfulLoginHandler successfulLoginHandler;
    private final SuccessfulQueryHandler successfulQueryHandler;
    private final FailedAuthenticationHandler authenticationFailureHandler;
    private final ZosmfAuthenticationProvider loginAuthenticationProvider;
    private final TokenAuthenticationProvider tokenAuthenticationProvider;
    private final SecurityConfigurationProperties securityConfigurationProperties;

    public SecurityConfiguration(
        @Qualifier("securityObjectMapper") ObjectMapper securityObjectMapper,
        UnauthorizedHandler unAuthorizedHandler,
        SuccessfulLoginHandler successfulLoginHandler,
        SuccessfulQueryHandler successfulQueryHandler,
        FailedAuthenticationHandler authenticationFailureHandler,
        ZosmfAuthenticationProvider loginAuthenticationProvider,
        TokenAuthenticationProvider tokenAuthenticationProvider,
        TokenService tokenService,
        SecurityConfigurationProperties securityConfigurationProperties) {
        super();
        this.securityObjectMapper = securityObjectMapper;
        this.unAuthorizedHandler = unAuthorizedHandler;
        this.successfulLoginHandler = successfulLoginHandler;
        this.successfulQueryHandler = successfulQueryHandler;
        this.authenticationFailureHandler = authenticationFailureHandler;
        this.loginAuthenticationProvider = loginAuthenticationProvider;
        this.tokenAuthenticationProvider = tokenAuthenticationProvider;
        this.tokenService = tokenService;
        this.securityConfigurationProperties = securityConfigurationProperties;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) {
        auth.authenticationProvider(loginAuthenticationProvider);
        auth.authenticationProvider(tokenAuthenticationProvider);
    }

    @Override
    public void configure(WebSecurity web) {
        // skip security filters matchers
        String[] noSecurityAntMatchers = {
            "/",
            "/static/**",
            "/favicon.ico",
            "/api-doc"
        };

        web.ignoring().antMatchers(noSecurityAntMatchers);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http

            .csrf().disable()
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
            .antMatchers(HttpMethod.POST, securityConfigurationProperties.getLoginPath()).permitAll()

            // query endpoint
            .and()
            .addFilterBefore(queryFilter(securityConfigurationProperties.getQueryPath()), UsernamePasswordAuthenticationFilter.class)
            .authorizeRequests()

            // logout endpoint
            .and()
            .logout()
            .logoutUrl(securityConfigurationProperties.getLogoutPath())
            .logoutSuccessHandler(logoutSuccessHandler())

            // endpoints protection
            .and()
            .addFilterBefore(cookieTokenFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(headerTokenFilter(), UsernamePasswordAuthenticationFilter.class)
            .authorizeRequests()
            .antMatchers("/containers/**").authenticated()
            .antMatchers("/apidoc/**").authenticated();
    }

    private TokenFilter headerTokenFilter() throws Exception {
        return new TokenFilter(authenticationManager(), authenticationFailureHandler, securityConfigurationProperties);
    }

    private LoginFilter loginFilter(String loginEndpoint) throws Exception {
        return new LoginFilter(loginEndpoint, successfulLoginHandler, authenticationFailureHandler,
            securityObjectMapper, authenticationManager());
    }

    private QueryFilter queryFilter(String queryEndpoint) throws Exception {
        return new QueryFilter(queryEndpoint, successfulQueryHandler, authenticationFailureHandler, tokenService,
            authenticationManager());
    }

    private CookieFilter cookieTokenFilter() throws Exception {
        return new CookieFilter(authenticationManager(), authenticationFailureHandler, securityConfigurationProperties);
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {
        return new ApiCatalogLogoutSuccessHandler(securityConfigurationProperties);
    }
}
