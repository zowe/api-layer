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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.zowe.apiml.gateway.controllers.AuthController;
import org.zowe.apiml.gateway.controllers.CacheServiceController;
import org.zowe.apiml.gateway.error.InternalServerErrorController;
import org.zowe.apiml.gateway.security.login.x509.X509AuthenticationProvider;
import org.zowe.apiml.gateway.security.query.QueryFilter;
import org.zowe.apiml.gateway.security.query.SuccessfulQueryHandler;
import org.zowe.apiml.gateway.security.query.TokenAuthenticationProvider;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.security.ticket.SuccessfulTicketHandler;
import org.zowe.apiml.gateway.services.ServicesInfoController;
import org.zowe.apiml.product.filter.AttlsFilter;
import org.zowe.apiml.product.filter.SecureConnectionFilter;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.config.HandlerInitializer;
import org.zowe.apiml.security.common.content.BasicContentFilter;
import org.zowe.apiml.security.common.content.CookieContentFilter;
import org.zowe.apiml.security.common.handler.FailedAuthenticationHandler;
import org.zowe.apiml.security.common.login.LoginFilter;
import org.zowe.apiml.security.common.login.ShouldBeAlreadyAuthenticatedFilter;

import java.util.Set;

/**
 * Main configuration place for Gateway endpoint security
 * <p>
 * Security is configured with separate filterchains per groups of endpoints
 * The main theme is to keep the filterchains independent and isolated
 * Gives more control over the behavior of individual filter chain
 * This makes the security filters simpler as they don't have to be smart
 * Also separates the x509 filters per filterchain for more control
 * <p>
 * Authentication providers are initialized per filterchain's needs. No unused auth providers on chains.
 */

