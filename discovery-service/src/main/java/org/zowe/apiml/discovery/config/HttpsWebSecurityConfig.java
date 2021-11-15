/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.preauth.x509.X509AuthenticationFilter;
import org.zowe.apiml.filter.SecureConnectionFilter;
import org.zowe.apiml.filter.AttlsFilter;
import org.zowe.apiml.security.client.EnableApimlAuth;
import org.zowe.apiml.security.client.login.GatewayLoginProvider;
import org.zowe.apiml.security.client.token.GatewayTokenProvider;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.config.HandlerInitializer;
import org.zowe.apiml.security.common.content.BasicContentFilter;
import org.zowe.apiml.security.common.content.CookieContentFilter;

import java.util.Collections;

/**
 * Main class configuring Spring security for Discovery Service
 * <p>
 * This configuration is applied if "https" Spring profile is active
 */
@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
@EnableApimlAuth
@Profile({"https", "attls"})
public class HttpsWebSecurityConfig {

    private final HandlerInitializer handlerInitializer;
    private final AuthConfigurationProperties securityConfigurationProperties;
    private final GatewayLoginProvider gatewayLoginProvider;
    private final GatewayTokenProvider gatewayTokenProvider;
    private static final String DISCOVERY_REALM = "API Mediation Discovery Service realm";
    @Value("${server.attls.enabled:false}")
    private boolean isAttlsEnabled;

    /**
     * Filter chain for protecting endpoints with MF credentials (basic or token)
     */
    @Configuration
    @Order(1)
    public class FilterChainBasicAuthOrToken extends AbstractWebSecurityConfigurer {

        @Override
        protected void configure(AuthenticationManagerBuilder auth) {
            auth.authenticationProvider(gatewayLoginProvider);
            auth.authenticationProvider(gatewayTokenProvider);
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {

            baseConfigure(http.requestMatchers().antMatchers(
                "/application/**",
                "/*"
            )
                .and())
                .addFilterBefore(basicFilter(authenticationManager()), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(cookieFilter(authenticationManager()), UsernamePasswordAuthenticationFilter.class)
                .authorizeRequests()
                .antMatchers("/**").authenticated()
                .and()
                .httpBasic().realmName(DISCOVERY_REALM);
            if (isAttlsEnabled) {
                http.addFilterBefore(new SecureConnectionFilter(), CookieContentFilter.class);
            }
        }
    }

    /**
     * Filter chain for protecting endpoints with client certificate
     */
    @Configuration
    @Order(2)
    public class FilterChainClientCertificate extends AbstractWebSecurityConfigurer {

        @Value("${apiml.security.ssl.verifySslCertificatesOfServices:true}")
        private boolean verifySslCertificatesOfServices;

        @Value("${apiml.security.ssl.nonStrictVerifySslCertificatesOfServices:false}")
        private boolean nonStrictVerifySslCertificatesOfServices;


        @Override
        public void configure(WebSecurity web) {
            String[] noSecurityAntMatchers = {
                "/eureka/css/**",
                "/eureka/js/**",
                "/eureka/fonts/**",
                "/eureka/images/**",
                "/application/health",
                "/application/info",
                "/application/hystrix.stream",
                "/favicon.ico"
            };
            web.ignoring().antMatchers(noSecurityAntMatchers);
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            baseConfigure(http.antMatcher("/eureka/**"));
            if (verifySslCertificatesOfServices || nonStrictVerifySslCertificatesOfServices) {
                http.authorizeRequests()
                    .anyRequest().authenticated()
                    .and().x509().userDetailsService(x509UserDetailsService());
                if (isAttlsEnabled) {
                    http.addFilterBefore(new AttlsFilter(), X509AuthenticationFilter.class);
                    http.addFilterBefore(new SecureConnectionFilter(), AttlsFilter.class);
                }
            } else {
                http.authorizeRequests().anyRequest().permitAll();
            }
        }
    }

    /**
     * Filter chain for protecting endpoints with MF credentials (basic or token) or x509 certificate
     */
    @Configuration
    @Order(3)
    public class FilterChainBasicAuthOrTokenOrCert extends AbstractWebSecurityConfigurer {

        @Value("${apiml.security.ssl.verifySslCertificatesOfServices:true}")
        private boolean verifySslCertificatesOfServices;

        @Value("${apiml.security.ssl.nonStrictVerifySslCertificatesOfServices:false}")
        private boolean nonStrictVerifySslCertificatesOfServices;


        @Override
        protected void configure(AuthenticationManagerBuilder auth) {
            auth.authenticationProvider(gatewayLoginProvider);
            auth.authenticationProvider(gatewayTokenProvider);
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            baseConfigure(http.antMatcher("/discovery/**"))
                .addFilterBefore(basicFilter(authenticationManager()), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(cookieFilter(authenticationManager()), UsernamePasswordAuthenticationFilter.class)
                .httpBasic().realmName(DISCOVERY_REALM);
            if (verifySslCertificatesOfServices || nonStrictVerifySslCertificatesOfServices) {
                http.authorizeRequests().anyRequest().authenticated().and()
                    .x509().userDetailsService(x509UserDetailsService());
                if (isAttlsEnabled) {
                    http.addFilterBefore(new AttlsFilter(), X509AuthenticationFilter.class);
                    http.addFilterBefore(new SecureConnectionFilter(), AttlsFilter.class);
                }
            }
        }
    }


    private BasicContentFilter basicFilter(AuthenticationManager authenticationManager) {
        return new BasicContentFilter(
            authenticationManager,
            handlerInitializer.getAuthenticationFailureHandler(),
            handlerInitializer.getResourceAccessExceptionHandler());
    }

    private CookieContentFilter cookieFilter(AuthenticationManager authenticationManager) {
        return new CookieContentFilter(
            authenticationManager,
            handlerInitializer.getAuthenticationFailureHandler(),
            handlerInitializer.getResourceAccessExceptionHandler(),
            securityConfigurationProperties);
    }

    private UserDetailsService x509UserDetailsService() {
        return username -> new User("eurekaClient", "", Collections.emptyList());
    }
}
