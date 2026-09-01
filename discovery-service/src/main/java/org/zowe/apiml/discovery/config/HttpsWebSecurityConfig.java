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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.core.annotation.Order;
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
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.preauth.x509.X509AuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
public class HttpsWebSecurityConfig extends AbstractWebSecurityConfigurer {

    private final HandlerInitializer handlerInitializer;
    private final AuthConfigurationProperties securityConfigurationProperties;
    private final GatewayLoginProvider gatewayLoginProvider;
    private final GatewayTokenProvider gatewayTokenProvider;
    private static final String DISCOVERY_REALM = "API Mediation Discovery Service realm";
    @Value("${server.attlsServer.enabled:false}")
    private boolean isServerAttlsEnabled;

    @Value("${apiml.security.ssl.verifySslCertificatesOfServices:true}")
    private boolean verifySslCertificatesOfServices;

    @Value("${apiml.security.ssl.nonStrictVerifySslCertificatesOfServices:false}")
    private boolean nonStrictVerifySslCertificatesOfServices;

    @Value("${apiml.metrics.enabled:false}")
    private boolean isMetricsEnabled;

    @Value("${apiml.health.protected:false}")
    private boolean isHealthEndpointProtected;

    @Value("${apiml.discovery.userid:#{null}}")
    private String discoveryUserId;

    @Value("${apiml.discovery.password:#{null}}")
    private char[] discoveryPassword;

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) {
        // we cannot use `auth.inMemoryAuthentication()` because it does not support char array
        auth.authenticationProvider(new EurekaBasicAuthenticationProvider(discoveryUserId, discoveryPassword));
    }

    @Bean
    public WebSecurityCustomizer httpsWebSecurityCustomizer() {
        return web -> {
            web.ignoring().requestMatchers(
                new AntPathRequestMatcher("/eureka/css/**"),
                new AntPathRequestMatcher("/eureka/js/**"),
                new AntPathRequestMatcher("/eureka/fonts/**"),
                new AntPathRequestMatcher("/eureka/images/**"),
                new AntPathRequestMatcher("/application/info"),
                new AntPathRequestMatcher("/favicon.ico")
            );

            if (!isHealthEndpointProtected) {
                web.ignoring().requestMatchers(new AntPathRequestMatcher("/application/health"));
            }

            if (isMetricsEnabled) {
                web.ignoring().requestMatchers(new AntPathRequestMatcher("/application/hystrixstream"));
            }
        };
    }

    /**
     * Filter chain for protecting endpoints with MF credentials (basic or token)
     */
    @Bean
    @Order(3)
    public SecurityFilterChain basicAuthOrTokenFilterChain(HttpSecurity http) throws Exception {
        baseConfigure(http.securityMatchers(matchers -> matchers.requestMatchers(
            new AntPathRequestMatcher("/application/**"),
            new AntPathRequestMatcher("/*")
        )))
            .authenticationProvider(gatewayLoginProvider)
            .authenticationProvider(gatewayTokenProvider)
            .authorizeHttpRequests(requests -> requests
                .requestMatchers(new AntPathRequestMatcher("/**")).authenticated())
            .httpBasic(basic -> basic.realmName(DISCOVERY_REALM));
        if (isServerAttlsEnabled) {
            http.addFilterBefore(new SecureConnectionFilter(), UsernamePasswordAuthenticationFilter.class);
        }

        return http.apply(new CustomSecurityFilters()).and().build();
    }

    /**
     * Filter chain for protecting endpoints with client certificate
     */
    @Bean
    @Order(2)
    public SecurityFilterChain clientCertificateFilterChain(HttpSecurity http) throws Exception {
        baseConfigure(http.securityMatcher("/eureka/**"));
        if (verifySslCertificatesOfServices || !nonStrictVerifySslCertificatesOfServices) {
            http.authorizeHttpRequests(requests -> requests
                .anyRequest().authenticated()).x509(x509 -> x509.userDetailsService(x509UserDetailsService()));
            if (isServerAttlsEnabled) {
                http.addFilterBefore(new AttlsFilter(), X509AuthenticationFilter.class);
                http.addFilterBefore(new SecureConnectionFilter(), AttlsFilter.class);
            }
        } else {
            http.authenticationProvider(new EurekaBasicAuthenticationProvider(discoveryUserId, discoveryPassword))
                .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
                .httpBasic(basic -> basic.realmName(DISCOVERY_REALM));

        }
        return http.build();
    }

    /**
     * Filter chain for protecting endpoints with MF credentials (basic or token) or x509 certificate
     */
    @Bean
    @Order(1)
    public SecurityFilterChain basicAuthOrTokenOrCertFilterChain(HttpSecurity http) throws Exception {
        baseConfigure(http.securityMatcher("/discovery/**"))
            .authenticationProvider(gatewayLoginProvider)
            .authenticationProvider(gatewayTokenProvider)
            .httpBasic(basic -> basic.realmName(DISCOVERY_REALM));
        if (verifySslCertificatesOfServices || !nonStrictVerifySslCertificatesOfServices) {
            http.authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
                .x509(x509 -> x509.userDetailsService(x509UserDetailsService()));
            if (isServerAttlsEnabled) {
                http.addFilterBefore(new AttlsFilter(), X509AuthenticationFilter.class);
                http.addFilterBefore(new SecureConnectionFilter(), AttlsFilter.class);
            }
        } else {
            http.authorizeHttpRequests(requests -> requests.anyRequest().authenticated());
        }

        return http.apply(new CustomSecurityFilters()).and().build();
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

    @Slf4j
    static class EurekaBasicAuthenticationProvider implements AuthenticationProvider {

        private final boolean discoveryCredentialsProvider;
        private final byte[] discoveryUserid;
        private final byte[] discoveryPassword;

        private final MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();

        EurekaBasicAuthenticationProvider(String discoveryUserid, char[] discoveryPassword) {
            this.discoveryUserid = getBytes(discoveryUserid);
            this.discoveryPassword = getBytes(discoveryPassword);

            discoveryCredentialsProvider = (this.discoveryUserid.length > 0) && (this.discoveryPassword.length > 0);
            if (!discoveryCredentialsProvider) {
                log.warn("Eureka credentials are not set. Please configure properties `apiml.discovery.userid` and `apiml.discovery.password` or change type of Eureka authentication.");
            }
        }

        private boolean isValid(Authentication authentication) {
            byte[] userId = getUser(authentication);
            boolean userMatching = MessageDigest.isEqual(userId, discoveryUserid);

            byte[] password = getPassword(authentication);
            boolean passwordMatching = MessageDigest.isEqual(password, discoveryPassword);

            return discoveryCredentialsProvider && userMatching && passwordMatching;
        }

        static byte[] getBytes(Object obj) {
            if (obj == null) {
                return new byte[0];
            }
            if (obj instanceof byte[]) {
                return (byte[]) obj;
            }
            if (obj instanceof char[]) {
                ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap((char[]) obj));
                byte[] bytes = new byte[byteBuffer.limit()];
                byteBuffer.get(bytes);
                return bytes;
            }
            if (obj instanceof String) {
                return ((String) obj).getBytes(StandardCharsets.UTF_8);
            }
            return String.valueOf(obj).getBytes(StandardCharsets.UTF_8);
        }

        private byte[] getPassword(Authentication authentication) {
            return getBytes(authentication.getCredentials());
        }

        private byte[] getUser(Authentication authentication) {
            return getBytes(authentication.getPrincipal());
        }

        @Override
        public Authentication authenticate(Authentication authentication) throws AuthenticationException {
            if (isValid(authentication)) {
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
