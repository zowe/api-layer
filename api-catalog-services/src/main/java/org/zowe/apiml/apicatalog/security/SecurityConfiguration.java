/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.apicatalog.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.preauth.x509.X509AuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.zowe.apiml.filter.SecureConnectionFilter;
import org.zowe.apiml.filter.AttlsFilter;
import org.zowe.apiml.security.client.EnableApimlAuth;
import org.zowe.apiml.security.client.login.GatewayLoginProvider;
import org.zowe.apiml.security.client.token.GatewayTokenProvider;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.config.CertificateAuthenticationProvider;
import org.zowe.apiml.security.common.config.HandlerInitializer;
import org.zowe.apiml.security.common.content.BasicContentFilter;
import org.zowe.apiml.security.common.content.CookieContentFilter;
import org.zowe.apiml.security.common.filter.ApimlX509Filter;
import org.zowe.apiml.security.common.login.LoginFilter;
import org.zowe.apiml.security.common.login.ShouldBeAlreadyAuthenticatedFilter;

import java.util.Collections;
import java.util.Set;

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
@EnableApimlAuth
public class SecurityConfiguration {
    private static final String APIDOC_ROUTES = "/apidoc/**";

    private final ObjectMapper securityObjectMapper;
    private final AuthConfigurationProperties authConfigurationProperties;
    private final HandlerInitializer handlerInitializer;
    private final GatewayLoginProvider gatewayLoginProvider;
    private final GatewayTokenProvider gatewayTokenProvider;
    @Qualifier("publicKeyCertificatesBase64")
    private final Set<String> publicKeyCertificatesBase64;
    @Value("${server.attls.enabled:false}")
    private boolean isAttlsEnabled;

    /**
     * Filter chain for protecting /apidoc/** endpoints with MF credentials for client certificate.
     */
    @Configuration
    @Order(1)
    public class FilterChainBasicAuthOrTokenOrCertForApiDoc extends WebSecurityConfigurerAdapter {

        @Value("${apiml.security.ssl.verifySslCertificatesOfServices:true}")
        private boolean verifySslCertificatesOfServices;

        @Value("${apiml.security.ssl.nonStrictVerifySslCertificatesOfServices:false}")
        private boolean nonStrictVerifySslCertificatesOfServices;


        @Override
        protected void configure(AuthenticationManagerBuilder auth) {
            auth.authenticationProvider(gatewayLoginProvider);
            auth.authenticationProvider(gatewayTokenProvider);
            auth.authenticationProvider(new CertificateAuthenticationProvider());
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            mainframeCredentialsConfiguration(
                baseConfiguration(http.antMatcher(APIDOC_ROUTES)),
                authenticationManager()
            )
                .authorizeRequests()
                .antMatchers(APIDOC_ROUTES).authenticated();

            if (verifySslCertificatesOfServices || nonStrictVerifySslCertificatesOfServices) {
                if (isAttlsEnabled) {
                    http.x509()
                        .x509AuthenticationFilter(apimlX509Filter(authenticationManager())) // filter out API ML certificate
                        .userDetailsService(x509UserDetailsService())
                        .and()
                        .addFilterBefore(new AttlsFilter(), X509AuthenticationFilter.class)
                        .addFilterBefore(new SecureConnectionFilter(), AttlsFilter.class);
                } else {
                    http.x509()
                        .userDetailsService(x509UserDetailsService());
                }
            }
        }

        private UserDetailsService x509UserDetailsService() {
            return username -> new User(username, "", Collections.emptyList());
        }

        private ApimlX509Filter apimlX509Filter(AuthenticationManager authenticationManager) {
            ApimlX509Filter out = new ApimlX509Filter(publicKeyCertificatesBase64);
            out.setCertificateForClientAuth(crt -> out.getPublicKeyCertificatesBase64().contains(out.base64EncodePublicKey(crt)));
            out.setNotCertificateForClientAuth(crt -> !out.getPublicKeyCertificatesBase64().contains(out.base64EncodePublicKey(crt)));
            out.setAuthenticationManager(authenticationManager);
            return out;
        }
    }

