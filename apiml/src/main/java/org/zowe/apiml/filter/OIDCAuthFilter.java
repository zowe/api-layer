/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import org.zowe.apiml.gateway.filters.security.AbstractTokenAuthFilter;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.OIDCProvider;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.security.common.token.TokenFormatNotValidException;
import org.zowe.apiml.security.common.util.JwtUtils;
import org.zowe.apiml.zaas.security.mapping.AuthenticationMapper;
import org.zowe.apiml.zaas.security.service.schema.source.OIDCAuthSource;
import org.zowe.apiml.util.CookieUtil;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

/**
 * A reactive WebFilter that performs OIDC token authentication.
 * <p>
 * Extracts a token from the Authorization Bearer header or the apimlAuthenticationToken cookie
 * (same sources as {@link org.zowe.apiml.gateway.filters.security.TokenAuthFilter}).
 * <p>
 * This filter is intended to be placed <b>before</b> TokenAuthFilter in the chain.
 * If the request is already authenticated, this filter is skipped.
 * Otherwise, it attempts to validate the token as an OIDC token via JWK,
 * extract the user identity, and map it to a mainframe user.
 * <p>
 * On successful OIDC authentication, the filter strips the token from the request
 * (removes Authorization header and auth cookie) so that downstream filters like
 * TokenAuthFilter do not attempt to re-validate the OIDC token as an APIML JWT.
 */
@RequiredArgsConstructor
@Slf4j
public class OIDCAuthFilter extends AbstractTokenAuthFilter {

    private final OIDCProvider oidcProvider;
    private final AuthenticationMapper oidcMapper;
    private final AuthConfigurationProperties authConfigurationProperties;
    private final List<String> userIdFieldPath;

    @Override
    protected AuthConfigurationProperties getAuthConfigurationProperties() {
        return authConfigurationProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .map(Authentication::isAuthenticated)
            .defaultIfEmpty(false)
            .flatMap(alreadyAuthenticated -> {
                if (alreadyAuthenticated) {
                    return chain.filter(exchange);
                }

                var tokenOpt = resolveToken(exchange.getRequest());
                if (tokenOpt.isEmpty()) {
                    return chain.filter(exchange);
                }

                var token = tokenOpt.get();
                return attemptOidcAuthentication(exchange, chain, token);
            });
    }

    /**
     * Validates the token as OIDC. On success, strips the token from the request
     * so that downstream filters (e.g. TokenAuthFilter) do not attempt to
     * re-validate it, then continues the chain with the authentication context set.
     * On failure, continues the chain unmodified.
     */
    private Mono<Void> attemptOidcAuthentication(ServerWebExchange exchange, WebFilterChain chain, String token) {
        return Mono.fromCallable(() -> authenticate(token))
            .flatMap(authOpt -> {
                if (authOpt.isPresent()) {
                    var sanitized = stripToken(exchange);
                    return chain.filter(sanitized)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authOpt.get()));
                }
                return chain.filter(exchange);
            });
    }

    private Optional<TokenAuthentication> authenticate(String token) {
        if (!oidcProvider.isValid(token)) {
            log.debug("Token is not OIDC or it is invalid");
            return Optional.empty();
        }

        List<String> userIds;
        try {
            userIds = JwtUtils.getFieldValuesFromToken(token, userIdFieldPath);
        } catch (TokenFormatNotValidException e) {
            log.debug("Cannot extract userId from OIDC token: {}", e.getMessage());
            return Optional.empty();
        }

        var authSource = new OIDCAuthSource(token);
        authSource.setDistributedId(userIds);

        String mainframeUser = oidcMapper.mapToMainframeUserId(authSource);
        if (StringUtils.isBlank(mainframeUser)) {
            log.debug("OIDC token identity mapping failed - no mainframe user found");
            return Optional.empty();
        }

        log.debug("OIDC token authenticated, mapped to mainframe user: {}", mainframeUser);
        return Optional.of(TokenAuthentication.createAuthenticated(mainframeUser, token, TokenAuthentication.Type.OIDC));
    }

    /**
     * Returns a mutated exchange with the Authorization header and auth cookie removed,
     * preventing downstream token-based filters from re-processing the OIDC token.
     */
    private ServerWebExchange stripToken(ServerWebExchange exchange) {
        String cookieName = authConfigurationProperties.getCookieProperties().getCookieName();
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
            .headers(headers -> {
                headers.remove(HttpHeaders.AUTHORIZATION);
                List<String> cookies = headers.get(HttpHeaders.COOKIE);
                if (cookies != null) {
                    List<String> filtered = cookies.stream()
                        .map(cookieHeader -> CookieUtil.removeCookie(cookieHeader, cookieName))
                        .filter(s -> !s.isEmpty()).toList();
                    headers.remove(HttpHeaders.COOKIE);
                    log.debug("Removing cookies.");
                    if (!filtered.isEmpty()) {
                        log.debug("Adding filtered cookies: {}", filtered.size());
                        filtered.forEach(c -> headers.add(HttpHeaders.COOKIE, c));
                    }
                }
            })
            .build();
        return exchange.mutate().request(mutatedRequest).build();
    }


}
