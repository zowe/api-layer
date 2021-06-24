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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
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

    // TODO gateway assumes this, not configurable
    private String applicationContextPath = "/gateway";

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
    @RequiredArgsConstructor
    @Order(1)
    class authenticationFunctionality extends WebSecurityConfigurerAdapter {

        private final CompoundAuthProvider compoundAuthProvider;

        @Override
        protected void configure(AuthenticationManagerBuilder auth) {
            //TODO eliminate AuthProviderInitializer?
            auth.authenticationProvider(compoundAuthProvider); // for authenticating credentials
            auth.authenticationProvider(new CertificateAuthenticationProvider()); // this is a dummy auth provider so the x509 prefiltering doesn't fail with nullpointer (no auth provider) or No AuthenticationProvider found for org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
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

                .x509()
                .x509AuthenticationFilter(apimlX509Filter(authenticationManager())) //this filter selects certificates to use for authentication and pushes to custom attribute
                .subjectPrincipalRegex(EXTRACT_USER_PRINCIPAL_FROM_COMMON_NAME)
                .userDetailsService(new SimpleUserDetailService())

                .and()
                .logout().disable()

                //drive filter order this way
                .addFilterBefore(loginFilter("/**", authenticationManager()), org.springframework.security.web.authentication.preauth.x509.X509AuthenticationFilter.class)
                .addFilterAfter(x509AuthenticationFilter("/**"), org.springframework.security.web.authentication.preauth.x509.X509AuthenticationFilter.class) // this filter consumes certificates from custom attribute and maps them to credentials and authenticates them
                .addFilterAfter(new ShouldBeAlreadyAuthenticatedFilter("/**", handlerInitializer.getAuthenticationFailureHandler()), org.springframework.security.web.authentication.preauth.x509.X509AuthenticationFilter.class); // this filter stops processing of filter chaing because there is nothing on /auth/login endpoint

        }



    }

    @Configuration
    @Order(100)
    class defaultSecurity extends WebSecurityConfigurerAdapter {

        // Only once here is correct, putting it to other filter chains causes multiple evaluations
        @Override
        public void configure(WebSecurity web) throws Exception {
            configureWebSecurity(web);
        }

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

    //Web security only needs to be configured once
    public void configureWebSecurity(WebSecurity web) {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowUrlEncodedSlash(true);
        firewall.setAllowBackSlash(true);
        firewall.setAllowUrlEncodedPercent(true);
        firewall.setAllowUrlEncodedPeriod(true);
        firewall.setAllowSemicolon(true);
        web.httpFirewall(firewall);

        // This is to allow for internal error controller through
        web.ignoring()
            .antMatchers(InternalServerErrorController.ERROR_ENDPOINT,
                "/application/health", "/application/info", applicationContextPath + "/version");
    }

    private LoginFilter loginFilter(String loginEndpoint, AuthenticationManager authenticationManager) throws Exception {
        return new LoginFilter(
            loginEndpoint,
            handlerInitializer.getSuccessfulLoginHandler(),
            handlerInitializer.getAuthenticationFailureHandler(),
            securityObjectMapper,
            authenticationManager,
            handlerInitializer.getResourceAccessExceptionHandler());
    }

    private ApimlX509Filter apimlX509Filter(AuthenticationManager authenticationManager) throws Exception {
        ApimlX509Filter out = new ApimlX509Filter(publicKeyCertificatesBase64);
        out.setAuthenticationManager(authenticationManager);
        return out;
    }

    private X509AuthenticationFilter x509AuthenticationFilter(String loginEndpoint) {
        return new X509AuthenticationFilter(loginEndpoint,
            handlerInitializer.getSuccessfulLoginHandler(),
            x509AuthenticationProvider);
    }

}