@ConditionalOnProperty(name = "apiml.security.filterChainConfiguration", havingValue = "new", matchIfMissing = false)
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class NewSecurityConfiguration {

    private String applicationContextPath = "/gateway"; //NOSONAR this is hardcoded as there is no value for this in config

    private static final String EXTRACT_USER_PRINCIPAL_FROM_COMMON_NAME = "CN=(.*?)(?:,|$)";

    private final ObjectMapper securityObjectMapper;
    private final AuthenticationService authenticationService;
    private final AuthConfigurationProperties authConfigurationProperties;
    private final HandlerInitializer handlerInitializer;
    private final SuccessfulQueryHandler successfulQueryHandler;
    private final SuccessfulTicketHandler successfulTicketHandler;
    @Qualifier("publicKeyCertificatesBase64")
    private final Set<String> publicKeyCertificatesBase64;
    private final X509AuthenticationProvider x509AuthenticationProvider;
    @Value("${server.attls.enabled:false}")
    private boolean isAttlsEnabled;

    /**
     * Login and Logout endpoints
     * <p>
     * logout filter matches for logout request and handles logout
     * apimlX509filter sifts through certs for authentication
     * login filter authenticates credentials
     * x509AuthenticationFilter authenticates certificate from apimlX509Filter
     * shouldAlreadyBeAuthenticated stops processing of request and ends the chain
     */
    @Configuration
    @RequiredArgsConstructor
    @Order(1)
    class AuthenticationFunctionality extends WebSecurityConfigurerAdapter {

        private final CompoundAuthProvider compoundAuthProvider;

        @Override
        protected void configure(AuthenticationManagerBuilder auth) {
            auth.authenticationProvider(compoundAuthProvider); // for authenticating credentials
            auth.authenticationProvider(new CertificateAuthenticationProvider()); // this is a dummy auth provider so the x509 prefiltering doesn't fail with nullpointer (no auth provider) or No AuthenticationProvider found for org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            baseConfigure(http.requestMatchers().antMatchers( // no http method to catch all attempts to login and handle them here. Otherwise it falls to default filterchain and tries to route the calls, which doesnt make sense
                authConfigurationProperties.getGatewayLoginEndpoint(),
                authConfigurationProperties.getGatewayLoginEndpointOldFormat(),
                authConfigurationProperties.getGatewayLogoutEndpoint(),
                authConfigurationProperties.getGatewayLogoutEndpointOldFormat()
            ).and())
                .authorizeRequests()
                .anyRequest().permitAll()
                .and()

                .x509()
                .x509AuthenticationFilter(apimlX509Filter(authenticationManager())) //this filter selects certificates to use for user authentication and pushes them to custom attribute
                .subjectPrincipalRegex(EXTRACT_USER_PRINCIPAL_FROM_COMMON_NAME)
                .userDetailsService(new SimpleUserDetailService())

                .and()
                .logout()
                .logoutRequestMatcher(new RegexRequestMatcher(
                    String.format("(%s|%s)",
                        authConfigurationProperties.getGatewayLogoutEndpoint(),
                        authConfigurationProperties.getGatewayLogoutEndpointOldFormat())
                    , HttpMethod.POST.name()))
                .addLogoutHandler(logoutHandler())
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
                .and()

                //drive filter order this way
                .addFilterBefore(loginFilter("/**", authenticationManager()), org.springframework.security.web.authentication.preauth.x509.X509AuthenticationFilter.class)
                .addFilterAfter(x509AuthenticationFilter("/**"), org.springframework.security.web.authentication.preauth.x509.X509AuthenticationFilter.class) // this filter consumes certificates from custom attribute and maps them to credentials and authenticates them
                .addFilterAfter(new ShouldBeAlreadyAuthenticatedFilter("/**", handlerInitializer.getAuthenticationFailureHandler()), org.springframework.security.web.authentication.preauth.x509.X509AuthenticationFilter.class); // this filter stops processing of filter chaing because there is nothing on /auth/login endpoint

        }

        private LoginFilter loginFilter(String loginEndpoint, AuthenticationManager authenticationManager) {
            return new LoginFilter(
                loginEndpoint,
                handlerInitializer.getSuccessfulLoginHandler(),
                handlerInitializer.getAuthenticationFailureHandler(),
                securityObjectMapper,
                authenticationManager,
                handlerInitializer.getResourceAccessExceptionHandler());
        }

        private ApimlX509Filter apimlX509Filter(AuthenticationManager authenticationManager) {
            ApimlX509Filter out = new ApimlX509Filter(publicKeyCertificatesBase64);
            out.setAuthenticationManager(authenticationManager);
            return out;
        }

        private X509AuthenticationFilter x509AuthenticationFilter(String loginEndpoint) {
            return new X509AuthenticationFilter(loginEndpoint,
                handlerInitializer.getSuccessfulLoginHandler(),
                x509AuthenticationProvider);
        }

        private LogoutHandler logoutHandler() {
            FailedAuthenticationHandler failure = handlerInitializer.getAuthenticationFailureHandler();
            return new JWTLogoutHandler(authenticationService, failure);
        }
    }

    /**
     * Query and Ticket endpoints share single filter that handles auth with and without certificate. This logic is encapsulated in the queryFilter or ticketFilter.
     * Query endpoint does not require certificate to be present in RequestContext. It verifies JWT token.
     */
    @Configuration
    @RequiredArgsConstructor
    @Order(2)
    class Query extends WebSecurityConfigurerAdapter {

        private final TokenAuthenticationProvider tokenAuthenticationProvider;

        @Override
        protected void configure(AuthenticationManagerBuilder auth) {
            auth.authenticationProvider(tokenAuthenticationProvider);
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            baseConfigure(http.requestMatchers().antMatchers(
                authConfigurationProperties.getGatewayQueryEndpoint(),
                authConfigurationProperties.getGatewayQueryEndpointOldFormat()
            ).and()).authorizeRequests()
                .anyRequest().authenticated()
                .and()
                .logout().disable() // logout filter in this chain not needed
                .addFilterBefore(queryFilter("/**", authenticationManager()), UsernamePasswordAuthenticationFilter.class);
        }

        private QueryFilter queryFilter(String queryEndpoint, AuthenticationManager authenticationManager) {
            return new QueryFilter(
                queryEndpoint,
                successfulQueryHandler,
                handlerInitializer.getAuthenticationFailureHandler(),
                authenticationService,
                HttpMethod.GET,
                false,
                authenticationManager);
        }
    }

    /**
     * Query and Ticket endpoints share single filter that handles auth with and without certificate. This logic is encapsulated in the queryFilter or ticketFilter.
     * Ticket endpoint does require certificate to be present in RequestContext. It verifies the JWT token.
     */

    @Configuration
    @RequiredArgsConstructor
    @Order(3)
    class Ticket extends WebSecurityConfigurerAdapter {

        private final AuthenticationProvider tokenAuthenticationProvider;

        @Override
        protected void configure(AuthenticationManagerBuilder auth) {
            auth.authenticationProvider(tokenAuthenticationProvider); // for authenticating Tokens
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            baseConfigure(http.requestMatchers().antMatchers(
                authConfigurationProperties.getGatewayTicketEndpoint(),
                authConfigurationProperties.getGatewayTicketEndpointOldFormat()
            ).and()).authorizeRequests()
                .anyRequest().authenticated()
                .and()
                .logout().disable() // logout filter in this chain not needed
                .x509() //default x509 filter, authenticates trusted cert, ticketFilter(..) depends on this
                .subjectPrincipalRegex(EXTRACT_USER_PRINCIPAL_FROM_COMMON_NAME)
                .userDetailsService(new SimpleUserDetailService())
                .and()
                .addFilterBefore(ticketFilter("/**", authenticationManager()), UsernamePasswordAuthenticationFilter.class);
        }

        private QueryFilter ticketFilter(String ticketEndpoint, AuthenticationManager authenticationManager) {
            return new QueryFilter(
                ticketEndpoint,
                successfulTicketHandler,
                handlerInitializer.getAuthenticationFailureHandler(),
                authenticationService,
                HttpMethod.POST,
                true,
                authenticationManager);
        }
    }

    /**
     * Endpoints which are protected by client certificate
     * Default Spring security x509 filter authenticates any trusted certificate
     */
    @Configuration
    @RequiredArgsConstructor
    @Order(4)
    class CertificateProtectedEndpoints extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            baseConfigure(http.requestMatchers()
                .antMatchers(HttpMethod.DELETE, CacheServiceController.CONTROLLER_PATH + "/**")
                .antMatchers(AuthController.CONTROLLER_PATH + AuthController.INVALIDATE_PATH, AuthController.CONTROLLER_PATH + AuthController.DISTRIBUTE_PATH).and()
            ).authorizeRequests()
                .anyRequest().authenticated()
                .and()
                .logout().disable() // logout filter in this chain not needed
                .x509() // default x509 filter, authenticates trusted cert
                .subjectPrincipalRegex(EXTRACT_USER_PRINCIPAL_FROM_COMMON_NAME)
                .userDetailsService(new SimpleUserDetailService());
        }
    }

    /**
     * Endpoints which require either authentication or client certificate
     * Filters for tokens and credentials are placed in front of certificate filter for precedence
     * Default Spring security x509 filter authenticates any trusted certificate
     */
    @Configuration
    @RequiredArgsConstructor
    @Order(5)
    class CertificateOrAuthProtectedEndpoints extends WebSecurityConfigurerAdapter {

        private final CompoundAuthProvider compoundAuthProvider;
        private final AuthenticationProvider tokenAuthenticationProvider;

        private final String[] protectedEndpoints = {
            "/application",
            ServicesInfoController.SERVICES_URL
        };

        @Override
        protected void configure(AuthenticationManagerBuilder auth) {
            auth.authenticationProvider(compoundAuthProvider); // for authenticating credentials
            auth.authenticationProvider(tokenAuthenticationProvider); // for authenticating Tokens
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            baseConfigure(http.requestMatchers()
                .antMatchers("/application/**")
                .antMatchers(ServicesInfoController.SERVICES_URL + "/**").and()
            ).authorizeRequests()
                .anyRequest().authenticated()
                .and()
                .logout().disable() // logout filter in this chain not needed
                .x509() // default x509 filter, authenticates trusted cert
                .subjectPrincipalRegex(EXTRACT_USER_PRINCIPAL_FROM_COMMON_NAME)
                .userDetailsService(new SimpleUserDetailService())
                .and()
                // place the following filters before the x509 filter
                .addFilterBefore(basicFilter(authenticationManager()), org.springframework.security.web.authentication.preauth.x509.X509AuthenticationFilter.class)
                .addFilterBefore(cookieFilter(authenticationManager()), org.springframework.security.web.authentication.preauth.x509.X509AuthenticationFilter.class);
        }

        /**
         * Processes basic authenticaiton credentials and authenticates them
         */
        private BasicContentFilter basicFilter(AuthenticationManager authenticationManager) {
            return new BasicContentFilter(
                authenticationManager,
                handlerInitializer.getAuthenticationFailureHandler(),
                handlerInitializer.getResourceAccessExceptionHandler(),
                protectedEndpoints);
        }

        /**
         * Processes token credentials stored in cookie and authenticates them
         */
        private CookieContentFilter cookieFilter(AuthenticationManager authenticationManager) {
            return new CookieContentFilter(
                authenticationManager,
                handlerInitializer.getAuthenticationFailureHandler(),
                handlerInitializer.getResourceAccessExceptionHandler(),
                authConfigurationProperties,
                protectedEndpoints);
        }
    }

    /**
     * fallback filterchain for all other requests
     * All Routing goes through here
     * The filterchain does not require authentication
     * Web security is configured here and only here
     */
    @Configuration
    @RequiredArgsConstructor
    @Order(100)
    class DefaultSecurity extends WebSecurityConfigurerAdapter {

        /**
         * "Singleton" configuration of web security
         */
        @Override
        public void configure(WebSecurity web) throws Exception {
            configureWebSecurity(web);
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            baseConfigure(http.requestMatchers().antMatchers("/**", applicationContextPath + "/version").and())
                .authorizeRequests()
                .anyRequest()
                .permitAll()
                .and().logout().disable();
        }
    }

    /**
     * Common configuration for all filterchains
     */
    protected HttpSecurity baseConfigure(HttpSecurity http) throws Exception {
        if (isAttlsEnabled) {
            http.addFilterBefore(new AttlsFilter(), org.springframework.security.web.authentication.preauth.x509.X509AuthenticationFilter.class);
            http.addFilterBefore(new SecureConnectionFilter(), AttlsFilter.class);
        }
        return http
            .cors()
            .and().csrf().disable()    // NOSONAR we are using SAMESITE cookie to mitigate CSRF
            .headers().httpStrictTransportSecurity().disable()
            .frameOptions().disable()
            .and().exceptionHandling().authenticationEntryPoint(handlerInitializer.getBasicAuthUnauthorizedHandler())
            .and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .exceptionHandling().authenticationEntryPoint(handlerInitializer.getBasicAuthUnauthorizedHandler())
            .and();
    }

    /**
     * Web security only needs to be configured once, putting it to multiple filter chains causes multiple evaluations of the same rules
     */
    public void configureWebSecurity(WebSecurity web) {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowUrlEncodedSlash(true);
        firewall.setAllowBackSlash(true);
        firewall.setAllowUrlEncodedPercent(true);
        firewall.setAllowUrlEncodedPeriod(true);
        firewall.setAllowSemicolon(true);
        web.httpFirewall(firewall);

        // Endpoints that skip Spring Security completely
        // There is no CORS filter on these endpoints. If you require CORS processing, use a defined filter chain
        web.ignoring()
            .antMatchers(InternalServerErrorController.ERROR_ENDPOINT, "/error",
                "/application/health", "/application/info", "/application/version",
                AuthController.CONTROLLER_PATH + AuthController.ALL_PUBLIC_KEYS_PATH,
                AuthController.CONTROLLER_PATH + AuthController.CURRENT_PUBLIC_KEYS_PATH);

    }


}
