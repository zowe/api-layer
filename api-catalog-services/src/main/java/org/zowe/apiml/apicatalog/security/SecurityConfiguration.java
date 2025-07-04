/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.WebFilter;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.security.client.EnableApimlAuth;
import org.zowe.apiml.security.client.service.GatewaySecurityService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.config.SafSecurityConfigurationProperties;
import org.zowe.apiml.security.common.error.AuthExceptionHandler;
import org.zowe.apiml.security.common.filter.CategorizeCertsFilter;
import org.zowe.apiml.security.common.login.LoginFilter;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.security.common.util.X509Util;
import org.zowe.apiml.security.common.verify.CertificateValidator;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static org.zowe.apiml.constants.ApimlConstants.HEADER_OIDC_TOKEN;
import static org.zowe.apiml.security.common.token.TokenAuthentication.createAuthenticated;

/**
 * Main configuration class of Spring web security for Api Catalog
 * binds authentication managers
 * configures ignores for static content
 * adds endpoints and secures them
 * adds security filters
 */
@Slf4j
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
@EnableApimlAuth
@EnableReactiveMethodSecurity
@EnableConfigurationProperties(SafSecurityConfigurationProperties.class)
public class SecurityConfiguration {

    private static final String APIDOC_ROUTES = "/apicatalog/apidoc/**";
    private static final String STATIC_REFRESH_ROUTE = "/apicatalog/static-api/refresh";

    private final AuthConfigurationProperties authConfigurationProperties;
    private final CertificateValidator certificateValidator;
    private final AuthExceptionHandler authExceptionHandler;
    @Qualifier("publicKeyCertificatesBase64")
    private final Set<String> publicKeyCertificatesBase64;
    @Value("${apiml.health.protected:true}")
    private boolean isHealthEndpointProtected;

    /**
     * Filter chain for protecting /apidoc/** endpoints with MF credentials for client certificate.
     */
    @Configuration
    @Order(1)
    public class FilterChainBasicAuthOrTokenOrCertForApiDoc {

        @Value("${apiml.security.ssl.verifySslCertificatesOfServices:true}")
        private boolean verifySslCertificatesOfServices;

        @Value("${apiml.security.ssl.nonStrictVerifySslCertificatesOfServices:false}")
        private boolean nonStrictVerifySslCertificatesOfServices;

        @Bean
        public SecurityWebFilterChain basicAuthOrTokenOrCertApiDocFilterChain(
            ServerHttpSecurity http,
            @Qualifier("basicAuthenticationFilter") WebFilter basicAuthenticationFilter,
            @Qualifier("tokenAuthenticationFilter") WebFilter tokenAuthenticationFilter,
            @Qualifier("oidcAuthenticationFilter") WebFilter oidcAuthenticationFilter
        ) {
            mainframeCredentialsConfiguration(
                baseConfiguration(http.securityMatcher(ServerWebExchangeMatchers.pathMatchers(APIDOC_ROUTES, STATIC_REFRESH_ROUTE))),
                basicAuthenticationFilter, tokenAuthenticationFilter, oidcAuthenticationFilter
            )
            .authorizeExchange(exchange -> exchange.anyExchange().authenticated());

            if (verifySslCertificatesOfServices || !nonStrictVerifySslCertificatesOfServices) {
                http.x509(x509 -> x509
                    .principalExtractor(X509Util.x509PrincipalExtractor())
                    .authenticationManager(X509Util.x509ReactiveAuthenticationManager())
                );
            }

            return http.build();
        }

    }

    /**
     * Default filter chain to protect all routes with MF credentials.
     */
    @Configuration
    @Order(2)
    public class FilterChainBasicAuthOrTokenAllEndpoints {

        @Bean
        public WebSecurityCustomizer webSecurityCustomizer() {
            String[] noSecurityAntMatchers = {
                "/",
                "/apicatalog",
                "/apicatalog/",
                "/apicatalog/static/**",
                "/apicatalog/favicon.ico",
                "/apicatalog/v3/api-docs",
                "/apicatalog/index.html",
                "/apicatalog/application/info",
                "/apicatalog/oidc/provider"
            };
            return web -> web.ignoring().requestMatchers(noSecurityAntMatchers);
        }

