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

import com.ca.apiml.security.config.AuthConfigurationProperties;
import com.ca.apiml.security.content.BasicContentFilter;
import com.ca.apiml.security.content.CookieContentFilter;
import com.ca.apiml.security.login.GatewayLoginProvider;
import com.ca.apiml.security.login.LoginFilter;
import com.ca.apiml.security.token.GatewayTokenProvider;
import com.ca.apiml.security.config.HandlerInitializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Main configuration class of Spring web security for Api Catalog
 * binds authentication managers
 * configures ignores for static content
 * adds endpoints and secures them
 * adds security filters
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@ComponentScan("com.ca.apiml.security")
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    private final ObjectMapper securityObjectMapper;
    private final AuthConfigurationProperties authConfigurationProperties;
    private final HandlerInitializer handlerInitializer;
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
                handlerInitializer.getBasicAuthUnauthorizedHandler(), new AntPathRequestMatcher("/application/**")
            )
            .defaultAuthenticationEntryPointFor(
                handlerInitializer.getBasicAuthUnauthorizedHandler(), new AntPathRequestMatcher("/apidoc/**")
            )
            .defaultAuthenticationEntryPointFor(
                handlerInitializer.getUnAuthorizedHandler(), new AntPathRequestMatcher("/**")
            )

            .and()
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)

            // login endpoint
            .and()
            .addFilterBefore(
                loginFilter(authConfigurationProperties.getServiceLoginPath()),
                UsernamePasswordAuthenticationFilter.class
            )
            .authorizeRequests()
            .antMatchers(HttpMethod.POST, authConfigurationProperties.getServiceLoginPath()).permitAll()

            // logout endpoint
            .and()
            .logout()
            .logoutUrl(authConfigurationProperties.getServiceLogoutPath())
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
        return new LoginFilter(loginEndpoint, handlerInitializer.getSuccessfulLoginHandler(), handlerInitializer.getAuthenticationFailureHandler(),
            securityObjectMapper, authenticationManager(), handlerInitializer.getResourceAccessExceptionHandler());
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
        return new CookieContentFilter(authenticationManager(), handlerInitializer.getAuthenticationFailureHandler(), handlerInitializer.getResourceAccessExceptionHandler(), authConfigurationProperties);
    }

    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {
        return new ApiCatalogLogoutSuccessHandler(authConfigurationProperties);
    }
}
