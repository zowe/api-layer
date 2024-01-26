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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.preauth.x509.X509AuthenticationFilter;
import org.zowe.apiml.filter.AttlsFilter;
import org.zowe.apiml.filter.SecureConnectionFilter;
import org.zowe.apiml.security.client.EnableApimlAuth;
import org.zowe.apiml.security.client.login.GatewayLoginProvider;
import org.zowe.apiml.security.client.token.GatewayTokenProvider;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.config.HandlerInitializer;
import org.zowe.apiml.security.common.content.BasicContentFilter;
import org.zowe.apiml.security.common.content.BearerContentFilter;
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
public class HttpsWebSecurityConfig extends AbstractWebSecurityConfigurer {

    private final HandlerInitializer handlerInitializer;
    private final AuthConfigurationProperties securityConfigurationProperties;
    private final GatewayLoginProvider gatewayLoginProvider;
    private final GatewayTokenProvider gatewayTokenProvider;
    private static final String DISCOVERY_REALM = "API Mediation Discovery Service realm";
    @Value("${server.attls.enabled:false}")
    private boolean isAttlsEnabled;

    @Value("${apiml.security.ssl.verifySslCertificatesOfServices:true}")
    private boolean verifySslCertificatesOfServices;

    @Value("${apiml.security.ssl.nonStrictVerifySslCertificatesOfServices:false}")
    private boolean nonStrictVerifySslCertificatesOfServices;

    @Bean
    public WebSecurityCustomizer httpsWebSecurityCustomizer() {
        String[] noSecurityAntMatchers = {
            "/eureka/css/**",
            "/eureka/js/**",
            "/eureka/fonts/**",
            "/eureka/images/**",
            "/application/health",
            "/application/info",
            "/favicon.ico"
        };
        return web -> {
            web.ignoring().requestMatchers(noSecurityAntMatchers);
        };
    }
    /**
     * Filter chain for protecting endpoints with MF credentials (basic or token) or x509 certificate
     */
    @Bean
    @Order(1)
    public SecurityFilterChain errorHandler(HttpSecurity http) throws Exception {
        return baseConfigure(http.securityMatcher("/error")).build();
    }
    /**
     * Filter chain for protecting endpoints with MF credentials (basic or token)
     */
    @Bean
    @Order(2)
    public SecurityFilterChain basicAuthOrTokenFilterChain(HttpSecurity http) throws Exception {
        baseConfigure(http.securityMatchers(matchers -> matchers.requestMatchers(
            "/application/**",
            "/*"
        )))
            .authenticationProvider(gatewayLoginProvider)
            .authenticationProvider(gatewayTokenProvider)
            .authorizeHttpRequests(requests -> requests
                .requestMatchers("/**").authenticated())
            .httpBasic(basic -> basic.realmName(DISCOVERY_REALM));
        if (isAttlsEnabled) {
            http.addFilterBefore(new SecureConnectionFilter(), UsernamePasswordAuthenticationFilter.class);
        }

        return http.with(new CustomSecurityFilters(), t -> {
        }).build();
    }

    /**
     * Filter chain for protecting endpoints with client certificate
     */
    @Bean
    @Order(3)
    public SecurityFilterChain clientCertificateFilterChain(HttpSecurity http) throws Exception {
        baseConfigure(http.securityMatcher("/eureka/**"));
        if (verifySslCertificatesOfServices || !nonStrictVerifySslCertificatesOfServices) {
            http.x509(x509 -> x509.userDetailsService(x509UserDetailsService()))
                .authorizeHttpRequests(requests -> requests
                    .anyRequest().authenticated()
                );
            if (isAttlsEnabled) {
                http.addFilterBefore(new AttlsFilter(), X509AuthenticationFilter.class);
                http.addFilterBefore(new SecureConnectionFilter(), AttlsFilter.class);
            }
        } else {
            http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
        }
        return http.build();
    }

    /**
     * Filter chain for protecting endpoints with MF credentials (basic or token) or x509 certificate
     */
    @Bean
    @Order(4)
    public SecurityFilterChain basicAuthOrTokenOrCertFilterChain(HttpSecurity http) throws Exception {
        baseConfigure(http.securityMatcher("/discovery/**"))
            .authenticationProvider(gatewayLoginProvider)
            .authenticationProvider(gatewayTokenProvider)
            .httpBasic(basic -> basic.realmName(DISCOVERY_REALM));
        if (verifySslCertificatesOfServices || !nonStrictVerifySslCertificatesOfServices) {
            http.authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
                .x509(x509 -> x509.userDetailsService(x509UserDetailsService()));
            if (isAttlsEnabled) {
                http.addFilterBefore(new AttlsFilter(), X509AuthenticationFilter.class);
                http.addFilterBefore(new SecureConnectionFilter(), AttlsFilter.class);
            }
        }

        return http.with(new CustomSecurityFilters(), t -> {
        }).build();
    }


    private class CustomSecurityFilters extends AbstractHttpConfigurer<CustomSecurityFilters, HttpSecurity> {
        @Override
        public void configure(HttpSecurity http) {
            AuthenticationManager authenticationManager = http.getSharedObject(AuthenticationManager.class);

            http.addFilterBefore(basicFilter(authenticationManager), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(cookieFilter(authenticationManager), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(bearerContentFilter(authenticationManager), UsernamePasswordAuthenticationFilter.class);
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

        /**
         * Secures content with a Bearer token
         */
        private BearerContentFilter bearerContentFilter(AuthenticationManager authenticationManager) {
            return new BearerContentFilter(
                authenticationManager,
                handlerInitializer.getAuthenticationFailureHandler(),
                handlerInitializer.getResourceAccessExceptionHandler()
            );
        }
    }

    private UserDetailsService x509UserDetailsService() {
        return username -> new User("eurekaClient", "", Collections.emptyList());
    }
}
