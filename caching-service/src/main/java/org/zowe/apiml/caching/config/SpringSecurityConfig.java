/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.zowe.apiml.security.common.util.X509Util;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SpringSecurityConfig {

    @Value("${apiml.service.ssl.verifySslCertificatesOfServices:true}")
    private boolean verifyCertificates;

    @Value("${apiml.service.ssl.nonStrictVerifySslCertificatesOfServices:false}")
    private boolean nonStrictVerifyCerts;

    @Value("${apiml.health.protected:true}")
    private boolean isHealthEndpointProtected;

    @Bean
    @Order(1)
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {

        var antMatchersToIgnore = new ArrayList<String>();
        antMatchersToIgnore.add("/cachingservice/application/info");
        antMatchersToIgnore.add("/cachingservice/v3/api-docs");
        if (!isHealthEndpointProtected) {
            antMatchersToIgnore.add("/cachingservice/application/health");
        }

        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .headers(headers -> headers.hsts(ServerHttpSecurity.HeaderSpec.HstsSpec::disable))
            .securityMatcher(new AndServerWebExchangeMatcher(
                ServerWebExchangeMatchers.pathMatchers("/cachingservice/**")
            ))
            .authorizeExchange(exchange -> exchange
                .pathMatchers(antMatchersToIgnore.toArray(new String[0])).permitAll()
                .anyExchange().authenticated()
            ).exceptionHandling(exceptionHandlingSpec ->
                exceptionHandlingSpec.authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.FORBIDDEN))
            );

        if (verifyCertificates || !nonStrictVerifyCerts) {
            http.x509(x509spec -> x509spec.principalExtractor(X509Util.x509PrincipalExtractor())
                .authenticationManager(X509Util.x509ReactiveAuthenticationManager()));
        } else {
            http.authorizeExchange(exchange -> exchange.anyExchange().permitAll());
        }

        return http.build();
    }


    @Bean
    ReactiveUserDetailsService userDetailsService() {

        return username -> {
            List<GrantedAuthority> authorities = new ArrayList<>();
            UserDetails userDetails = User.withUsername(username).authorities(authorities).password("").build();
            return Mono.just(userDetails);
        };
    }
}
