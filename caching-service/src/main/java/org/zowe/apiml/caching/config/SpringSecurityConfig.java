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
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.SecurityWebFilterChain;
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

    @Value("${server.attls.enabled:false}")
    private boolean isAttlsEnabled;

    @Value("${apiml.health.protected:true}")
    private boolean isHealthEndpointProtected;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        String[] noSecurityAntMatchers = {
            "/application/info",
            "/v3/api-docs"
        };

        List<String> antMatchersToIgnore = new ArrayList<>(List.of(noSecurityAntMatchers));
        if (!isHealthEndpointProtected) {
            antMatchersToIgnore.add("/application/health");
        }

        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .headers(headers -> headers.hsts(ServerHttpSecurity.HeaderSpec.HstsSpec::disable))
            .authorizeExchange(exchange -> exchange
                .pathMatchers(antMatchersToIgnore.toArray(new String[0])).permitAll()
                .anyExchange().authenticated()
            );

        if (verifyCertificates || !nonStrictVerifyCerts) {
            http.x509(x509spec -> x509spec.principalExtractor(cert -> "cachingUser"));
        } else {
            http.authorizeExchange(exchange -> exchange.anyExchange().permitAll());
        }

        return http.build();
    }


    @Bean
    @Primary
    ReactiveUserDetailsService userDetailsService() {

        return username -> {
            List<GrantedAuthority> authorities = new ArrayList<>();
            UserDetails userDetails = User.withUsername(username).authorities(authorities).password("").build();
            return Mono.just(userDetails);
        };
    }
}
