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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.WebFilter;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.security.client.EnableApimlAuth;
import org.zowe.apiml.security.client.service.GatewaySecurityService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.config.SafSecurityConfigurationProperties;
import org.zowe.apiml.security.common.login.LoginFilter;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.security.common.util.X509Util;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.apache.hc.core5.http.HttpStatus.SC_UNAUTHORIZED;
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

    private static final String APIDOC_ROUTES = "/apidoc/**";
    private static final String STATIC_REFRESH_ROUTE = "/static-api/refresh";

    private final AuthConfigurationProperties authConfigurationProperties;

    @Value("${apiml.security.ssl.verifySslCertificatesOfServices:true}")
    private boolean verifySslCertificatesOfServices;

    @Value("${apiml.security.ssl.nonStrictVerifySslCertificatesOfServices:false}")
    private boolean nonStrictVerifySslCertificatesOfServices;

    private String[] getFullUrls(String...baseUrl) {
        return Stream.of(
                Arrays.stream(baseUrl).map(url -> "/apicatalog" + url),
                Arrays.stream(baseUrl).map(url -> "/apicatalog/api/v1" + url)
            )
            .flatMap(Function.identity())
            .toArray(String[]::new);
    }

    @Bean
    @Order(1)
    public SecurityWebFilterChain loginSecurityWebFilterChain(ServerHttpSecurity http, ServerAuthenticationEntryPoint serverAuthenticationEntryPoint) {
        return baseConfiguration(http, serverAuthenticationEntryPoint)
            .securityMatcher(ServerWebExchangeMatchers.pathMatchers(HttpMethod.POST, getFullUrls(authConfigurationProperties.getServiceLoginEndpoint())))
            .authorizeExchange(exchange -> exchange
                .pathMatchers(HttpMethod.POST, getFullUrls(authConfigurationProperties.getServiceLoginEndpoint())).permitAll()
            )
            .build();
    }

    @Bean
    @Order(2)
    public SecurityWebFilterChain logoutSecurityWebFilterChain(ServerHttpSecurity http, ServerAuthenticationEntryPoint serverAuthenticationEntryPoint) {
        return baseConfiguration(http, serverAuthenticationEntryPoint)
            .securityMatcher(ServerWebExchangeMatchers.pathMatchers(HttpMethod.POST, getFullUrls(authConfigurationProperties.getServiceLogoutEndpoint())))
            .logout(logout -> logout
                .requiresLogout(ServerWebExchangeMatchers.pathMatchers(HttpMethod.POST, getFullUrls(authConfigurationProperties.getServiceLogoutEndpoint())))
                .logoutSuccessHandler(new ApiCatalogLogoutSuccessHandler(authConfigurationProperties))
            )
            .build();
    }

    /**
     * Filter chain for protecting /apidoc/** endpoints with MF credentials for client certificate.
     */
    @Bean
    @Order(3)
    public SecurityWebFilterChain basicAuthOrTokenOrCertApiDocFilterChain(
        ServerHttpSecurity http,
        @Qualifier("basicAuthenticationFilter") WebFilter basicAuthenticationFilter,
        @Qualifier("tokenAuthenticationFilter") WebFilter tokenAuthenticationFilter,
        @Qualifier("oidcAuthenticationFilter") WebFilter oidcAuthenticationFilter,
        ServerAuthenticationEntryPoint serverAuthenticationEntryPoint
    ) {
        baseConfiguration(
            http.securityMatcher(ServerWebExchangeMatchers.pathMatchers(getFullUrls(APIDOC_ROUTES, STATIC_REFRESH_ROUTE))),
            serverAuthenticationEntryPoint,
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

    @Bean
    @Order(4)
    public SecurityWebFilterChain healthEndpointSecurityWebFilterChain(
        ServerHttpSecurity http,
        @Value("${apiml.health.protected:true}") boolean isHealthEndpointProtected,
        @Qualifier("basicAuthenticationFilter") WebFilter basicAuthenticationFilter,
        @Qualifier("tokenAuthenticationFilter") WebFilter tokenAuthenticationFilter,
        @Qualifier("oidcAuthenticationFilter") WebFilter oidcAuthenticationFilter,
        ServerAuthenticationEntryPoint serverAuthenticationEntryPoint
    ) {
        http = baseConfiguration(
            http.securityMatcher(ServerWebExchangeMatchers.pathMatchers(getFullUrls("/application/health"))),
            serverAuthenticationEntryPoint,
            basicAuthenticationFilter, tokenAuthenticationFilter, oidcAuthenticationFilter
        );

        if (isHealthEndpointProtected) {
            http.authorizeExchange(exchange -> exchange
                .pathMatchers(getFullUrls("/application/health")).authenticated());
        } else {
            http.authorizeExchange(exchange -> exchange
                .pathMatchers(getFullUrls("/application/health")).permitAll());
        }

        return http.build();
    }

    @Bean
    @Order(5)
    public SecurityWebFilterChain basicAuthOrTokenAllEndpointsFilterChain(
        ServerHttpSecurity http,
        @Qualifier("basicAuthenticationFilter") WebFilter basicAuthenticationFilter,
        @Qualifier("tokenAuthenticationFilter") WebFilter tokenAuthenticationFilter,
        @Qualifier("oidcAuthenticationFilter") WebFilter oidcAuthenticationFilter,
        ServerAuthenticationEntryPoint serverAuthenticationEntryPoint
    ) {
        return baseConfiguration(http.securityMatcher(ServerWebExchangeMatchers.pathMatchers(
                getFullUrls("/static-api/**", "/containers/**", "/application/**", "/services/**", APIDOC_ROUTES))),
                serverAuthenticationEntryPoint,
                basicAuthenticationFilter, tokenAuthenticationFilter, oidcAuthenticationFilter
            )
            .authorizeExchange(exchange -> exchange.anyExchange().authenticated())
            .build();
    }

    /**
     * Default filter chain to protect all routes with MF credentials.
     */
    @Bean
    @Order(6)
    public SecurityWebFilterChain webSecurityCustomizer(
        ServerHttpSecurity http,
        ServerAuthenticationEntryPoint serverAuthenticationEntryPoint
    ) {
        return baseConfiguration(http, serverAuthenticationEntryPoint)
            .securityMatcher(ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET,
                ArrayUtils.addAll(getFullUrls(
                    "",
                    "/",
                    "/static/**",
                    "/favicon.ico",
                    "/v3/api-docs",
                    "/index.html",
                    "/application/info",
                    "/oidc/provider"
                ), "/")))
            .authorizeExchange(authorizeExchangeSpec -> authorizeExchangeSpec.anyExchange().permitAll())
            .build();
    }

    private ServerHttpSecurity baseConfiguration(
        ServerHttpSecurity http,
        ServerAuthenticationEntryPoint serverAuthenticationEntryPoint,
        WebFilter...webFiltersAuthorization
    ) {
        var antMatcher = new AntPathMatcher();

        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)

            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .formLogin(formLoginSpec -> formLoginSpec.disable())
            .httpBasic(httpBasicSpec -> httpBasicSpec.disable())

            .headers(httpSecurityHeadersConfigurer ->
                httpSecurityHeadersConfigurer.hsts(ServerHttpSecurity.HeaderSpec.HstsSpec::disable)
                    .frameOptions(ServerHttpSecurity.HeaderSpec.FrameOptionsSpec::disable))

            .exceptionHandling(exceptionHandlingSpec -> exceptionHandlingSpec
                .authenticationEntryPoint((exchange, exception) -> {
                    String requestedUri = exchange.getRequest().getURI().toString();
                    log.debug("Unauthorized access to '{}' endpoint", requestedUri);

                    if (Stream.of(getFullUrls(
                            "/application/**",
                            APIDOC_ROUTES,
                            STATIC_REFRESH_ROUTE
                        )).noneMatch(pattern -> antMatcher.match(pattern, requestedUri))
                    ) {
                        exchange.getResponse().getHeaders().add(HttpHeaders.WWW_AUTHENTICATE, ApimlConstants.BASIC_AUTHENTICATION_PREFIX);
                    }

                    return serverAuthenticationEntryPoint.commence(exchange, exception);
                })
            );

            Stream.of(webFiltersAuthorization).forEach(webFilter -> http.addFilterBefore(webFilter, SecurityWebFiltersOrder.AUTHENTICATION));

        return http;
    }

    @Bean
    public WebFilter basicAuthenticationFilter(
        GatewaySecurityService gatewaySecurityService
    ) {
        return (exchange, chain) -> chain.filter(exchange)
            .contextWrite(context -> {
                var authorizationHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                return LoginFilter.getCredentialFromAuthorizationHeader(Optional.ofNullable(authorizationHeader)).map(login -> {
                    try {
                        return gatewaySecurityService.login(login.getUsername(), login.getPassword(), null).map(token ->
                            ReactiveSecurityContextHolder.withAuthentication(
                                new UsernamePasswordAuthenticationToken(login.getUsername(), login.getPassword(), Collections.emptyList())
                            )
                        ).orElse(context);
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

    @Bean
    public ReactiveAuthenticationManager reactiveAuthenticationManager() {
        return Mono::just;
    }

    @Bean
    public ServerAuthenticationEntryPoint serverAuthenticationEntryPoint(
        MessageService messageService,
        ObjectMapper mapper
    ) {
        return (exchange, authenticationException) -> {
            try {
                ApiMessageView message = messageService.createMessage("org.zowe.apiml.common.unauthorized").mapToView();
                DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(mapper.writeValueAsBytes(message));
                exchange.getResponse().setRawStatusCode(SC_UNAUTHORIZED);
                return exchange.getResponse()
                    .writeWith(Mono.just(buffer));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        };
    }

}