    /**
     * Default filter chain to protect all routes with MF credentials.
     */
    @Configuration
    public class FilterChainBasicAuthOrTokenAllEndpoints extends WebSecurityConfigurerAdapter {

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
            mainframeCredentialsConfiguration(
                baseConfiguration(http),
                authenticationManager()
            )
                .authorizeRequests()
                .antMatchers("/static-api/**").authenticated()
                .antMatchers("/containers/**").authenticated()
                .antMatchers(APIDOC_ROUTES).authenticated()
                .antMatchers("/application/health", "/application/info").permitAll()
                .antMatchers("/application/**").authenticated();
            if (isAttlsEnabled) {
                http.addFilterBefore(new SecureConnectionFilter(), BasicContentFilter.class);
            }

        }
    }

    private HttpSecurity baseConfiguration(HttpSecurity http) throws Exception {
        http
            .csrf().disable()   // NOSONAR
            .headers()
            .httpStrictTransportSecurity().disable()
            .frameOptions().disable()
            .and()
            .exceptionHandling()

            .defaultAuthenticationEntryPointFor(
                handlerInitializer.getBasicAuthUnauthorizedHandler(), new AntPathRequestMatcher("/application/**")
            )
            .defaultAuthenticationEntryPointFor(
                handlerInitializer.getBasicAuthUnauthorizedHandler(), new AntPathRequestMatcher(APIDOC_ROUTES)
            )
            .defaultAuthenticationEntryPointFor(
                handlerInitializer.getUnAuthorizedHandler(), new AntPathRequestMatcher("/**")
            )

            .and()
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        return http;
    }

    private HttpSecurity mainframeCredentialsConfiguration(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {
        http
            // login endpoint
            .addFilterBefore(new ShouldBeAlreadyAuthenticatedFilter(authConfigurationProperties.getServiceLoginEndpoint(), handlerInitializer.getAuthenticationFailureHandler()), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(loginFilter(authConfigurationProperties.getServiceLoginEndpoint(), authenticationManager), ShouldBeAlreadyAuthenticatedFilter.class)
            .authorizeRequests()
            .antMatchers(HttpMethod.POST, authConfigurationProperties.getServiceLoginEndpoint()).permitAll()

            // logout endpoint
            .and()
            .logout()
            .logoutUrl(authConfigurationProperties.getServiceLogoutEndpoint())
            .logoutSuccessHandler(logoutSuccessHandler())

            // endpoints protection
            .and()
            .addFilterBefore(basicFilter(authenticationManager), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(cookieFilter(authenticationManager), UsernamePasswordAuthenticationFilter.class);

        return http;
    }

    private LoginFilter loginFilter(String loginEndpoint, AuthenticationManager authenticationManager) {
        return new LoginFilter(
            loginEndpoint,
            handlerInitializer.getSuccessfulLoginHandler(),
            handlerInitializer.getAuthenticationFailureHandler(),
            securityObjectMapper,
            authenticationManager,
            handlerInitializer.getResourceAccessExceptionHandler()
        );
    }

    /**
     * Secures content with a basic authentication
     */
    private BasicContentFilter basicFilter(AuthenticationManager authenticationManager) {
        return new BasicContentFilter(
            authenticationManager,
            handlerInitializer.getAuthenticationFailureHandler(),
            handlerInitializer.getResourceAccessExceptionHandler()
        );
    }

    /**
     * Secures content with a token stored in a cookie
     */
    private CookieContentFilter cookieFilter(AuthenticationManager authenticationManager) {
        return new CookieContentFilter(
            authenticationManager,
            handlerInitializer.getAuthenticationFailureHandler(),
            handlerInitializer.getResourceAccessExceptionHandler(),
            authConfigurationProperties);
    }

    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {
        return new ApiCatalogLogoutSuccessHandler(authConfigurationProperties);
    }
}
