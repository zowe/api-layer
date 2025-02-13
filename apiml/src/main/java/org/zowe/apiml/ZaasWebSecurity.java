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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.gateway.x509.X509Util;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.login.LoginRequest;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.util.CookieUtil;
import org.zowe.apiml.zaas.security.config.CompoundAuthManager;
import org.zowe.apiml.zaas.zaas.ExtractAuthSourceWebFilter;
import reactor.core.publisher.Mono;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ZaasWebSecurity {
    public static final String LOGIN = "/gateway/api/v1/auth/login";
    public static final String TICKET_LONG_URL = "gateway/api/v1/auth/ticket";

    private final CompoundAuthManager compoundAuthManager;
    private final AuthConfigurationProperties authConfigurationProperties;
    private final ExtractAuthSourceWebFilter extractAuthSourceWebFilter;

    private final ObjectMapper mapper;

    @Bean
    @Order(1)
    public SecurityWebFilterChain loginSecurityWebFilterChain(ServerHttpSecurity http) {
        return defaultSecurityConfig(http)
            .securityMatcher(ServerWebExchangeMatchers.pathMatchers(
                LOGIN
            ))
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .anonymous(ServerHttpSecurity.AnonymousSpec::disable)
            .authenticationManager(compoundAuthManager)
            .authorizeExchange(authorizeExchangeSpec ->
                authorizeExchangeSpec
                    .anyExchange().authenticated()
            )
            .addFilterAt(authenticationWebFilter(), SecurityWebFiltersOrder.AUTHENTICATION)

            .build();
    }


    // Endpoints protected with X509
    @Bean
    @Order(3)
    public SecurityWebFilterChain ticketSecurityWebFilterChain(ServerHttpSecurity http) {
        return defaultSecurityConfig(http).x509(x509 -> x509
                .principalExtractor(X509Util.x509PrincipalExtractor())
                .authenticationManager(X509Util.x509ReactiveAuthenticationManager())
            )
            .securityMatcher(ServerWebExchangeMatchers.pathMatchers(
                TICKET_LONG_URL
            ))
            .authorizeExchange(authorizeExchangeSpec ->
                authorizeExchangeSpec
                    .anyExchange().authenticated()
            )
            .addFilterAfter(extractAuthSourceWebFilter,SecurityWebFiltersOrder.AUTHENTICATION)

            .build();
    }


    AuthenticationWebFilter authenticationWebFilter() {
        var authWebFilter = new AuthenticationWebFilter(compoundAuthManager);
        authWebFilter.setServerAuthenticationConverter(exchange -> {
            var loginRequest = getCredentialFromAuthorizationHeader(exchange.getRequest());
            if (loginRequest.isEmpty()) {
                return getCredentialsFromBody(exchange.getRequest()).map(loginRequest1 -> new UsernamePasswordAuthenticationToken(loginRequest1.getUsername(), loginRequest1));
            }
            return Mono.just(new UsernamePasswordAuthenticationToken(loginRequest.get().getUsername(), loginRequest.get()));
        });
        authWebFilter.setAuthenticationSuccessHandler((webFilterExchange, authentication) -> {
                TokenAuthentication tokenAuthentication = (TokenAuthentication) authentication;
                String token = tokenAuthentication.getCredentials();

                setCookie(token, webFilterExchange.getExchange().getResponse());
                webFilterExchange.getExchange().getResponse().setStatusCode(HttpStatus.NO_CONTENT);
                return Mono.empty();
            }
        );
        return authWebFilter;
    }


    public ServerHttpSecurity defaultSecurityConfig(ServerHttpSecurity http) {

        return http
            .headers(customizer -> customizer.frameOptions(ServerHttpSecurity.HeaderSpec.FrameOptionsSpec::disable))

            .csrf(ServerHttpSecurity.CsrfSpec::disable);
    }

    /**
     * Add the cookie to the response
     *
     * @param token    the authentication token
     * @param response send back this response
     */
    private void setCookie(String token, ServerHttpResponse response) {
        // SameSite attribute is not supported in Cookie used in HttpServletResponse.addCookie,
        // so specify Set-Cookie header directly

        AuthConfigurationProperties.CookieProperties cp = authConfigurationProperties.getCookieProperties();
        String cookieHeader = new CookieUtil.CookieHeaderBuilder(cp.getCookieName(), token)
            .path(cp.getCookiePath())
            .sameSite(cp.getCookieSameSite().getValue())
            .maxAge(cp.getCookieMaxAge())
            .httpOnly(true)
            .secure(cp.isCookieSecure())
            .build();
        response.getHeaders().add(HttpHeaders.SET_COOKIE, cookieHeader);

    }

    /**
     * Extract credentials from the authorization header in the request and decode them
     *
     * @param request the http request
     * @return the decoded credentials
     */
    public static Optional<LoginRequest> getCredentialFromAuthorizationHeader(ServerHttpRequest request) {
        var headers = Optional.ofNullable(
            request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)
        );
        return getCredentialFromAuthorizationHeader(headers);
    }

    public static Optional<LoginRequest> getCredentialFromAuthorizationHeader(Optional<String> headers) {
        return headers.filter(
                header -> header.startsWith(ApimlConstants.BASIC_AUTHENTICATION_PREFIX)
            ).map(
                header -> header.replaceFirst(ApimlConstants.BASIC_AUTHENTICATION_PREFIX, "").trim()
            )
            .filter(base64Credentials -> !base64Credentials.isEmpty())
            .map(ZaasWebSecurity::mapBase64Credentials);
    }


    /**
     * Decode the encoded credentials
     *
     * @param base64Credentials the credentials encoded in base64
     * @return the decoded credentials in {@link LoginRequest}
     */
    private static LoginRequest mapBase64Credentials(String base64Credentials) {
        byte[] credentials = null;
        try {
            credentials = Base64.getDecoder().decode(base64Credentials);
            int index = ArrayUtils.indexOf(credentials, (byte) ':');
            if (index > 0) {
                byte[] password = null;
                char[] passwordChars;
                try {
                    password = Arrays.copyOfRange(credentials, index + 1, credentials.length);
                    passwordChars = new char[password.length];
                    for (int i = 0; i < password.length; i++) {
                        passwordChars[i] = (char) password[i];
                    }
                    return new LoginRequest(
                        new String(Arrays.copyOfRange(credentials, 0, index), StandardCharsets.UTF_8),
                        passwordChars
                    );
                } finally {
                    if (password != null) {
                        Arrays.fill(password, (byte) 0);
                    }
                }
            }
        } finally {
            if (credentials != null) {
                Arrays.fill(credentials, (byte) 0);
            }
        }
        throw new BadCredentialsException("Invalid basic authentication header");
    }

    /**
     * Get credentials from the request body
     *
     * @param request the http request
     * @return the credentials in {@link LoginRequest}
     * @throws AuthenticationCredentialsNotFoundException if the login object has wrong format
     */
    private Mono<LoginRequest> getCredentialsFromBody(ServerHttpRequest request) {
        return Mono.from(request.getBody().flatMap(dataBuffer -> {
            try (var is = dataBuffer.asInputStream();
                 var bis = new BufferedInputStream(is)) {
                return Mono.just(mapper.readValue(bis, LoginRequest.class));
            } catch (IOException e) {
                log.debug("Authentication problem: login object has wrong format");
                return Mono.error(new AuthenticationCredentialsNotFoundException("Login object has wrong format."));
            }
        })).onErrorResume(throwable -> Mono.empty());

    }

}
