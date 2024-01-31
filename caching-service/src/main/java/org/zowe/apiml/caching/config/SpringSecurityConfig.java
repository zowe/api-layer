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
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.x509.X509AuthenticationFilter;
import org.zowe.apiml.filter.AttlsFilter;
import org.zowe.apiml.filter.SecureConnectionFilter;

import java.util.Collections;

@Configuration
@EnableWebSecurity
public class SpringSecurityConfig {

    @Value("${apiml.service.ssl.verifySslCertificatesOfServices:true}")
    private boolean verifyCertificates;

    @Value("${apiml.service.ssl.nonStrictVerifySslCertificatesOfServices:false}")
    private boolean nonStrictVerifyCerts;

    @Value("${server.attls.enabled:false}")
    private boolean isAttlsEnabled;

    @Value("${apiml.metrics.enabled:false}")
    private boolean isMetricsEnabled;

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        String[] noSecurityAntMatchers = {
            "/application/health",
            "/application/info",
            "/v3/api-docs"
        };

        return web -> {
            web.ignoring().requestMatchers(noSecurityAntMatchers);
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)   // NOSONAR
                .headers(httpSecurityHeadersConfigurer ->
                    httpSecurityHeadersConfigurer.httpStrictTransportSecurity(HeadersConfigurer.HstsConfig::disable))
            .sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (verifyCertificates || !nonStrictVerifyCerts) {
            http.authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
                    .x509(x509 -> x509.userDetailsService(x509UserDetailsService()));
            if (isAttlsEnabled) {
                http.addFilterBefore(new AttlsFilter(), X509AuthenticationFilter.class);
                http.addFilterBefore(new SecureConnectionFilter(), AttlsFilter.class);
            }
        } else {
            http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
        }

        return http.build();
    }

    private UserDetailsService x509UserDetailsService() {
        return username -> new User("cachingUser", "", Collections.emptyList());
    }
}
