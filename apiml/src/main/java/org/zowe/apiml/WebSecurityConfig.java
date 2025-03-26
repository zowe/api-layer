/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpBasicServerAuthenticationEntryPoint;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.zowe.apiml.gateway.filters.security.BasicAuthFilter;
import org.zowe.apiml.gateway.filters.security.CookieAuthFilter;
import org.zowe.apiml.gateway.filters.security.TokenAuthFilter;
import org.zowe.apiml.gateway.service.BasicAuthProvider;
import org.zowe.apiml.gateway.service.TokenProvider;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.config.HandlerInitializer;


@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {
    private static final String DISCOVERY_REALM = "API Mediation Discovery Service realm";
    private final BasicAuthProvider basicAuthProvider;
    private final TokenProvider tokenProvider;
    private final HandlerInitializer handlerInitializer;
    private final AuthConfigurationProperties securityConfigurationProperties;

    @Value("${apiml.health.protected:true}")
    private boolean isHealthEndpointProtected;

    @Value("${apiml.security.ssl.verifySslCertificatesOfServices:true}")
    private boolean verifySslCertificatesOfServices;

    @Value("${apiml.security.ssl.nonStrictVerifySslCertificatesOfServices:false}")
    private boolean nonStrictVerifySslCertificatesOfServices;

//    @Bean
//    @Order(Ordered.HIGHEST_PRECEDENCE)
//    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
//        http.securityMatcher(ServerWebExchangeMatchers.pathMatchers(
//            "/application/**",
//            "/favicon.ico",
//            "/application/info",
//            "/application/health",
//            "/eureka/css/**",
//            "/eureka/js/**",
//            "/eureka/fonts/**",
//            "/eureka/images/**"
//        ));
//
//        http.csrf(ServerHttpSecurity.CsrfSpec::disable);
//        http.authorizeExchange(exchange -> {
//            exchange
//                .pathMatchers(
//                    "/eureka/css/**",
//                    "/eureka/js/**",
//                    "/eureka/fonts/**",
//                    "/eureka/images/**",
//                    "/application/info",
//                    "/favicon.ico"
//                ).permitAll();
//
//            if (!isHealthEndpointProtected) {
//                exchange.pathMatchers("/application/health").permitAll();
//            }
//
//            exchange.anyExchange().authenticated();
//        });
//
//        return http.build();
//    }

//    @Bean
//    @Order(1)
//    public SecurityWebFilterChain errorFilterChain(ServerHttpSecurity http) {
//        return http
//            .securityMatcher(ServerWebExchangeMatchers.pathMatchers("/error"))
//            .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
//            .build();
//    }
//
//    /**
//     * Filter chain for protecting endpoints with client certificate
//     */
//    @Bean
//    @Order(2)
//    public SecurityWebFilterChain clientCertificateFilterChain(ServerHttpSecurity http) {
//        http.securityMatcher(new PathPatternParserServerWebExchangeMatcher("/eureka/**"));
//
//        http.httpBasic(ServerHttpSecurity.HttpBasicSpec::disable);
//        http.formLogin(ServerHttpSecurity.FormLoginSpec::disable);
//
//        if (verifySslCertificatesOfServices || !nonStrictVerifySslCertificatesOfServices) {
//           return x509SecurityConfig(http).build();
//        } else {
//            http.authorizeExchange(exchange -> exchange
//                .anyExchange().permitAll()
//            );
//        }
//
//        return http.build();
//    }
//
//    public ServerHttpSecurity x509SecurityConfig(ServerHttpSecurity http) {
//        return http
//            .headers(customizer -> customizer.frameOptions(ServerHttpSecurity.HeaderSpec.FrameOptionsSpec::disable))
//            .x509(x509 -> x509
//                .principalExtractor(X509Util.x509PrincipalExtractor())
//                .authenticationManager(reactiveAuthenticationManager())
//            )
//            .csrf(ServerHttpSecurity.CsrfSpec::disable);
//    }
//
    /**
     * Filter chain for protecting endpoints with MF credentials (basic or token)
     */
    @Bean
    @Order(3)
    public SecurityWebFilterChain basicAuthOrTokenFilterChain(ServerHttpSecurity http, AuthConfigurationProperties authConfigurationProperties) {
        HttpBasicServerAuthenticationEntryPoint entryPoint = new HttpBasicServerAuthenticationEntryPoint();
        entryPoint.setRealm(DISCOVERY_REALM);
        return http
            .securityMatcher(ServerWebExchangeMatchers.pathMatchers("/application/**", "/*", "/eureka"))
            .authorizeExchange(exchange -> exchange.anyExchange().authenticated())
            .httpBasic(spec -> spec.authenticationEntryPoint(entryPoint))
            .addFilterAt(new TokenAuthFilter(tokenProvider, authConfigurationProperties), SecurityWebFiltersOrder.AUTHENTICATION)
            .addFilterAt(new BasicAuthFilter(basicAuthProvider), SecurityWebFiltersOrder.AUTHENTICATION)
            .addFilterAt(new CookieAuthFilter(authConfigurationProperties), SecurityWebFiltersOrder.AUTHENTICATION)
            .build();
    }
//
//    /**
//     * Filter chain for protecting endpoints with MF credentials (basic or token) or x509 certificate
//     */
//    @Bean
//    @Order(4)
//    public SecurityWebFilterChain basicAuthOrTokenOrCertFilterChain(ServerHttpSecurity http, AuthConfigurationProperties authConfigurationProperties) throws Exception {
//        HttpBasicServerAuthenticationEntryPoint entryPoint = new HttpBasicServerAuthenticationEntryPoint();
//        entryPoint.setRealm(DISCOVERY_REALM);
//        return x509SecurityConfig(http)
//            .securityMatcher(ServerWebExchangeMatchers.pathMatchers("/discovery/**"))
//            .httpBasic(spec -> spec.authenticationEntryPoint(entryPoint))
//            .addFilterAt(new TokenAuthFilter(tokenProvider, authConfigurationProperties), SecurityWebFiltersOrder.AUTHENTICATION)
//            .addFilterAt(new BasicAuthFilter(basicAuthProvider), SecurityWebFiltersOrder.AUTHENTICATION)
//            .addFilterAt(new CookieAuthFilter(authConfigurationProperties), SecurityWebFiltersOrder.AUTHENTICATION)
//            .build();
//    }
//
//    @Bean
//    ReactiveAuthenticationManager reactiveAuthenticationManager() {
//        return new UserDetailsRepositoryReactiveAuthenticationManager(reactiveUserDetailsService());
//    }
//
//    // ok
//    @Bean
//    MapReactiveUserDetailsService reactiveUserDetailsService() {
//        UserDetails user = User.withUsername("eurekaClient")
//            .password("")
//            .authorities(List.of())
//            .build();
//        return new MapReactiveUserDetailsService(user);
//    }

//    /**
//     * Used for dummy authentication provider
//     */
//    @Bean
//    public BCryptPasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder(10);
//    }
}
