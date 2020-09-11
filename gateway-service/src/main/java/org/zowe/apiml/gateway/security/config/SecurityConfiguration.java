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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.zowe.apiml.gateway.controllers.AuthController;
import org.zowe.apiml.gateway.controllers.CacheServiceController;
import org.zowe.apiml.gateway.security.login.x509.X509AuthenticationProvider;
import org.zowe.apiml.gateway.security.query.QueryFilter;
import org.zowe.apiml.gateway.security.query.SuccessfulQueryHandler;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.security.ticket.SuccessfulTicketHandler;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.config.HandlerInitializer;
import org.zowe.apiml.security.common.content.BasicContentFilter;
import org.zowe.apiml.security.common.content.CookieContentFilter;
import org.zowe.apiml.security.common.handler.FailedAuthenticationHandler;
import org.zowe.apiml.security.common.login.LoginFilter;
import org.zowe.apiml.security.common.token.TokenFormatNotValidException;

import javax.servlet.ServletException;
import java.util.*;

/**
 * Security configuration for Gateway
 * <p>
 * 1. Adds Login and Query endpoints
 * 2. Allows basic and token (cookie) authentication
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {
    // List of endpoints protected by content filters
    private static final String[] PROTECTED_ENDPOINTS = {
        "/api/v1/gateway",
        "/application"
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

    @Override
    protected void configure(AuthenticationManagerBuilder auth) {
        authProviderInitializer.configure(auth);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .cors().and()
            .csrf().disable()
            .headers()
            .httpStrictTransportSecurity().disable()
            .frameOptions().disable()
            .and()
            .exceptionHandling().authenticationEntryPoint(handlerInitializer.getBasicAuthUnauthorizedHandler())

            .and()
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)

            // login endpoint
            .and()
            .authorizeRequests()
            .antMatchers(HttpMethod.POST, authConfigurationProperties.getGatewayLoginEndpoint()).permitAll()

            // ticket endpoint
            .and()
            .authorizeRequests()
            .antMatchers(HttpMethod.POST, authConfigurationProperties.getGatewayTicketEndpoint()).authenticated()
            .and().x509()
            .userDetailsService(x509UserDetailsService())

            // logout endpoint
            .and()
            .logout()
            .logoutRequestMatcher(new AntPathRequestMatcher(authConfigurationProperties.getGatewayLogoutEndpoint(), HttpMethod.POST.name()))
            .addLogoutHandler(logoutHandler())
            .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
            .permitAll()

            // endpoint protection
            .and()
            .authorizeRequests()
            .antMatchers("/application/health", "/application/info").permitAll()
            .antMatchers("/application/**").authenticated()

            // auth controller
            .and()
            .authorizeRequests()
            .antMatchers(
                AuthController.CONTROLLER_PATH + AuthController.ALL_PUBLIC_KEYS_PATH,
                AuthController.CONTROLLER_PATH + AuthController.CURRENT_PUBLIC_KEYS_PATH
            ).permitAll()
            .and()
            .authorizeRequests()
            .antMatchers(AuthController.CONTROLLER_PATH + AuthController.INVALIDATE_PATH, AuthController.CONTROLLER_PATH + AuthController.DISTRIBUTE_PATH).authenticated()
            .and().x509()
            .x509AuthenticationFilter(apimlX509Filter())
            .subjectPrincipalRegex(EXTRACT_USER_PRINCIPAL_FROM_COMMON_NAME)
            .userDetailsService(x509UserDetailsService())

            // cache controller
            .and()
            .authorizeRequests()
            .antMatchers(HttpMethod.DELETE, CacheServiceController.CONTROLLER_PATH, CacheServiceController.CONTROLLER_PATH + "/**").authenticated()
            .and().x509()
            .x509AuthenticationFilter(apimlX509Filter())
            .subjectPrincipalRegex(EXTRACT_USER_PRINCIPAL_FROM_COMMON_NAME)
            .userDetailsService(x509UserDetailsService())

            // add filters - login, query, ticket
            .and()
            .addFilterBefore(loginFilter(authConfigurationProperties.getGatewayLoginEndpoint()), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(x509Filter(authConfigurationProperties.getGatewayLoginEndpoint()), LoginFilter.class)
            .addFilterBefore(queryFilter(authConfigurationProperties.getGatewayQueryEndpoint()), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(ticketFilter(authConfigurationProperties.getGatewayTicketEndpoint()), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(basicFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(cookieFilter(), UsernamePasswordAuthenticationFilter.class);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        final CorsConfiguration config = new CorsConfiguration();
        List<String> pathsToEnable;
        if (corsEnabled) {
            addCorsRelatedIgnoredHeaders();

            config.setAllowCredentials(true);
            config.addAllowedOrigin(CorsConfiguration.ALL);
            config.setAllowedHeaders(Collections.singletonList(CorsConfiguration.ALL));
            config.setAllowedMethods(allowedCorsHttpMethods());
            pathsToEnable = CORS_ENABLED_ENDPOINTS;
        } else {
            pathsToEnable = Collections.singletonList("/**");
        }
        pathsToEnable.forEach(path -> source.registerCorsConfiguration(path, config));
        return source;
    }

    private void addCorsRelatedIgnoredHeaders() {
        zuulProperties.setIgnoredHeaders(new HashSet<>(
            Arrays.asList((ignoredHeadersWhenCorsEnabled).split(","))
        ));
    }

    @Bean
    List<String> allowedCorsHttpMethods() {
        return Collections.unmodifiableList(Arrays.asList(
            HttpMethod.GET.name(), HttpMethod.HEAD.name(), HttpMethod.POST.name(),
            HttpMethod.DELETE.name(), HttpMethod.PUT.name(), HttpMethod.OPTIONS.name()
        ));
    }

    /**
     * Processes /login requests
     */
    private LoginFilter loginFilter(String loginEndpoint) throws Exception {
        return new LoginFilter(
            loginEndpoint,
            handlerInitializer.getSuccessfulLoginHandler(),
            handlerInitializer.getAuthenticationFailureHandler(),
            securityObjectMapper,
            authenticationManager(),
            handlerInitializer.getResourceAccessExceptionHandler());
    }

    private X509AuthenticationFilter x509Filter(String loginEndpoint) {
        return new X509AuthenticationFilter(loginEndpoint,
            handlerInitializer.getSuccessfulLoginHandler(),
            x509AuthenticationProvider);
    }

    /**
     * Processes /query requests
     */
    private QueryFilter queryFilter(String queryEndpoint) throws Exception {
        return new QueryFilter(
            queryEndpoint,
            successfulQueryHandler,
            handlerInitializer.getAuthenticationFailureHandler(),
            authenticationService,
            HttpMethod.GET,
            false,
            authenticationManager());
    }

    /**
     * Processes /ticket requests
     */
    private QueryFilter ticketFilter(String ticketEndpoint) throws Exception {
        return new QueryFilter(
            ticketEndpoint,
            successfulTicketHandler,
            handlerInitializer.getAuthenticationFailureHandler(),
            authenticationService,
            HttpMethod.POST,
            true,
            authenticationManager());
    }

    /**
     * Secures content with a basic authentication
     */
    private BasicContentFilter basicFilter() throws Exception {
        return new BasicContentFilter(
            authenticationManager(),
            handlerInitializer.getAuthenticationFailureHandler(),
            handlerInitializer.getResourceAccessExceptionHandler(),
            PROTECTED_ENDPOINTS);
    }

    /**
     * Secures content with a token stored in a cookie
     */
    private CookieContentFilter cookieFilter() throws Exception {
        return new CookieContentFilter(
            authenticationManager(),
            handlerInitializer.getAuthenticationFailureHandler(),
            handlerInitializer.getResourceAccessExceptionHandler(),
            authConfigurationProperties,
            PROTECTED_ENDPOINTS);
    }

    /**
     * Handles the logout action by checking the validity of JWT token passed in the Cookie.
     * If present, the token will be invalidated.
     */
    private LogoutHandler logoutHandler() {
        FailedAuthenticationHandler failure = handlerInitializer.getAuthenticationFailureHandler();
        return (request, response, authentication) -> {
            try {
                authenticationService.getJwtTokenFromRequestToLogout(request)
                    .ifPresent(x ->
                        authenticationService.invalidateJwtToken(x, true)
                    );
            }
            catch (TokenFormatNotValidException e) {
                try {
                    failure.onAuthenticationFailure(request, response, e);
                } catch (ServletException ex) {
                    ex.printStackTrace();
                }
            }
        };
    }

    /**
     * The problem the squad was trying to solve:
     * For certain endpoints the Gateway should accept only certificates issued by API Mediation Layer. The key reason
     * is that these endpoints leverage the Gateway certificate as an authentication method for the downstream service
     * This certificate has stronger priviliges and as such the Gateway need to be more careful with who can use it.
     * <p>
     * This solution is risky as it removes the certificates from the chain from all subsequent processing.
     * Despite this it seems to be the simplest solution to the problem.
     * While exploring the topic, the API squad explored following possibilities without any results:
     * 1) As this is more linked to the authentication, move it to the CertificateAuthenticationProvider and distinguish
     * authentication based on the provided certificates - This doesn't work as the CertificateAuthenticationProvider
     * is never used
     * 2) Move the logic to decide whether the client which is signed by the Gateway client certificate should be used
     * into the HttpClientChooser - This didn't work as the HttpClientChooser isn't used in these specific calls.
     */
    private ApimlX509Filter apimlX509Filter() throws Exception {
        ApimlX509Filter out = new ApimlX509Filter(publicKeyCertificatesBase64);
        out.setAuthenticationManager(authenticationManager());
        return out;
    }

    private UserDetailsService x509UserDetailsService() {
        return username -> new User("gatewayClient", "", Collections.emptyList());
    }

    @Override
    public void configure(WebSecurity web) {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowUrlEncodedSlash(true);
        firewall.setAllowBackSlash(true);
        firewall.setAllowUrlEncodedPercent(true);
        firewall.setAllowUrlEncodedPeriod(true);
        firewall.setAllowSemicolon(true);
        web.httpFirewall(firewall);

        web.ignoring()
            .antMatchers(AuthController.CONTROLLER_PATH + AuthController.PUBLIC_KEYS_PATH + "/**");
    }


}
