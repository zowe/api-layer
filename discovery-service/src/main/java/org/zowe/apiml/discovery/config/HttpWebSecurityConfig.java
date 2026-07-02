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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
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
@Slf4j
@Configuration
@ComponentScan({
    "org.zowe.apiml.security.common",
    "org.zowe.apiml.gateway.security.login"
})
@EnableWebSecurity
@RequiredArgsConstructor
@Profile({"!https", "!attlsServer", "!attlsClient"})
public class HttpWebSecurityConfig extends AbstractWebSecurityConfigurer {
    private static final String DISCOVERY_REALM = "API Mediation Discovery Service realm";

    @Value("${apiml.discovery.userid:#{null}}")
    private String discoveryUserId;

    @Value("${apiml.discovery.password:#{null}}")
    private char[] discoveryPassword;

    @Value("${apiml.metrics.enabled:false}")
    private boolean isMetricsEnabled;

    @Value("${apiml.health.protected:false}")
    private boolean isHealthEndpointProtected;

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) {
        // we cannot use `auth.inMemoryAuthentication()` because it does not support char array
        auth.authenticationProvider(new EurekaBasicAuthenticationProvider(discoveryUserId, discoveryPassword));
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

        if (!isHealthEndpointProtected) {
            http.authorizeHttpRequests(requests -> requests
                .requestMatchers("/application/health").permitAll());
        }

        baseConfigure(http)
                .httpBasic(basic -> basic.realmName(DISCOVERY_REALM))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/application/info").permitAll()
                        .requestMatchers("/**").authenticated());

        if (isMetricsEnabled) {
            http.authorizeHttpRequests(requests -> requests.requestMatchers("/application/hystrixstream").permitAll());
        }

        return http.apply(new CustomSecurityFilters()).and().build();
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

    @RequiredArgsConstructor
    static class EurekaBasicAuthenticationProvider implements AuthenticationProvider {

        private final String eurekaUserid;
        private final char[] eurekaPassword;

        private final MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();

        private boolean isCredentialsSet() {
            if (!StringUtils.isEmpty(eurekaUserid) && !ArrayUtils.isEmpty(eurekaPassword)) {
                return true;
            }

            log.warn("Eureka credentials are not set. Please configure properties `apiml.discovery.userid` and `apiml.discovery.password` or change type of Eureka authentication.");
            return false;
        }

        private char[] getPassword(Authentication authentication) {
            if (authentication.getCredentials() instanceof char[]) {
                return (char[]) authentication.getCredentials();
            }
            return String.valueOf(authentication.getCredentials()).toCharArray();
        }

        private String getUser(Authentication authentication) {
            if (authentication.getCredentials() == null) {
                return null;
            }
            return String.valueOf(authentication.getPrincipal());
        }

        @Override
        public Authentication authenticate(Authentication authentication) throws AuthenticationException {
            if (
                isCredentialsSet() &&
                    Strings.CS.equals(eurekaUserid, getUser(authentication)) &&
                    Arrays.equals(eurekaPassword, getPassword(authentication))
            ) {
                UsernamePasswordAuthenticationToken result = UsernamePasswordAuthenticationToken.authenticated(
                    authentication.getPrincipal(),
                    authentication.getCredentials(),
                    Collections.singleton(new SimpleGrantedAuthority("EUREKA"))
                );
                result.setDetails(authentication.getDetails());
                return result;
            }

            throw new BadCredentialsException(this.messages
                .getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credentials"));
        }

        @Override
        public boolean supports(Class<?> authentication) {
            return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
        }

    }

}
