/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.zowe.apiml.gateway.error.InternalServerErrorController;
import org.zowe.apiml.gateway.security.login.x509.X509AuthenticationProvider;
import org.zowe.apiml.gateway.security.query.SuccessfulQueryHandler;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.security.ticket.SuccessfulTicketHandler;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.config.HandlerInitializer;
import org.zowe.apiml.security.common.login.LoginFilter;
import org.zowe.apiml.security.common.login.ShouldBeAlreadyAuthenticatedFilter;

import java.util.*;

@Profile("newSecurity")
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class NewSecurityConfiguration {

    // List of endpoints protected by content filters
    private static final String[] PROTECTED_ENDPOINTS = {
        "/gateway/api/v1",
        "/api/v1/gateway",
        "/application",
        "/gateway/services"
    };

    private static final List<String> CORS_ENABLED_ENDPOINTS = Arrays.asList("/api/v1/gateway/**", "/gateway/version");

    @Value("${apiml.service.corsEnabled:false}")
    private boolean corsEnabled;

    @Value("${apiml.service.ignoredHeadersWhenCorsEnabled}")
    private String ignoredHeadersWhenCorsEnabled;

    private static final String EXTRACT_USER_PRINCIPAL_FROM_COMMON_NAME = "CN=(.*?)(?:,|$)";

    private final ObjectMapper securityObjectMapper;
    private final AuthenticationService authenticationService;
    private final AuthConfigurationProperties authConfigurationProperties;
    private final HandlerInitializer handlerInitializer;
    private final SuccessfulQueryHandler successfulQueryHandler;
    private final SuccessfulTicketHandler successfulTicketHandler;
    private final AuthProviderInitializer authProviderInitializer;
    @Qualifier("publicKeyCertificatesBase64")
    private final Set<String> publicKeyCertificatesBase64;
    private final ZuulProperties zuulProperties;
    private final X509AuthenticationProvider x509AuthenticationProvider;
    @Value("${server.attls.enabled:false}")
    private boolean isAttlsEnabled;


    @Configuration
    @Order(1)
    class authenticationFunctionality extends WebSecurityConfigurerAdapter {

        @Override
        public void configure(WebSecurity web) throws Exception {
            configureWebSecurity(web);
        }

        @Override
        protected void configure(AuthenticationManagerBuilder auth) {
            //authProviderInitializer.configure(auth);
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            baseConfigure(http.requestMatchers().antMatchers(
                    authConfigurationProperties.getGatewayLoginEndpoint(),
                    authConfigurationProperties.getGatewayLoginEndpointOldFormat()
                ).and())
                .authorizeRequests()
                .anyRequest().permitAll()
                .and()
                .logout().disable() //TODO see if this doesn't screw up the other filterchain

                .addFilterBefore(new ShouldBeAlreadyAuthenticatedFilter("/**", handlerInitializer.getAuthenticationFailureHandler()), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(x509AuthenticationFilter("/**"), ShouldBeAlreadyAuthenticatedFilter.class)
                .addFilterBefore(loginFilter("/**"), X509AuthenticationFilter.class);
        }

        private LoginFilter loginFilter(String loginEndpoint) throws Exception {
            return new LoginFilter(
                loginEndpoint,
                handlerInitializer.getSuccessfulLoginHandler(),
                handlerInitializer.getAuthenticationFailureHandler(),
                securityObjectMapper,
                authenticationManager(),
                handlerInitializer.getResourceAccessExceptionHandler());
        }

        // TODO refactor this filter to not rely on prefiltering by another filter
        private X509AuthenticationFilter x509AuthenticationFilter(String loginEndpoint) {
            return new X509AuthenticationFilter(loginEndpoint,
                handlerInitializer.getSuccessfulLoginHandler(),
                x509AuthenticationProvider);
        }
    }

    @Configuration
    @Order(100)
    class defaultSecurity extends WebSecurityConfigurerAdapter {

        @Override
        public void configure(WebSecurity web) throws Exception {
            configureWebSecurity(web);
        }
//        @Override
//        protected void configure(AuthenticationManagerBuilder auth) {
//            authProviderInitializer.configure(auth);
//        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            baseConfigure(http.requestMatchers().antMatchers("/**").and())
                .authorizeRequests()
                .anyRequest().denyAll();
        }
    }

    protected HttpSecurity baseConfigure(HttpSecurity http) throws Exception {
        return http
            .cors()
            .and().csrf().disable()    // NOSONAR
            .headers().httpStrictTransportSecurity().disable()
            .frameOptions().disable()
            .and().exceptionHandling().authenticationEntryPoint(handlerInitializer.getBasicAuthUnauthorizedHandler())
            .and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and();
    }

    public void configureWebSecurity(WebSecurity web) {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowUrlEncodedSlash(true);
        firewall.setAllowBackSlash(true);
        firewall.setAllowUrlEncodedPercent(true);
        firewall.setAllowUrlEncodedPeriod(true);
        firewall.setAllowSemicolon(true);
        web.httpFirewall(firewall);

//        web.ignoring()
//            .antMatchers(AuthController.CONTROLLER_PATH + AuthController.PUBLIC_KEYS_PATH + "/**");
        web.ignoring()
            .antMatchers(InternalServerErrorController.ERROR_ENDPOINT);
    }

}