        @Bean
        public SecurityWebFilterChain basicAuthOrTokenAllEndpointsFilterChain(
            ServerHttpSecurity http,
            @Qualifier("basicAuthenticationFilter") WebFilter basicAuthenticationFilter,
            @Qualifier("tokenAuthenticationFilter") WebFilter tokenAuthenticationFilter,
            @Qualifier("oidcAuthenticationFilter") WebFilter oidcAuthenticationFilter
        ) {
            if (isHealthEndpointProtected) {
                http.authorizeExchange(exchange -> exchange
                    .pathMatchers("/apicatalog/application/health").authenticated());
            } else {
                http.authorizeExchange(exchange -> exchange
                    .pathMatchers("/apicatalog/application/health").permitAll());
            }

            mainframeCredentialsConfiguration(
                    baseConfiguration(http.securityMatcher(ServerWebExchangeMatchers.pathMatchers(
                        "/apicatalog/static-api/**", "/apicatalog/containers/**", "/apicatalog/application/**", "/apicatalog/services/**", APIDOC_ROUTES))
                    ),
                    basicAuthenticationFilter, tokenAuthenticationFilter, oidcAuthenticationFilter
                )
                .authorizeExchange(exchange -> exchange
                    .anyExchange().authenticated()
                );
            return http.build();
        }
    }

    private ServerHttpSecurity baseConfiguration(ServerHttpSecurity http) {
        var antMatcher = new AntPathMatcher();
        var mapper = new ObjectMapper();

        http.csrf(ServerHttpSecurity.CsrfSpec::disable)

            .headers(httpSecurityHeadersConfigurer ->
                httpSecurityHeadersConfigurer.hsts(ServerHttpSecurity.HeaderSpec.HstsSpec::disable)
                    .frameOptions(ServerHttpSecurity.HeaderSpec.FrameOptionsSpec::disable))

            .exceptionHandling(exceptionHandlingSpec -> exceptionHandlingSpec
                .authenticationEntryPoint((exchange, exception) -> {
                    String requestedUri = exchange.getRequest().getURI().toString();
                    log.debug("Unauthorized access to '{}' endpoint", requestedUri);

                    var response = exchange.getResponse();

                    var addHeader = (BiConsumer<String, String>) response.getHeaders()::add;

                    if (Stream.of(
                            "/apicatalog/application/**",
                            APIDOC_ROUTES,
                            STATIC_REFRESH_ROUTE
                        ).noneMatch(pattern -> antMatcher.match(pattern, requestedUri))
                    ) {
                        addHeader.accept(HttpHeaders.WWW_AUTHENTICATE, ApimlConstants.BASIC_AUTHENTICATION_PREFIX);
                    }

                    AtomicReference<byte[]> responseJson = new AtomicReference<>();
                    BiConsumer<ApiMessageView, HttpStatus> consumer = (message, status) -> {
                        response.setStatusCode(status);
                        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

                        try {
                            responseJson.set(mapper.writeValueAsBytes(message));
                        } catch (JsonProcessingException e) {
                            log.error("Cannot serialize response message");
                        }
                    };

                    try {
                        authExceptionHandler.handleException(requestedUri, consumer, addHeader, exception);

                        var buffer = exchange.getResponse().bufferFactory().wrap(responseJson.get());
                        return exchange.getResponse().writeWith(Mono.just(buffer));
                    } catch (ServletException e) {
                        log.error("Cannot handle exception: {}", exception, e);
                        return Mono.error(() -> new RuntimeException(e));
                    }
                })
            )
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance());

