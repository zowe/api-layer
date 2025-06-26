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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
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
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher.MatchResult;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.zowe.apiml.filter.BasicLoginFilter;
import org.zowe.apiml.filter.CategorizeCertsWebFilter;
import org.zowe.apiml.filter.LogoutHandler;
import org.zowe.apiml.filter.QueryWebFilter;
import org.zowe.apiml.filter.X509AuthFilter;
import org.zowe.apiml.gateway.filters.security.AuthExceptionHandlerReactive;
import org.zowe.apiml.gateway.filters.security.TokenAuthFilter;
import org.zowe.apiml.security.common.util.X509Util;
import org.zowe.apiml.handler.FailedAuthenticationWebHandler;
import org.zowe.apiml.handler.LocalTokenProvider;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.verify.CertificateValidator;
import org.zowe.apiml.util.HttpUtils;
import org.zowe.apiml.zaas.security.config.CompoundAuthProvider;
import org.zowe.apiml.zaas.security.login.x509.X509AuthenticationProvider;
import org.zowe.apiml.zaas.security.query.TokenAuthenticationProvider;

import java.util.List;
import java.util.Set;

import static org.zowe.apiml.gateway.services.ServicesInfoController.SERVICES_FULL_URL;
import static org.zowe.apiml.gateway.services.ServicesInfoController.SERVICES_SHORT_URL;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private static final String CONTEXT_PATH = String.format("/%s", CoreService.GATEWAY.getServiceId());
    private static final String REGISTRY_PATH = CONTEXT_PATH + "/api/v1/registry";
    private static final String CONFORMANCE_SHORT_URL = CONTEXT_PATH + "/conformance/**";
    private static final String CONFORMANCE_LONG_URL = CONTEXT_PATH + "/api/v1" + "/conformance/**";
    private static final String VALIDATE_SHORT_URL = "gateway/validate";
    private static final String VALIDATE_LONG_URL = "gateway/api/v1/validate";
    private static final String APPLICATION_HEALTH = "/application/health";
    private static final String APPLICATION_INFO = "/application/info";

    private final CompoundAuthProvider compoundAuthProvider;
    private final X509AuthenticationProvider x509AuthenticationProvider;
    private final LocalTokenProvider localTokenProvider;
    private final Set<String> publicKeyCertificatesBase64; // Base64 encoded public keys of APIML certificates
    private final CertificateValidator certificateValidator; // Service for validating certificates
    private final FailedAuthenticationWebHandler failedAuthenticationWebHandler;
    private final TokenAuthenticationProvider tokenAuthenticationProvider;
    private final HttpUtils httpUtils;

    @Value("${apiml.health.protected:true}")
    private boolean isHealthEndpointProtected;

    @Value("${apiml.security.ssl.verifySslCertificatesOfServices:true}")
    private boolean verifySslCertificatesOfServices;

    @Value("${apiml.service.port}")
    private int gatewayPort;

    @Value("${apiml.internal-discovery.port:10011}")
    private int internalDiscoveryPort;

    private static final List<String> UNAUTHENTICATED_PATTERNS = List.of(
    "/application/",
    "/application/version",
    "/eureka/css/**",
    "/eureka/js/**",
    "/eureka/fonts/**",
    "/eureka/images/**",
    APPLICATION_INFO,
    "/favicon.ico");

    private final ServerWebExchangeMatcher discoveryPortMatcher = exchange -> exchange.getRequest().getURI().getPort() == internalDiscoveryPort ? MatchResult.match() : MatchResult.notMatch();
    private final ServerWebExchangeMatcher isInUnauthenticatedPaths = ServerWebExchangeMatchers.pathMatchers(UNAUTHENTICATED_PATTERNS.toArray(new String[]{}));
    private final ServerWebExchangeMatcher notInUnauthenticatedPaths = new NegatedServerWebExchangeMatcher(isInUnauthenticatedPaths);

    @Bean
    SecurityWebFilterChain errorFilterChain(ServerHttpSecurity http) {
        return http
            .securityMatcher(ServerWebExchangeMatchers.pathMatchers("/error"))
            .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
            .build();
    }

    /**
     * This chain is only applicable for the Discovery Service connector in the API ML modulith
     * Secures the /eureka/** API endpoints with an explicit exclusion for /eureka (homepage)
     *
     * @param http
     * @return
     */
    @Bean
    @Order(0)
    SecurityWebFilterChain discoveryServiceClientCertificateFilterChain(ServerHttpSecurity http) {
        http
            .securityMatcher(new AndServerWebExchangeMatcher(
                discoveryPortMatcher,
                ServerWebExchangeMatchers.pathMatchers("/eureka/**"),
                notInUnauthenticatedPaths,
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

    /**
     * This chain is only applicable for the Discovery Service connector in the API ML modulith
     * Secures the /discovery/** API endpoints
     *
     * @param http
     * @param authConfigurationProperties
     * @param authExceptionHandlerReactive
     * @return
     */
    @Bean
    @Order(1)
    SecurityWebFilterChain discoveryServiceBasicAuthOrTokenOrCertFilterChain(ServerHttpSecurity http,
                                                             AuthConfigurationProperties authConfigurationProperties, AuthExceptionHandlerReactive authExceptionHandlerReactive) {
        http
            .securityMatcher(new AndServerWebExchangeMatcher(
                discoveryPortMatcher,
                notInUnauthenticatedPaths,
                ServerWebExchangeMatchers.pathMatchers("/discovery/**")
            ))
            .authorizeExchange(exchange -> exchange.anyExchange().authenticated())
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .addFilterAfter(new TokenAuthFilter(localTokenProvider, authConfigurationProperties, authExceptionHandlerReactive), SecurityWebFiltersOrder.AUTHENTICATION)
            .addFilterAfter(new BasicLoginFilter(compoundAuthProvider, failedAuthenticationWebHandler), SecurityWebFiltersOrder.AUTHENTICATION);

        if (verifySslCertificatesOfServices) {
            return x509SecurityConfig(http).build();
        }
        return http.build();
    }

    /**
     * Set up the default x509 authentication mode. It verifies only trusted certificates such as server certs, without mapping
     */
    private ServerHttpSecurity x509SecurityConfig(ServerHttpSecurity http, boolean defaultExceptionHandler) {
        http
            .headers(customizer -> customizer.frameOptions(ServerHttpSecurity.HeaderSpec.FrameOptionsSpec::disable))
            .x509(x509 -> x509
                .principalExtractor(X509Util.x509PrincipalExtractor())
                .authenticationManager(X509Util.x509ReactiveAuthenticationManager())
            )
            .csrf(ServerHttpSecurity.CsrfSpec::disable);

        if (defaultExceptionHandler) {
            return http.exceptionHandling(exceptionHandlingSpec ->
                exceptionHandlingSpec.authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.FORBIDDEN))
            );
        }
        return http;
    }

    private ServerHttpSecurity x509SecurityConfig(ServerHttpSecurity http) {
        return x509SecurityConfig(http, true);
    }

    /**
     * This chain is only applicable for the Discovery Service connector in the API ML modulith
     *
     * @param http
     * @return
     */
    @Bean
    @Order(9)
    SecurityWebFilterChain discoveryAllowedEndpoints(ServerHttpSecurity http) {
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
                        APPLICATION_INFO,
                        "/favicon.ico"
                    )
                    .permitAll();
                exchange.anyExchange().authenticated();
            });
        return http.build();
    }

    /**
     * Optionally protect the /application/health endpoint due to the information exposed there
     * This one applies independently of connector, it covers /application/health in both GW and DS
     *
     * @param http
     * @param authConfigurationProperties Obtain auth configuration such as auth cookie name
     * @param authExceptionHandlerReactive Exception handler
     * @return The configured {@link SecurityWebFilterChain} to optionally protect /application/health path
     */
    @Bean
    @Order(2)
    SecurityWebFilterChain healthEndpointFilterChain(ServerHttpSecurity http,
                                                   AuthConfigurationProperties authConfigurationProperties,
                                                   AuthExceptionHandlerReactive authExceptionHandlerReactive) {
        http
            .securityMatcher(ServerWebExchangeMatchers.pathMatchers(APPLICATION_HEALTH))
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchange -> {
                if (!isHealthEndpointProtected) {
                    exchange.pathMatchers(APPLICATION_HEALTH).permitAll();
                } else {
                    exchange.anyExchange().authenticated();
                }
            })
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable);
        if (isHealthEndpointProtected) {
            http.addFilterAfter(new TokenAuthFilter(localTokenProvider, authConfigurationProperties, authExceptionHandlerReactive), SecurityWebFiltersOrder.AUTHENTICATION)
                .addFilterAfter(new BasicLoginFilter(compoundAuthProvider, failedAuthenticationWebHandler), SecurityWebFiltersOrder.AUTHENTICATION);
        }
        return http.build();
    }

    /**
     * Security filter chain that protects all endpoints under the path "/application/**",
     * except for "/application/health" - which is handled separately based on the configuration - and "/application/info".
     * <p>
     * This chain requires that all incoming requests to the matched paths are authenticated,
     * either via Basic Authentication or Bearer JWT token.
     * <p>
     * The chain disables relies on custom filters to handle authentication:
     * <ul>
     *     <li>{@link TokenAuthFilter} - Validates JWT tokens</li>
     *     <li>{@link BasicLoginFilter} - Validates Basic Authentication credentials</li>
     * </ul>
     *
     * @param http                         HTTP security configuration
     * @param authConfigurationProperties  authentication configuration properties
     * @param authExceptionHandlerReactive custom handler for authentication failures
     * @return the configured {@link SecurityWebFilterChain} for protecting "/application/**" paths
     */
    @Bean
    SecurityWebFilterChain applicationEndpointsProtected(ServerHttpSecurity http,
                                                         AuthConfigurationProperties authConfigurationProperties,
                                                         AuthExceptionHandlerReactive authExceptionHandlerReactive) {
        return http
            .securityMatcher(new AndServerWebExchangeMatcher(
                ServerWebExchangeMatchers.pathMatchers("/application/**"),
                new NegatedServerWebExchangeMatcher(ServerWebExchangeMatchers.pathMatchers(APPLICATION_HEALTH, APPLICATION_INFO, "/application/version"))
            ))
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchange -> exchange.anyExchange().authenticated())
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .addFilterAfter(new TokenAuthFilter(localTokenProvider, authConfigurationProperties, authExceptionHandlerReactive), SecurityWebFiltersOrder.AUTHENTICATION)
            .addFilterAfter(new BasicLoginFilter(compoundAuthProvider, failedAuthenticationWebHandler), SecurityWebFiltersOrder.AUTHENTICATION)
            .build();

    }

    /**
    * Filter chain for protecting endpoints with MF credentials (basic or token)
    */
    @Bean
    @Order(10)
    SecurityWebFilterChain discoveryBasicAuthOrToken(ServerHttpSecurity http,
                                                     AuthConfigurationProperties authConfigurationProperties, AuthExceptionHandlerReactive authExceptionHandlerReactive) {
        return http
            .securityMatcher(new AndServerWebExchangeMatcher(
                discoveryPortMatcher,
                notInUnauthenticatedPaths
            ))
            .authorizeExchange(exchange -> exchange.anyExchange().authenticated())
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .addFilterAfter(new TokenAuthFilter(localTokenProvider, authConfigurationProperties, authExceptionHandlerReactive), SecurityWebFiltersOrder.AUTHENTICATION) // waiting for the new one not relying on zaas
            .addFilterAfter(new BasicLoginFilter(compoundAuthProvider, failedAuthenticationWebHandler), SecurityWebFiltersOrder.AUTHENTICATION)
            .build();
    }

    /**
     * This filter chain secures the /logout endpoint
     * The /login endpoint is explicitly permitted for clarity
     *
     * @param http
     * @param logoutHandler the logout handler
     * @return
     */
    @Bean
    SecurityWebFilterChain loginFilter(ServerHttpSecurity http, LogoutHandler logoutHandler) {
        var man = new ProviderManager(x509AuthenticationProvider);
        var reactiveX509provider = new ReactiveAuthenticationManagerAdapter(man);
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
            .securityMatcher(new AndServerWebExchangeMatcher(
                ServerWebExchangeMatchers.pathMatchers(HttpMethod.POST, "gateway/api/v1/auth/login", "gateway/api/v1/auth/logout")
            ))
            .authorizeExchange(exchange ->
                exchange.matchers(ServerWebExchangeMatchers.pathMatchers(HttpMethod.POST, "gateway/api/v1/auth/logout")).authenticated()
            )
            .authorizeExchange(exchange ->
                exchange.matchers(ServerWebExchangeMatchers.pathMatchers(HttpMethod.POST, "gateway/api/v1/auth/login")).permitAll()
            )
            .logout(c -> c
                .logoutUrl("/gateway/api/v1/auth/logout")
                .logoutHandler(logoutHandler)
                .logoutSuccessHandler(new HttpStatusReturningServerLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .addFilterAfter(new CategorizeCertsWebFilter(publicKeyCertificatesBase64, certificateValidator), SecurityWebFiltersOrder.FIRST)
            .addFilterAfter(new BasicLoginFilter(compoundAuthProvider, failedAuthenticationWebHandler), SecurityWebFiltersOrder.AUTHENTICATION)
            .addFilterAfter(new X509AuthFilter(reactiveX509provider), SecurityWebFiltersOrder.AUTHENTICATION)
            .build();
    }

    /**
     * This security filter chain secures the /query endpoint
     *
     * @param http
     * @return
     */
    @Bean
    SecurityWebFilterChain queryFilter(ServerHttpSecurity http) {
        var man = new ProviderManager(tokenAuthenticationProvider);
        var reactiveTokenAuthProvider = new ReactiveAuthenticationManagerAdapter(man);

        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
            .securityMatcher(new AndServerWebExchangeMatcher(
                ServerWebExchangeMatchers.pathMatchers("gateway/api/v1/auth/query")
            ))
            .authorizeExchange(exchange -> exchange.anyExchange().authenticated())
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .addFilterAfter(new QueryWebFilter(failedAuthenticationWebHandler, HttpMethod.GET, false, reactiveTokenAuthProvider, httpUtils), SecurityWebFiltersOrder.FIRST)
            .build();
    }

    /**
     * Secures endpoints:
     *   - /auth/access-token/generate
     * <p>
     * Requires authentication by a client certificate or basic authentication, supports credentials in header and body.
     * The request is fulfilled by the filter chain only, there is no controller to handle it.
     * Order of custom filters:
     *   - CategorizeCertsWebFilter - checks for forwarded client certificate and put it into a custom request attribute
     *   - X509AuthFilter - attempts to log in a user using forwarded client certificate, generates access token and stops the chain on success, reply with the token
     *   - ShouldBeAlreadyAuthenticatedFilter - stops filter chain if none of the authentications was successful
     *
     */
    @Bean
    SecurityWebFilterChain accessTokenFilter(ServerHttpSecurity http) {
        var man = new ProviderManager(x509AuthenticationProvider);
        var reactiveX509provider = new ReactiveAuthenticationManagerAdapter(man);
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .securityMatcher(ServerWebExchangeMatchers.pathMatchers(HttpMethod.POST, "/gateway/api/v1/auth/access-token/generate"))
            .authorizeExchange(exchange -> exchange.anyExchange().authenticated())
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .addFilterAfter(new CategorizeCertsWebFilter(publicKeyCertificatesBase64, certificateValidator), SecurityWebFiltersOrder.FIRST)
            .addFilterAfter(new BasicLoginFilter(compoundAuthProvider, failedAuthenticationWebHandler), SecurityWebFiltersOrder.AUTHENTICATION)
            .addFilterAfter(new X509AuthFilter(reactiveX509provider), SecurityWebFiltersOrder.AUTHENTICATION)
            .build();
    }

    /**
     * Secures endpoints:
     *  - /auth/access-token/revoke/tokens/**
     *  - /auth/access-token/evict
     * <p>
     * Requires authentication by a client certificate forwarded form Gateway or basic authentication, supports only credentials in header.
     * Order of custom filters:
     *  - CategorizeCertsWebFilter - checks for forwarded client certificate and put it into a custom request attribute
     *  - X509AuthFilter - attempts to log in using a user using forwarded client certificate, replaces pre-authentication in security context by the authentication result
     *  - BasicLoginFilter - attempts to log in a user using credentials from basic authentication header
     */
    @Bean
    SecurityWebFilterChain revokeTokenFilterChain(ServerHttpSecurity http,
                                                  AuthConfigurationProperties authConfigurationProperties,
                                                  AuthExceptionHandlerReactive authExceptionHandlerReactive) {
        var man = new ProviderManager(x509AuthenticationProvider);
        var reactiveX509provider = new ReactiveAuthenticationManagerAdapter(man);

        return x509SecurityConfig(http)
            .securityMatcher(ServerWebExchangeMatchers.pathMatchers("/gateway/api/v1/auth/access-token/revoke/tokens/**"))
            .authorizeExchange(exchange -> exchange.anyExchange().authenticated())
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .addFilterAfter(new CategorizeCertsWebFilter(publicKeyCertificatesBase64, certificateValidator), SecurityWebFiltersOrder.FIRST)
            .addFilterAfter(new X509AuthFilter(reactiveX509provider), SecurityWebFiltersOrder.AUTHENTICATION)
            .addFilterAfter(new BasicLoginFilter(compoundAuthProvider, failedAuthenticationWebHandler), SecurityWebFiltersOrder.AUTHENTICATION)
            .build();
    }

    /**
     * This security filter chain secures the /refresh access token (PAT) endpoint
     *
     * @param http
     * @return
     */
    @ConditionalOnProperty(name = "apiml.security.allowTokenRefresh", havingValue = "true")
    @Bean
    SecurityWebFilterChain refreshTokenFilter(ServerHttpSecurity http) {
        var man = new ProviderManager(tokenAuthenticationProvider);
        var reactiveTokenAuthProvider = new ReactiveAuthenticationManagerAdapter(man);
        return x509SecurityConfig(http)
            .securityMatcher(new AndServerWebExchangeMatcher(
                ServerWebExchangeMatchers.pathMatchers(HttpMethod.POST, "gateway/api/v1/auth/refresh")
            ))
            .authorizeExchange(exchange -> exchange.anyExchange().authenticated())
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .addFilterAfter(new QueryWebFilter(failedAuthenticationWebHandler, HttpMethod.POST, true, reactiveTokenAuthProvider, httpUtils), SecurityWebFiltersOrder.AUTHENTICATION)
            .build();
    }

    /**
     * This security filter chain secures the /ticket endpoint
     *
     * @param http
     * @return
     */
    @Bean
    SecurityWebFilterChain ticketFilter(ServerHttpSecurity http) {
        var man = new ProviderManager(tokenAuthenticationProvider);
        var reactiveTokenAuthProvider = new ReactiveAuthenticationManagerAdapter(man);
        return x509SecurityConfig(http)
            .securityMatcher(new AndServerWebExchangeMatcher(
                ServerWebExchangeMatchers.pathMatchers("gateway/api/v1/auth/ticket")
            ))
            .authorizeExchange(exchange -> exchange.anyExchange().authenticated())
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .addFilterAfter(new QueryWebFilter(failedAuthenticationWebHandler, HttpMethod.POST, true, reactiveTokenAuthProvider, httpUtils), SecurityWebFiltersOrder.AUTHENTICATION)
            .build();
    }

    /**
     * This security filter chain secures the SAF Resource check endpoint
     *
     * @param http
     * @param authConfigurationProperties
     * @param authExceptionHandlerReactive
     * @return
     */
    @Bean
    SecurityWebFilterChain safResourceCheckFilter(ServerHttpSecurity http,
                                                  AuthConfigurationProperties authConfigurationProperties,
                                                  AuthExceptionHandlerReactive authExceptionHandlerReactive) {
        var man = new ProviderManager(x509AuthenticationProvider);
        var reactiveX509provider = new ReactiveAuthenticationManagerAdapter(man);

        return x509SecurityConfig(http)
            .securityMatcher(new AndServerWebExchangeMatcher(
                ServerWebExchangeMatchers.pathMatchers("gateway/auth/check")
            ))
            .authorizeExchange(exchange -> exchange.anyExchange().authenticated())
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .addFilterAfter(new CategorizeCertsWebFilter(publicKeyCertificatesBase64, certificateValidator), SecurityWebFiltersOrder.FIRST)
            .addFilterAfter(new TokenAuthFilter(localTokenProvider, authConfigurationProperties, authExceptionHandlerReactive), SecurityWebFiltersOrder.AUTHENTICATION)
            .addFilterAfter(new BasicLoginFilter(compoundAuthProvider, failedAuthenticationWebHandler), SecurityWebFiltersOrder.AUTHENTICATION)
            .addFilterAfter(new X509AuthFilter(reactiveX509provider), SecurityWebFiltersOrder.AUTHENTICATION)
            .build();
    }

    /**
     * This security filter chain secures the Gateway's endpoints:
     * /services
     * /registry
     * /conformance
     * /validate
     *
     * @param http
     * @param authConfigurationProperties
     * @param authExceptionHandlerReactive
     * @return
     */
    @Bean
    SecurityWebFilterChain gatewayAuthenticatedEndpoints(ServerHttpSecurity http, AuthConfigurationProperties authConfigurationProperties, AuthExceptionHandlerReactive authExceptionHandlerReactive) {
        return x509SecurityConfig(http, false)
            .securityMatcher(ServerWebExchangeMatchers.pathMatchers(
                REGISTRY_PATH,
                REGISTRY_PATH + "/**",
                SERVICES_SHORT_URL,
                SERVICES_SHORT_URL + "/**",
                SERVICES_FULL_URL,
                SERVICES_FULL_URL + "/**",
                CONFORMANCE_SHORT_URL,
                CONFORMANCE_LONG_URL,
                VALIDATE_SHORT_URL,
                VALIDATE_LONG_URL
            ))
            .authorizeExchange(authorizeExchangeSpec ->
                authorizeExchangeSpec
                    .anyExchange().authenticated()
            )
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .addFilterAfter(new CategorizeCertsWebFilter(publicKeyCertificatesBase64, certificateValidator), SecurityWebFiltersOrder.FIRST)
            .addFilterAfter(new TokenAuthFilter(localTokenProvider, authConfigurationProperties, authExceptionHandlerReactive), SecurityWebFiltersOrder.AUTHENTICATION)
            .addFilterAfter(new BasicLoginFilter(compoundAuthProvider, failedAuthenticationWebHandler), SecurityWebFiltersOrder.AUTHENTICATION)
            .build();
    }

}
