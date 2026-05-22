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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SpringSecurityConfig {

    @Value("${apiml.service.http.userId:#{null}}")
    private String cachingServiceUserId;

    @Value("${apiml.service.http.password:#{null}}")
    private char[] cachingServicePassword;

    @Value("${apiml.service.ssl.verifySslCertificatesOfServices:true}")
    private boolean verifyCertificates;

    @Value("${apiml.health.protected:true}")
    private boolean isHealthEndpointProtected;

    @Bean
    @Order(1)
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {

        var antMatchersToIgnore = new ArrayList<String>();
        antMatchersToIgnore.add("/cachingservice/application/info");
        antMatchersToIgnore.add("/cachingservice/application/eurekaversion");
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
            .exceptionHandling(exceptionHandlingSpec ->
                exceptionHandlingSpec.authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.FORBIDDEN))
            );

        http.authorizeExchange(exchange -> exchange
            .pathMatchers(antMatchersToIgnore.toArray(new String[0])).permitAll()
            .anyExchange().authenticated());

        if (verifyCertificates) {
            http.x509(x509spec -> x509spec.principalExtractor(X509Util.x509PrincipalExtractor())
                .authenticationManager(X509Util.x509ReactiveAuthenticationManager()));
        } else {
            http.httpBasic(httpBasicSpec -> httpBasicSpec.authenticationManager(
                new BasicAuthenticationManager(cachingServiceUserId, cachingServicePassword)));
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

    @RequiredArgsConstructor
    @Slf4j
    static class BasicAuthenticationManager implements ReactiveAuthenticationManager {

        private final String cachingServiceUserId;
        private final char[] cachingServicePassword;
        private final MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();

        private boolean isCredentialsSet() {
            if (!StringUtils.isEmpty(cachingServiceUserId) && !ArrayUtils.isEmpty(cachingServicePassword)) {
                return true;
            }

            log.warn("Caching-service credentials are not set. Please configure properties `apiml.service.http.userid` and `apiml.service.http.password`.");
            return false;
        }

        private char[] getPassword(Authentication authentication) {
            if (authentication.getCredentials() instanceof char[]) {
                return (char[]) authentication.getCredentials();
            }
            return String.valueOf(authentication.getCredentials()).toCharArray();
        }

        @Override
        public Mono<Authentication> authenticate(Authentication authentication) {
            String username = authentication.getName();
            char[] password = getPassword(authentication);

            if (isCredentialsSet() && Strings.CS.equals(cachingServiceUserId, username) &&
                Arrays.equals(cachingServicePassword, password)) {
                // Return an authenticated token with a default role
                return Mono.just(new UsernamePasswordAuthenticationToken(username, password,
                    Collections.singletonList(new SimpleGrantedAuthority("CACHING_SERVICE"))));
            }

            // Reject anything else
            return Mono.error(new BadCredentialsException(this.messages
                .getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credentials")));
        }
    }
}
