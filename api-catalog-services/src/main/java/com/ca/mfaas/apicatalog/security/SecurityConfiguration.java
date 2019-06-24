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

import com.ca.apiml.security.config.SecurityConfigurationProperties;
import com.ca.apiml.security.content.BasicContentFilter;
import com.ca.apiml.security.content.CookieContentFilter;
import com.ca.apiml.security.handler.BasicAuthUnauthorizedHandler;
import com.ca.apiml.security.handler.FailedAuthenticationHandler;
import com.ca.apiml.security.handler.UnauthorizedHandler;
import com.ca.apiml.security.login.GatewayLoginProvider;
import com.ca.apiml.security.login.LoginFilter;
import com.ca.apiml.security.login.SuccessfulLoginHandler;
import com.ca.apiml.security.token.GatewayTokenProvider;
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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@ComponentScan("com.ca.apiml.security")
@Import(ComponentsConfiguration.class)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    private final ObjectMapper securityObjectMapper;
    private final SecurityConfigurationProperties securityConfigurationProperties;
    private final SuccessfulLoginHandler successfulLoginHandler;
    private final UnauthorizedHandler unAuthorizedHandler;
    private final BasicAuthUnauthorizedHandler basicAuthUnauthorizedHandler;
    private final FailedAuthenticationHandler authenticationFailureHandler;
    private final GatewayLoginProvider gatewayLoginProvider;
    private final GatewayTokenProvider gatewayTokenProvider;

    public SecurityConfiguration(
        ObjectMapper securityObjectMapper,
        SecurityConfigurationProperties securityConfigurationProperties,
        SuccessfulLoginHandler successfulLoginHandler,
        @Qualifier("plainAuth")
            UnauthorizedHandler unAuthorizedHandler,
        BasicAuthUnauthorizedHandler basicAuthUnauthorizedHandler,
        FailedAuthenticationHandler authenticationFailureHandler,
        GatewayLoginProvider gatewayLoginProvider,
        GatewayTokenProvider gatewayTokenProvider) {
        this.securityObjectMapper = securityObjectMapper;
        this.securityConfigurationProperties = securityConfigurationProperties;
        this.successfulLoginHandler = successfulLoginHandler;
        this.unAuthorizedHandler = unAuthorizedHandler;
        this.basicAuthUnauthorizedHandler = basicAuthUnauthorizedHandler;
        this.authenticationFailureHandler = authenticationFailureHandler;
        this.gatewayLoginProvider = gatewayLoginProvider;
        this.gatewayTokenProvider = gatewayTokenProvider;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) {
        auth.authenticationProvider(gatewayLoginProvider);
        auth.authenticationProvider(gatewayTokenProvider);
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
            .headers()
            .httpStrictTransportSecurity().disable()
            .frameOptions().disable()
            .and()
            .exceptionHandling()

            .defaultAuthenticationEntryPointFor(
                basicAuthUnauthorizedHandler, new AntPathRequestMatcher("/application/**")
            )
            .defaultAuthenticationEntryPointFor(
                basicAuthUnauthorizedHandler, new AntPathRequestMatcher("/apidoc/**")
            )
            .defaultAuthenticationEntryPointFor(
                unAuthorizedHandler, new AntPathRequestMatcher("/**")
            )

            .and()
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)

            // login endpoint
            .and()
            .addFilterBefore(
                loginFilter(securityConfigurationProperties.getServiceLoginPath()),
                UsernamePasswordAuthenticationFilter.class
            )
            .authorizeRequests()
            .antMatchers(HttpMethod.POST, securityConfigurationProperties.getServiceLoginPath()).permitAll()

            // logout endpoint
            .and()
            .logout()
            .logoutUrl(securityConfigurationProperties.getServiceLogoutPath())
            .logoutSuccessHandler(logoutSuccessHandler())

            // endpoints protection
            .and()
            .addFilterBefore(basicFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(cookieFilter(), UsernamePasswordAuthenticationFilter.class)
            .authorizeRequests()
            .antMatchers("/containers/**").authenticated()
            .antMatchers("/apidoc/**").authenticated()
            .antMatchers("/application/health", "/application/info").permitAll()
            .antMatchers("/application/**").authenticated();
    }

    private LoginFilter loginFilter(String loginEndpoint) throws Exception {
        return new LoginFilter(loginEndpoint, successfulLoginHandler, authenticationFailureHandler,
            securityObjectMapper, authenticationManager());
    }

    /**
     * Secures content with a basic authentication
     */
    private BasicContentFilter basicFilter() throws Exception {
        return new BasicContentFilter(authenticationManager(), authenticationFailureHandler);
    }

    /**
     * Secures content with a token stored in a cookie
     */
    private CookieContentFilter cookieFilter() throws Exception {
        return new CookieContentFilter(authenticationManager(), authenticationFailureHandler, securityConfigurationProperties);
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