        return http;
    }

    private String[] getAllUrl(String baseUrl) {
        return new String[] {
            "/apicatalog" + baseUrl,
            "/apicatalog/api/v1" + baseUrl
        };
    }

    private ServerHttpSecurity mainframeCredentialsConfiguration(ServerHttpSecurity http, WebFilter...webFiltersAuthorization) {
        http.securityMatcher(ServerWebExchangeMatchers.pathMatchers(HttpMethod.POST, getAllUrl(authConfigurationProperties.getServiceLoginEndpoint())))
        // login endpoint
            .authorizeExchange(exchange -> exchange
                .pathMatchers(HttpMethod.POST, getAllUrl(authConfigurationProperties.getServiceLoginEndpoint())).permitAll());

        http.securityMatcher(ServerWebExchangeMatchers.pathMatchers(HttpMethod.POST, getAllUrl(authConfigurationProperties.getServiceLogoutEndpoint())))
        // logout endpoint
            .logout(logout -> logout
                // logoutUrl for multiple URLs
                .requiresLogout(ServerWebExchangeMatchers.pathMatchers(HttpMethod.POST, getAllUrl(authConfigurationProperties.getServiceLogoutEndpoint())))
                .logoutSuccessHandler(new ApiCatalogLogoutSuccessHandler(authConfigurationProperties)
            )
        );

        Stream.of(webFiltersAuthorization).forEach(webFilter -> http.addFilterBefore(webFilter, SecurityWebFiltersOrder.AUTHENTICATION));

        return http;
    }

    @Bean
    public WebFilter basicAuthenticationFilter(
        GatewaySecurityService gatewaySecurityService,
        AuthConfigurationProperties authConfigurationProperties
    ) {
        AuthConfigurationProperties.CookieProperties cp = authConfigurationProperties.getCookieProperties();
        int cookieMaxAge = (cp.getCookieMaxAge() != null) ? cp.getCookieMaxAge() : -1;

        return (exchange, chain) -> chain.filter(exchange)
            .contextWrite(context -> {
                var authorizationHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                return LoginFilter.getCredentialFromAuthorizationHeader(Optional.ofNullable(authorizationHeader)).map(login -> {
                    try {
                        return gatewaySecurityService.login(login.getUsername(), login.getPassword(), null).map(token -> {
                            exchange.getResponse().addCookie(ResponseCookie.from(cp.getCookieName(), token)
                                .path(cp.getCookiePath())
                                .sameSite(cp.getCookieSameSite().getValue())
                                .maxAge(cookieMaxAge)
                                .httpOnly(true)
                                .secure(cp.isCookieSecure())
                                .build()
                            );

                            return ReactiveSecurityContextHolder.withAuthentication(
                                createAuthenticated(login.getUsername(), token, TokenAuthentication.Type.JWT));
                        }).orElse(context);
                    } catch (Exception e) {
                        log.debug("Cannot verify basic auth", e);
                        return context;
                    }
                }).orElse(context);
            });
    }

    @Bean
    public WebFilter tokenAuthenticationFilter(
        GatewaySecurityService gatewaySecurityService,
        AuthConfigurationProperties authConfigurationProperties
    ) {
        AuthConfigurationProperties.CookieProperties cp = authConfigurationProperties.getCookieProperties();

        return (exchange, chain) -> chain.filter(exchange)
            .contextWrite(context ->
                Optional.ofNullable(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                    .filter(header -> StringUtils.startsWith(header, "Bearer "))
                    .map(header -> header.substring("Bearer ".length()))
                    .map(String::trim)
                    .or(() -> Optional.ofNullable(exchange.getRequest().getCookies().getFirst(cp.getCookieName()))
                        .map(HttpCookie::getValue)
                    )
                    .map(token -> {
                        try {
                            return Map.entry(token, gatewaySecurityService.query(token));
                        } catch (Exception e) {
                            log.debug("Cannot query token: {}", token, e);
                            return null;
                        }
                    })
                    .map(pair -> ReactiveSecurityContextHolder.withAuthentication(
                        createAuthenticated(pair.getValue().getUserId(), pair.getKey(), TokenAuthentication.Type.JWT)
                    ))
                    .orElse(context)
            );
    }

    @Bean
    public WebFilter oidcAuthenticationFilter(
        GatewaySecurityService gatewaySecurityService
    ) {
        return (exchange, chain) -> chain.filter(exchange)
            .contextWrite(context ->
                Optional.ofNullable(exchange.getRequest().getHeaders().getFirst(HEADER_OIDC_TOKEN))
                    .map(token -> {
                        try {
                            return Map.entry(token, gatewaySecurityService.verifyOidc(token));
                        } catch (Exception e) {
                            log.debug("Cannot verify OIDC token: {}", token, e);
                            return null;
                        }
                    })
                    .map(pair -> ReactiveSecurityContextHolder.withAuthentication(
                        createAuthenticated(pair.getValue().getUserId(), pair.getKey(), TokenAuthentication.Type.OIDC)
                    ))
                    .orElse(context)
            );
    }

}
