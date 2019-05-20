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
import com.ca.apiml.security.handler.FailedAuthenticationHandler;
import com.ca.apiml.security.handler.UnauthorizedHandler;
import com.ca.apiml.security.login.GatewayLoginProvider;
import com.ca.apiml.security.login.LoginFilter;
import com.ca.apiml.security.login.SuccessfulLoginHandler;
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
@ComponentScan("com.ca.apiml.security")
@Import(ComponentsConfiguration.class)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    /*  private final TokenService tokenService;

      private final SuccessfulLoginHandler successfulLoginHandler;
      private final SuccessfulQueryHandler successfulQueryHandler;

      private final TokenAuthenticationProvider tokenAuthenticationProvider;*/

    private final ObjectMapper securityObjectMapper;
    private final SecurityConfigurationProperties securityConfigurationProperties;
    private final SuccessfulLoginHandler successfulLoginHandler;
    private final UnauthorizedHandler unAuthorizedHandler;
    private final FailedAuthenticationHandler authenticationFailureHandler;
    private final GatewayLoginProvider gatewayLoginProvider;

    public SecurityConfiguration(
        ObjectMapper securityObjectMapper,
        SecurityConfigurationProperties securityConfigurationProperties,
        SuccessfulLoginHandler successfulLoginHandler,
        UnauthorizedHandler unAuthorizedHandler,
        FailedAuthenticationHandler authenticationFailureHandler,
        GatewayLoginProvider gatewayLoginProvider) {
        this.securityObjectMapper = securityObjectMapper;
        this.securityConfigurationProperties = securityConfigurationProperties;
        this.successfulLoginHandler = successfulLoginHandler;
        this.unAuthorizedHandler = unAuthorizedHandler;
        this.authenticationFailureHandler = authenticationFailureHandler;
        this.gatewayLoginProvider =  gatewayLoginProvider;
    }

  /*
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
   */

    @Override
    protected void configure(AuthenticationManagerBuilder auth) {
            auth.authenticationProvider(gatewayLoginProvider);
        //   auth.authenticationProvider(tokenAuthenticationProvider);
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
            .exceptionHandling().authenticationEntryPoint(unAuthorizedHandler)

            .and()
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)

            // login endpoint
            .and()
            .addFilterBefore(loginFilter(securityConfigurationProperties.getLoginPath()), UsernamePasswordAuthenticationFilter.class)
            .authorizeRequests()
            .antMatchers(HttpMethod.POST, securityConfigurationProperties.getLoginPath()).permitAll()

            // logout endpoint
            .and()
            .logout()
            .logoutUrl(securityConfigurationProperties.getLogoutPath())
            .logoutSuccessHandler(logoutSuccessHandler())

            // endpoints protection
            .and()
            //        .addFilterBefore(cookieTokenFilter(), UsernamePasswordAuthenticationFilter.class)
            //       .addFilterAfter(headerTokenFilter(), UsernamePasswordAuthenticationFilter.class)
            .authorizeRequests()
            .antMatchers("/containers/**").authenticated()
            .antMatchers("/apidoc/**").authenticated();
    }

    /*
    private TokenFilter headerTokenFilter() throws Exception {
        return new TokenFilter(authenticationManager(), authenticationFailureHandler, securityConfigurationProperties);
    }*/

    private LoginFilter loginFilter(String loginEndpoint) throws Exception {
        return new LoginFilter(loginEndpoint, successfulLoginHandler, authenticationFailureHandler,
            securityObjectMapper, authenticationManager());
    }

    /*
    private QueryFilter queryFilter(String queryEndpoint) throws Exception {
        return new QueryFilter(queryEndpoint, successfulQueryHandler, authenticationFailureHandler, tokenService,
            authenticationManager());
    }

    private CookieFilter cookieTokenFilter() throws Exception {
        return new CookieFilter(authenticationManager(), authenticationFailureHandler, securityConfigurationProperties);
    }
     */

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
