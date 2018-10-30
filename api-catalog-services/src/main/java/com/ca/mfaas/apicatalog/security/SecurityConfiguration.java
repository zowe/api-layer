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

import com.ca.mfaas.product.config.MFaaSConfigPropertiesContainer;
import com.ca.mfaas.security.handler.FailedAuthenticationHandler;
import com.ca.mfaas.security.handler.UnauthorizedHandler;
import com.ca.mfaas.security.login.LoginAuthenticationProvider;
import com.ca.mfaas.security.login.LoginFilter;
import com.ca.mfaas.security.login.SuccessfulLoginHandler;
import com.ca.mfaas.security.token.CookieConfiguration;
import com.ca.mfaas.security.token.CookieFilter;
import com.ca.mfaas.security.token.HeaderTokenFilter;
import com.ca.mfaas.security.token.TokenAuthenticationProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity()
@ComponentScan("com.ca.mfaas.security")
@Import(ComponentsConfiguration.class)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {
    private static final String LOGIN_ENDPOINT = "/auth/login";
    private static final String LOGOUT_ENDPOINT = "/auth/logout";

    private final ObjectMapper mapper;
    private final UnauthorizedHandler unAuthorizedHandler;
    private final SuccessfulLoginHandler successfulLoginHandler;
    private final FailedAuthenticationHandler authenticationFailureHandler;
    private final LoginAuthenticationProvider loginAuthenticationProvider;
    private final TokenAuthenticationProvider tokenAuthenticationProvider;
    private final CookieConfiguration cookieConfiguration;

    public SecurityConfiguration(
        @Qualifier("securityObjectMapper") ObjectMapper mapper,
        UnauthorizedHandler unAuthorizedHandler,
        SuccessfulLoginHandler successfulLoginHandler,
        FailedAuthenticationHandler authenticationFailureHandler,
        LoginAuthenticationProvider loginAuthenticationProvider,
        TokenAuthenticationProvider tokenAuthenticationProvider,
        CookieConfiguration cookieConfiguration) {
        super();
        this.mapper = mapper;
        this.unAuthorizedHandler = unAuthorizedHandler;
        this.successfulLoginHandler = successfulLoginHandler;
        this.authenticationFailureHandler = authenticationFailureHandler;
        this.loginAuthenticationProvider = loginAuthenticationProvider;
        this.tokenAuthenticationProvider = tokenAuthenticationProvider;
        this.cookieConfiguration = cookieConfiguration;
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
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)

            // login endpoint
            .and()
            .addFilterBefore(loginFilter(LOGIN_ENDPOINT), UsernamePasswordAuthenticationFilter.class)
            .authorizeRequests()
            .antMatchers(HttpMethod.POST, LOGIN_ENDPOINT).permitAll()

            // logout endpoint
            .and()
            .logout()
            .logoutUrl(LOGOUT_ENDPOINT)
            .logoutSuccessHandler(logoutSuccessHandler())

            // endpoints protection
            .and()
            .addFilterBefore(cookieTokenFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(headerTokenFilter(), UsernamePasswordAuthenticationFilter.class)
            .authorizeRequests()
            .antMatchers("/containers/**").authenticated()
            .antMatchers("/apidoc/**").authenticated();
    }

    public HeaderTokenFilter headerTokenFilter() throws Exception {
        return new HeaderTokenFilter(authenticationManager(), authenticationFailureHandler);
    }

    public LoginFilter loginFilter(String loginEndpoint) throws Exception {
        return new LoginFilter(loginEndpoint, successfulLoginHandler, authenticationFailureHandler, mapper, authenticationManager());
    }

    public CookieFilter cookieTokenFilter() throws Exception {
        return new CookieFilter(authenticationManager(), authenticationFailureHandler, cookieConfiguration);
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {
        return new ApiCatalogLogoutSuccessHandler(cookieConfiguration);
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    CorsConfigurationSource corsConfigurationSource(MFaaSConfigPropertiesContainer propertiesContainer) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(ImmutableList.of(propertiesContainer.getGateway().getGatewayHostname(), "http://localhost:3000", "http://localhost:10014"));
        configuration.setAllowedMethods(ImmutableList.of("HEAD", "GET", "POST"));
        configuration.setAllowedHeaders(ImmutableList.of("Cookie", "Authorization", "Cache-Control", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
