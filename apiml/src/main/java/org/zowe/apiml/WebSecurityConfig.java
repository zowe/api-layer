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
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpBasicServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher.MatchResult;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.zowe.apiml.gateway.filters.security.BasicAuthFilter;
import org.zowe.apiml.gateway.filters.security.CookieAuthFilter;
import org.zowe.apiml.gateway.filters.security.TokenAuthFilter;
import org.zowe.apiml.gateway.service.BasicAuthProvider;
import org.zowe.apiml.gateway.service.TokenProvider;
import org.zowe.apiml.gateway.x509.X509Util;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;

import java.util.List;


@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private static final String DISCOVERY_REALM = "API Mediation Discovery Service realm";

    private final BasicAuthProvider basicAuthProvider;
    private final TokenProvider tokenProvider;

    @Value("${apiml.health.protected:true}")
    private boolean isHealthEndpointProtected;

    @Value("${apiml.security.ssl.verifySslCertificatesOfServices:true}")
    private boolean verifySslCertificatesOfServices;

    @Value("${apiml.security.ssl.nonStrictVerifySslCertificatesOfServices:false}")
    private boolean nonStrictVerifySslCertificatesOfServices;

    @Value("${apiml.internal-discovery.port:10011}")
    private int internalDiscoveryPort;

    private static final List<String> UNAUTHENTICATED_PATTERNS = List.of("/application/",
    "/favicon.ico",
    "/application/info",
    "/application/health",
    "/eureka/css/",
    "/eureka/js/",
    "/eureka/fonts/",
    "/eureka/images/");

    @Bean
    public SecurityWebFilterChain errorFilterChain(ServerHttpSecurity http) {
        return http
            .securityMatcher(ServerWebExchangeMatchers.pathMatchers("/error"))
            .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
            .build();
    }

    /**
    * Filter chain for protecting endpoints with client certificate
    */
    @Bean
    public SecurityWebFilterChain clientCertificateFilterChain(ServerHttpSecurity http) {
        http
            .securityMatcher(matcher -> { // Matches Discovery internal port and URLs with /eureka/**
                var uri = matcher.getRequest().getURI();
                var port = uri.getPort();
                if (port == internalDiscoveryPort
                    && UNAUTHENTICATED_PATTERNS.stream().noneMatch(p -> uri.getPath().startsWith(p))
                    && uri.getPath().startsWith("/eureka/")) {
                        return MatchResult.match();
                }
                return MatchResult.notMatch();
            })
            .authorizeExchange(authorizeExchangeSpec ->
                authorizeExchangeSpec
                    .anyExchange().authenticated()
            )
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable);

        if (verifySslCertificatesOfServices) {
            return x509SecurityConfig(http).build();
        } else {
            http
                .authorizeExchange(exchange -> exchange
                    .anyExchange().permitAll()
                );
        }

        return http.build();
    }

    public ServerHttpSecurity x509SecurityConfig(ServerHttpSecurity http) {
        return http
            .headers(customizer -> customizer.frameOptions(ServerHttpSecurity.HeaderSpec.FrameOptionsSpec::disable))
            .x509(x509 -> x509
                .principalExtractor(X509Util.x509PrincipalExtractor())
                .authenticationManager(X509Util.x509ReactiveAuthenticationManager())
            )
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .exceptionHandling(exceptionHandlingSpec -> exceptionHandlingSpec.authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.FORBIDDEN)));
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            .securityMatcher(matcher -> {
                var uri = matcher.getRequest().getURI();
                var port = uri.getPort();
                if (port == internalDiscoveryPort && UNAUTHENTICATED_PATTERNS.stream().anyMatch(p -> uri.getPath().startsWith(p))) {
                    return MatchResult.match();
                }
                return MatchResult.notMatch();
            })
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchange -> {
                exchange
                    .pathMatchers(
         "/eureka/css/**",
                        "/eureka/js/**",
                        "/eureka/fonts/**",
                        "/eureka/images/**",
                        "/application/info",
                        "/favicon.ico"
                    )
                    .permitAll();

                if (!isHealthEndpointProtected) {
                    exchange.pathMatchers("/application/health").permitAll();
                }
                exchange.anyExchange().authenticated();
            });
        return http.build();
    }

    /**
    * Filter chain for protecting endpoints with MF credentials (basic or token)
    */
    @Bean
    public SecurityWebFilterChain discoveryBasicAuthOrToken(ServerHttpSecurity http, AuthConfigurationProperties authConfigurationProperties) {
        HttpBasicServerAuthenticationEntryPoint entryPoint = new HttpBasicServerAuthenticationEntryPoint();
        entryPoint.setRealm(DISCOVERY_REALM);
        return http
        .securityMatcher(matcher -> {
            var uri = matcher.getRequest().getURI();
            var port = uri.getPort();
            if (port == internalDiscoveryPort && UNAUTHENTICATED_PATTERNS.stream().noneMatch(p -> uri.getPath().startsWith(p))) {
                return MatchResult.match();
            }
            return MatchResult.notMatch();
        })
        .authorizeExchange(exchange -> exchange.anyExchange().authenticated())
        .httpBasic(spec -> spec.authenticationEntryPoint(entryPoint))
        .authenticationManager(reactiveAuthenticationManager())
        .addFilterAt(new TokenAuthFilter(tokenProvider, authConfigurationProperties), SecurityWebFiltersOrder.AUTHENTICATION)
        .addFilterAt(new BasicAuthFilter(basicAuthProvider), SecurityWebFiltersOrder.AUTHENTICATION)
        .addFilterAt(new CookieAuthFilter(authConfigurationProperties), SecurityWebFiltersOrder.AUTHENTICATION)
        .build();
    }

    @Bean
    ReactiveAuthenticationManager reactiveAuthenticationManager() {
        return new UserDetailsRepositoryReactiveAuthenticationManager(reactiveUserDetailsService());
    }

    @Bean
    MapReactiveUserDetailsService reactiveUserDetailsService() {
        UserDetails user = User.withUsername("eurekaClient")
        .password("")
        .authorities(List.of())
        .build();
        return new MapReactiveUserDetailsService(user);
    }

}
