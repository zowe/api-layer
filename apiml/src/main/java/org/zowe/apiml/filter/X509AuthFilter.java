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
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.zowe.apiml.security.common.token.X509AuthenticationToken;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

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
        if (ArrayUtils.isEmpty(certs)) {
            return chain.filter(exchange);
        }

        return ReactiveSecurityContextHolder.getContext()
            .flatMap(ctx -> {
                if (ctx.getAuthentication().isAuthenticated() && ctx.getAuthentication().getPrincipal() != null) {
                    return Mono.just(ctx.getAuthentication());
                }
                return Mono.empty();
            })
            .switchIfEmpty(Mono.<Authentication>defer(() -> x509AuthenticationProvider.authenticate(new X509AuthenticationToken(certs))))
            .onErrorResume(AuthenticationException.class, ex -> Mono.empty())
            .map(auth -> auth.isAuthenticated() ? ReactiveSecurityContextHolder.withAuthentication(auth) : Context.empty())
            .switchIfEmpty(Mono.just(Context.empty()))
            .flatMap(ctx -> {
                var next = chain.filter(exchange);
                if (!ctx.isEmpty()) {
                    return next.contextWrite(ctx);
                }
                return next;
            });

    }

}
