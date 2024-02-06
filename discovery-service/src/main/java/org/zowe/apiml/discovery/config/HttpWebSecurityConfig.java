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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.zowe.apiml.security.common.config.HandlerInitializer;
import org.zowe.apiml.security.common.content.BasicContentFilter;

import java.util.Arrays;
import java.util.Collections;

/**
 * Main class configuring Spring security for Discovery Service
 * <p>
 * This configuration is applied if "https" Spring profile is not active
 */
@Configuration
@ComponentScan({
    "org.zowe.apiml.security.common",
    "org.zowe.apiml.gateway.security.login"
})
@EnableWebSecurity
@RequiredArgsConstructor
@Profile("!https & !attls")
public class HttpWebSecurityConfig extends AbstractWebSecurityConfigurer {
    private static final String DISCOVERY_REALM = "API Mediation Discovery Service realm";

    @Value("${apiml.discovery.userid:eureka}")
    private String eurekaUserid;

    @Value("${apiml.discovery.password:password}")
    private char[] eurekaPassword;

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) {
        // we cannot use `auth.inMemoryAuthentication()` because it does not support char array
        auth.authenticationProvider(new AuthenticationProvider() {
            private MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();

            @Override
            public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                if (
                    StringUtils.equals(eurekaUserid, String.valueOf(authentication.getPrincipal())) &&
                        authentication.getCredentials() != null
                ) {
                    char[] credentials;
                    if (authentication.getCredentials() instanceof char[]) {
                        credentials = (char[]) authentication.getCredentials();
                    } else {
                        credentials = String.valueOf(authentication.getCredentials()).toCharArray();
                    }

                    if (Arrays.equals(eurekaPassword, credentials)) {
                        UsernamePasswordAuthenticationToken result = UsernamePasswordAuthenticationToken.authenticated(
                            authentication.getPrincipal(),
                            authentication.getCredentials(),
                            Collections.singleton(new SimpleGrantedAuthority("EUREKA"))
                        );
                        result.setDetails(authentication.getDetails());
                        return result;
                    }
                }

                throw new BadCredentialsException(this.messages
                    .getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credentials"));

            }

            @Override
            public boolean supports(Class<?> authentication) {
                return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
            }
        });
    }

    private final HandlerInitializer handlerInitializer;

    @Bean
    public WebSecurityCustomizer httpWebSecurityCustomizer() {
        String[] noSecurityAntMatchers = {
            "/favicon.ico",
            "/eureka/css/**",
            "/eureka/js/**",
            "/eureka/fonts/**",
            "/eureka/images/**"
        };
        return web -> web.ignoring().requestMatchers(noSecurityAntMatchers);
    }

    @Bean
    public SecurityFilterChain httpFilterChain(HttpSecurity http) throws Exception {
        baseConfigure(http)
            .httpBasic(s -> s.realmName(DISCOVERY_REALM))

            .authorizeHttpRequests(
                s -> s.requestMatchers("/application/info", "/application/health").permitAll()
                      .requestMatchers("/**").authenticated()
            );

        return http.with(new CustomSecurityFilters(), t -> { })
                   .build();
    }

    private class CustomSecurityFilters extends AbstractHttpConfigurer<CustomSecurityFilters, HttpSecurity> {
        @Override
        public void configure(HttpSecurity http) {
            AuthenticationManager authenticationManager = http.getSharedObject(AuthenticationManager.class);
            http.addFilterBefore(basicFilter(authenticationManager), UsernamePasswordAuthenticationFilter.class);
        }

        private BasicContentFilter basicFilter(AuthenticationManager authenticationManager) {
            return new BasicContentFilter(authenticationManager, handlerInitializer.getAuthenticationFailureHandler(), handlerInitializer.getResourceAccessExceptionHandler());
        }
    }
}
