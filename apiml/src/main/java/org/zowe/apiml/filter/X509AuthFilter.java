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
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.zowe.apiml.security.common.token.X509AuthenticationToken;
import reactor.core.publisher.Mono;

import java.security.cert.X509Certificate;

import static org.zowe.apiml.security.common.filter.CategorizeCertsFilter.ATTR_NAME_CLIENT_AUTH_X509_CERTIFICATE;

/**
 * A reactive WebFilter that performs X.509 client certificate authentication.
 * <p>
 * It checks if a client certificate is present in the exchange attributes (injected upstream),
 * and attempts to authenticate using the provided {@link ReactiveAuthenticationManager}.
 * <p>
 * If the current context is already authenticated or no certificate is provided,
 * the request proceeds without authentication.
 * <p>
 * If authentication is successful, the resulting {@link org.springframework.security.core.Authentication}
 * is propagated via {@link ReactiveSecurityContextHolder}.
 */
@RequiredArgsConstructor
public class X509AuthFilter implements WebFilter {

    private final ReactiveAuthenticationManager x509AuthenticationProvider;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        X509Certificate[] certs = exchange.getAttribute(ATTR_NAME_CLIENT_AUTH_X509_CERTIFICATE);
        return ReactiveSecurityContextHolder.getContext().defaultIfEmpty(new SecurityContextImpl(new X509AuthenticationToken(null)))
            .flatMap(ctx -> {
                if ((ctx.getAuthentication().isAuthenticated() && ctx.getAuthentication().getPrincipal() != null) || certs == null || certs.length == 0) {
                    return chain.filter(exchange);
                }
                return x509AuthenticationProvider.authenticate(new X509AuthenticationToken(certs))
                    .flatMap(authentication -> {
                        if (!authentication.isAuthenticated()) {
                            return chain.filter(exchange);
                        }
                        return chain.filter(exchange)
                            .contextWrite(context -> ReactiveSecurityContextHolder.withAuthentication(authentication));
                    })
                    .onErrorResume(AuthenticationException.class, ex -> chain.filter(exchange));
            });

    }

}
