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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.ReactiveAuthenticationManagerAdapter;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authentication.logout.HttpStatusReturningServerLogoutSuccessHandler;
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher.MatchResult;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.zowe.apiml.filter.*;
import org.zowe.apiml.gateway.filters.security.AuthExceptionHandlerReactive;
import org.zowe.apiml.gateway.service.BasicAuthProvider;
import org.zowe.apiml.gateway.x509.X509Util;
import org.zowe.apiml.handler.*;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.verify.CertificateValidator;
import org.zowe.apiml.zaas.security.config.CompoundAuthProvider;
import org.zowe.apiml.zaas.security.login.x509.X509AuthenticationProvider;
import org.zowe.apiml.zaas.security.query.TokenAuthenticationProvider;

import java.util.Collections;
import java.util.List;
import java.util.Set;


@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final BasicAuthProvider basicAuthProvider;
    private final CompoundAuthProvider compoundAuthProvider;
    private final X509AuthenticationProvider x509AuthenticationProvider;
    private final LocalTokenProvider localTokenProvider;
    private final Set<String> publicKeyCertificatesBase64; // Base64 encoded public keys of APIML certificates
    private final CertificateValidator certificateValidator; // Service for validating certificates
    private final SuccessQueryHandler successQueryHandler;
    private final SuccessRefreshHandler successRefreshHandler;
    private final SuccessTicketHandler successTicketHandler;
    private final FailedAuthenticationWebHandler failedAuthenticationWebHandler;
    private final TokenAuthenticationProvider tokenAuthenticationProvider;
    private final SuccessfulPersonalAccessTokenHandler successfulAccessTokenHandler;

    @Value("${apiml.health.protected:true}")
    private boolean isHealthEndpointProtected;

    @Value("${apiml.security.ssl.verifySslCertificatesOfServices:true}")
    private boolean verifySslCertificatesOfServices;

    @Value("${apiml.internal-discovery.port:10011}")
    private int internalDiscoveryPort;

    private static final List<String> UNAUTHENTICATED_PATTERNS = List.of("/application/**",
    "/eureka/css/**",
    "/eureka/js/**",
    "/eureka/fonts/**",
    "/eureka/images/**",
    "/application/info",
    "/favicon.ico");

    private final ServerWebExchangeMatcher discoveryPortMatcher = exchange -> exchange.getRequest().getURI().getPort() == internalDiscoveryPort ? MatchResult.match() : MatchResult.notMatch();
    private final ServerWebExchangeMatcher isInUnauthenticatedPaths = ServerWebExchangeMatchers.pathMatchers(UNAUTHENTICATED_PATTERNS.toArray(new String[]{}));

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
            .securityMatcher(new AndServerWebExchangeMatcher(
                discoveryPortMatcher,
                ServerWebExchangeMatchers.pathMatchers("/eureka/**"),
                exchange -> exchange.getRequest().getURI().getPath().startsWith("/eureka/") ? MatchResult.match() : MatchResult.notMatch() // Prevents matching /eureka (mapping for homepage in modulith)
            ))
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

    @Bean
    public SecurityWebFilterChain basicAuthOrTokenOrCertFilterChain(ServerHttpSecurity http,
                                                                    ObjectMapper mapper,
                                                                    AuthConfigurationProperties authConfigurationProperties, AuthExceptionHandlerReactive authExceptionHandlerReactive) {
        http
            .securityMatcher(new AndServerWebExchangeMatcher(
                discoveryPortMatcher,
                ServerWebExchangeMatchers.pathMatchers("/discovery/**")
            ))
            .authorizeExchange(exchange -> exchange.anyExchange().authenticated())
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .addFilterAfter(new TokenAuthenticationFilter(localTokenProvider, authConfigurationProperties, authExceptionHandlerReactive), SecurityWebFiltersOrder.AUTHENTICATION) // waiting for the new one not relying on zaas
            .addFilterAfter(new BasicLoginFilter(compoundAuthProvider, mapper, failedAuthenticationWebHandler), SecurityWebFiltersOrder.AUTHENTICATION);

        if (verifySslCertificatesOfServices) {
            return x509SecurityConfig(http).build();
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
    public SecurityWebFilterChain allowedEndpoints(ServerHttpSecurity http) {
        http
            .securityMatcher(new AndServerWebExchangeMatcher(
                discoveryPortMatcher,
                isInUnauthenticatedPaths
            ))
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
    public SecurityWebFilterChain discoveryBasicAuthOrToken(ServerHttpSecurity http,
                                                            ObjectMapper mapper,
                                                            AuthConfigurationProperties authConfigurationProperties, AuthExceptionHandlerReactive authExceptionHandlerReactive) {
        return http
            .securityMatcher(new AndServerWebExchangeMatcher(
                discoveryPortMatcher
            ))
            .authorizeExchange(exchange -> exchange.anyExchange().authenticated())
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .addFilterAfter(new TokenAuthenticationFilter(localTokenProvider, authConfigurationProperties, authExceptionHandlerReactive), SecurityWebFiltersOrder.AUTHENTICATION) // waiting for the new one not relying on zaas
            .addFilterAfter(new BasicLoginFilter(compoundAuthProvider, mapper, failedAuthenticationWebHandler), SecurityWebFiltersOrder.AUTHENTICATION)
            .build();
    }

    @Bean
    public SecurityWebFilterChain loginFilter(ServerHttpSecurity http, ObjectMapper mapper, LogoutHandler logoutHandler) {
        var man = new ProviderManager(x509AuthenticationProvider);
        var reactiveX509provider = new ReactiveAuthenticationManagerAdapter(man);
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
            .securityMatcher(new AndServerWebExchangeMatcher(
                                   ServerWebExchangeMatchers.pathMatchers( HttpMethod.POST,"gateway/api/v1/auth/login", "gateway/api/v1/auth/logout")
                            ))
            .authorizeExchange(exchange ->
                exchange.anyExchange().authenticated()
            )

            .logout((c) -> c.logoutUrl("/gateway/api/v1/auth/logout").logoutHandler(logoutHandler).logoutSuccessHandler(new HttpStatusReturningServerLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .addFilterAfter(new CategorizeCertsWebFilter(publicKeyCertificatesBase64, certificateValidator),SecurityWebFiltersOrder.FIRST)
            .addFilterAfter(new BasicLoginFilter(compoundAuthProvider, mapper, failedAuthenticationWebHandler), SecurityWebFiltersOrder.AUTHENTICATION)
            .addFilterAfter(new X509AuthFilter(reactiveX509provider), SecurityWebFiltersOrder.AUTHENTICATION)
            .build();

    }

    private boolean isNotAllowed(ServerHttpRequest request, String path) {
        return request.getPath().value().equals(path) && !HttpMethod.POST.equals(request.getMethod());
    }
    @Bean
    public SecurityWebFilterChain queryFilter(ServerHttpSecurity http) {
        var man = new ProviderManager(tokenAuthenticationProvider);
        var reactiveTokenAuthProvider = new ReactiveAuthenticationManagerAdapter(man);
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
            .securityMatcher(new AndServerWebExchangeMatcher(
                                    ServerWebExchangeMatchers.pathMatchers("gateway/api/v1/auth/query")
                            ))
            .authorizeExchange(exchange -> exchange.anyExchange().authenticated())
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .addFilterAfter(new QueryWebFilter(successQueryHandler, failedAuthenticationWebHandler, HttpMethod.GET, false, reactiveTokenAuthProvider), SecurityWebFiltersOrder.FIRST)
             .build();

    }

    @Bean
    public SecurityWebFilterChain accessTokenFilter(ServerHttpSecurity http,
                                                    ObjectMapper mapper) {
        // TODO return ZWEAT606E in case of no scopes/validity passed.
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .securityMatcher(ServerWebExchangeMatchers.pathMatchers("/gateway/api/v1/auth/access-token/generate"))
            .authorizeExchange(exchange -> exchange.anyExchange().authenticated())
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .addFilterAfter(new CategorizeCertsWebFilter(publicKeyCertificatesBase64, certificateValidator), SecurityWebFiltersOrder.FIRST)
            .addFilterAfter(new StoreAccessTokenInfoWebFilter(failedAuthenticationWebHandler, mapper), SecurityWebFiltersOrder.AUTHENTICATION)
            .addFilterAfter(new BasicLoginFilterForPatEndpoint(compoundAuthProvider, mapper, successfulAccessTokenHandler, failedAuthenticationWebHandler), SecurityWebFiltersOrder.AUTHENTICATION)
            .build();
    }

    @Bean
    public SecurityWebFilterChain revokeTokenFilterChain(ServerHttpSecurity http,
                                                         ObjectMapper mapper,
                                                         AuthConfigurationProperties authConfigurationProperties,
                                                         AuthExceptionHandlerReactive authExceptionHandlerReactive) {

        return x509SecurityConfig(http)
            .securityMatcher(ServerWebExchangeMatchers.pathMatchers("/gateway/api/v1/auth/access-token/revoke/tokens/**"))
            .authorizeExchange(exchange -> exchange.anyExchange().authenticated())
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .addFilterAfter(new CategorizeCertsWebFilter(publicKeyCertificatesBase64, certificateValidator), SecurityWebFiltersOrder.FIRST)
            .addFilterAfter(new BasicLoginFilter(compoundAuthProvider, mapper, failedAuthenticationWebHandler), SecurityWebFiltersOrder.AUTHENTICATION)
//            .addFilterAfter(new TokenAuthenticationFilter(localTokenProvider, authConfigurationProperties, authExceptionHandlerReactive), SecurityWebFiltersOrder.AUTHENTICATION)
//            .addFilterAfter(new BasicAuthFilter(basicAuthProvider), SecurityWebFiltersOrder.AUTHENTICATION)
            .build();
    }

    @ConditionalOnProperty(name = "apiml.security.allowTokenRefresh", havingValue = "true")
    @Bean
    public SecurityWebFilterChain refreshTokenFilter(ServerHttpSecurity http) {
        var man = new ProviderManager(tokenAuthenticationProvider);
        var reactiveTokenAuthProvider = new ReactiveAuthenticationManagerAdapter(man);
        return x509SecurityConfig(http)
            .securityMatcher(new AndServerWebExchangeMatcher(
                                    ServerWebExchangeMatchers.pathMatchers("gateway/api/v1/auth/refresh")
                            ))
            .authorizeExchange(exchange -> exchange.anyExchange().authenticated())
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .addFilterAfter(new QueryWebFilter(successRefreshHandler,failedAuthenticationWebHandler, HttpMethod.GET, true, reactiveTokenAuthProvider),SecurityWebFiltersOrder.AUTHENTICATION)
             .build();

    }


    @Bean
    public SecurityWebFilterChain ticketFilter(ServerHttpSecurity http) {
        var man = new ProviderManager(tokenAuthenticationProvider);
        var reactiveTokenAuthProvider = new ReactiveAuthenticationManagerAdapter(man);
        return x509SecurityConfig(http)
            .securityMatcher(new AndServerWebExchangeMatcher(
                                    ServerWebExchangeMatchers.pathMatchers("gateway/api/v1/auth/ticket")
                            ))
            .authorizeExchange(exchange -> exchange.anyExchange().authenticated())
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .addFilterAfter(new QueryWebFilter(successTicketHandler,failedAuthenticationWebHandler, HttpMethod.POST, true, reactiveTokenAuthProvider),SecurityWebFiltersOrder.AUTHENTICATION)
             .build();

    }



}


