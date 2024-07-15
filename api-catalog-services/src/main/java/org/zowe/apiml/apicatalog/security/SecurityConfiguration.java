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
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.preauth.x509.X509AuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.zowe.apiml.filter.AttlsFilter;
import org.zowe.apiml.filter.SecureConnectionFilter;
import org.zowe.apiml.security.client.EnableApimlAuth;
import org.zowe.apiml.security.client.login.GatewayLoginProvider;
import org.zowe.apiml.security.client.token.GatewayTokenProvider;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.config.CertificateAuthenticationProvider;
import org.zowe.apiml.security.common.config.HandlerInitializer;
import org.zowe.apiml.security.common.content.BasicContentFilter;
import org.zowe.apiml.security.common.content.BearerContentFilter;
import org.zowe.apiml.security.common.content.CookieContentFilter;
import org.zowe.apiml.security.common.filter.CategorizeCertsFilter;
import org.zowe.apiml.security.common.login.LoginFilter;
import org.zowe.apiml.security.common.login.ShouldBeAlreadyAuthenticatedFilter;
import org.zowe.apiml.security.common.verify.CertificateValidator;

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
    private static final String STATIC_REFRESH_ROUTE = "/static-api/refresh";

    private final ObjectMapper securityObjectMapper;
    private final AuthConfigurationProperties authConfigurationProperties;
    private final HandlerInitializer handlerInitializer;
    private final GatewayLoginProvider gatewayLoginProvider;
    private final GatewayTokenProvider gatewayTokenProvider;
    private final CertificateValidator certificateValidator;

    @Qualifier("publicKeyCertificatesBase64")
    private final Set<String> publicKeyCertificatesBase64;

    @Value("${server.attls.enabled:false}")
    private boolean isAttlsEnabled;

    @Value("${apiml.metrics.enabled:false}")
    private boolean isMetricsEnabled;

    @Value("${apiml.health.protected:false}")
    private boolean isHealthEndpointProtected;

    /**
     * Filter chain for protecting /apidoc/** endpoints with MF credentials for client certificate.
     */
    @Configuration
    @Order(1)
    public class FilterChainBasicAuthOrTokenOrCertForApiDoc {

        @Value("${apiml.security.ssl.verifySslCertificatesOfServices:true}")
        private boolean verifySslCertificatesOfServices;

        @Value("${apiml.security.ssl.nonStrictVerifySslCertificatesOfServices:false}")
        private boolean nonStrictVerifySslCertificatesOfServices;

        @Bean
        public SecurityFilterChain basicAuthOrTokenOrCertApiDocFilterChain(HttpSecurity http) throws Exception {
            mainframeCredentialsConfiguration(
                    baseConfiguration(http.requestMatchers(matchers -> matchers.antMatchers(APIDOC_ROUTES, STATIC_REFRESH_ROUTE)))
            )
                    .authorizeHttpRequests(requests -> requests
                            .requestMatchers(APIDOC_ROUTES, STATIC_REFRESH_ROUTE).authenticated())
                    .authenticationProvider(gatewayLoginProvider)
                    .authenticationProvider(gatewayTokenProvider)
                    .authenticationProvider(new CertificateAuthenticationProvider());

            if (verifySslCertificatesOfServices || !nonStrictVerifySslCertificatesOfServices) {
                if (isAttlsEnabled) {
                    http.x509(x509 -> x509
                            .userDetailsService(x509UserDetailsService()))
                            .addFilterBefore(reversedCategorizeCertFilter(), X509AuthenticationFilter.class)
                            .addFilterBefore(new AttlsFilter(), X509AuthenticationFilter.class)
                            .addFilterBefore(new SecureConnectionFilter(), AttlsFilter.class);
                } else {
                    http.x509(x509 -> x509
                            .userDetailsService(x509UserDetailsService()));
                }
            }

            return http.build();
        }

        private UserDetailsService x509UserDetailsService() {
            return username -> new User(username, "", Collections.emptyList());
        }

        private CategorizeCertsFilter reversedCategorizeCertFilter() {
            CategorizeCertsFilter out = new CategorizeCertsFilter(publicKeyCertificatesBase64, certificateValidator);
            out.setCertificateForClientAuth(crt -> out.getPublicKeyCertificatesBase64().contains(out.base64EncodePublicKey(crt)));
            out.setApimlCertificate(crt -> !out.getPublicKeyCertificatesBase64().contains(out.base64EncodePublicKey(crt)));
            return out;
        }
    }

    /**
     * Default filter chain to protect all routes with MF credentials.
     */
    @Configuration
    @Order(2)
    public class FilterChainBasicAuthOrTokenAllEndpoints {

        @Bean
        public WebSecurityCustomizer webSecurityCustomizer() {
            String[] noSecurityAntMatchers = {
                "/",
                "/static/**",
                "/favicon.ico",
                "/api-doc"
            };
            return web -> web.ignoring().antMatchers(noSecurityAntMatchers);
        }

        @Bean
        public SecurityFilterChain basicAuthOrTokenAllEndpointsFilterChain(HttpSecurity http) throws Exception {
            mainframeCredentialsConfiguration(baseConfiguration(http))
                    .authorizeHttpRequests(requests -> requests
                            .requestMatchers("/static-api/**").authenticated()
                            .requestMatchers("/containers/**").authenticated()
                            .requestMatchers(APIDOC_ROUTES).authenticated()
                            .requestMatchers("/application/info").permitAll())
                    .authenticationProvider(gatewayLoginProvider)
                    .authenticationProvider(gatewayTokenProvider);


            if (isHealthEndpointProtected) {
             //   http.authorizeHttpRequests()
                http.authorizeHttpRequests(requests -> requests
                    .requestMatchers("/application/health").authenticated());
            } else {
                http.authorizeHttpRequests(requests -> requests
                    .requestMatchers("/application/health").permitAll());
            }

            if (isMetricsEnabled) {
                http.authorizeHttpRequests(requests -> requests.requestMatchers("/application/hystrixstream").permitAll());
            }

            http.authorizeHttpRequests(requests -> requests.requestMatchers("/application/**").authenticated());

            if (isAttlsEnabled) {
                http.addFilterBefore(new SecureConnectionFilter(), UsernamePasswordAuthenticationFilter.class);
            }
            return http.build();
        }
    }

    private HttpSecurity baseConfiguration(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())   // NOSONAR
                .headers(headers -> headers
                        .httpStrictTransportSecurity().disable()
                        .frameOptions().disable())
                .exceptionHandling(handling -> handling

                        .defaultAuthenticationEntryPointFor(
                                handlerInitializer.getBasicAuthUnauthorizedHandler(), new AntPathRequestMatcher("/application/**")
                        )
                        .defaultAuthenticationEntryPointFor(
                                handlerInitializer.getBasicAuthUnauthorizedHandler(), new AntPathRequestMatcher(APIDOC_ROUTES)
                        )
                        .defaultAuthenticationEntryPointFor(
                                handlerInitializer.getBasicAuthUnauthorizedHandler(), new AntPathRequestMatcher(STATIC_REFRESH_ROUTE)
                        )
                        .defaultAuthenticationEntryPointFor(
                                handlerInitializer.getUnAuthorizedHandler(), new AntPathRequestMatcher("/**")
                        ))
                .sessionManagement(management -> management
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http;
    }

    private HttpSecurity mainframeCredentialsConfiguration(HttpSecurity http) throws Exception {
        http
                // login endpoint
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.POST, authConfigurationProperties.getServiceLoginEndpoint()).permitAll())
                .logout(logout -> logout
                        .logoutUrl(authConfigurationProperties.getServiceLogoutEndpoint())
                        .logoutSuccessHandler(logoutSuccessHandler())).apply(new CustomSecurityFilters());

        return http;
    }

    private class CustomSecurityFilters extends AbstractHttpConfigurer<CustomSecurityFilters, HttpSecurity> {
        @Override
        public void configure(HttpSecurity http) throws Exception {
            AuthenticationManager authenticationManager = http.getSharedObject(AuthenticationManager.class);

            http.addFilterBefore(new ShouldBeAlreadyAuthenticatedFilter(authConfigurationProperties.getServiceLoginEndpoint(), handlerInitializer.getAuthenticationFailureHandler()), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(loginFilter(authConfigurationProperties.getServiceLoginEndpoint(), authenticationManager), ShouldBeAlreadyAuthenticatedFilter.class)
                .addFilterBefore(basicFilter(authenticationManager), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(cookieFilter(authenticationManager), BasicContentFilter.class)
                .addFilterAfter(bearerContentFilter(authenticationManager), CookieContentFilter.class);
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

    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {
        return new ApiCatalogLogoutSuccessHandler(authConfigurationProperties);
    }
}
