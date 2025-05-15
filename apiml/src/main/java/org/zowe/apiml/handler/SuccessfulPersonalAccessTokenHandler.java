/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.handler;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.stereotype.Service;
import org.zowe.apiml.security.common.audit.RauditxService;
import org.zowe.apiml.security.common.token.AccessTokenProvider;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.zowe.apiml.security.common.filter.StoreAccessTokenInfoFilter.TOKEN_REQUEST;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuccessfulPersonalAccessTokenHandler implements ServerAuthenticationSuccessHandler {
    private final AccessTokenProvider accessTokenProvider;
    private final RauditxService rauditxService;

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {
        String username = authentication.getName();
        log.debug("Generating access token for user {}", username);

        RauditxService.RauditxBuilder rauditBuilder = rauditxService.builder()
            .userId(username)
            .messageSegment("An attempt to generate PAT")
            .alwaysLogSuccesses()
            .alwaysLogFailures();

        return Mono.defer(() -> {
            AccessTokenRequest tokenRequest = webFilterExchange.getExchange().getAttribute(TOKEN_REQUEST);
            if (tokenRequest == null) {
                rauditBuilder.failure();
                rauditBuilder.issue();
                return Mono.error(new IllegalStateException("Missing token request"));
            }

            String token;
            try {
                token = accessTokenProvider.getToken(
                    username,
                    tokenRequest.getValidity(),
                    tokenRequest.getScopes()
                );
                rauditBuilder.success();
            } catch (RuntimeException e) {
                rauditBuilder.failure();
                rauditBuilder.issue();
                return Mono.error(e);
            }

            rauditBuilder.issue();

            // Write token to response
            byte[] tokenBytes = token.getBytes(StandardCharsets.UTF_8);
            return webFilterExchange.getExchange().getResponse()
                .writeWith(
                Mono.just(webFilterExchange.getExchange().getResponse()
                    .bufferFactory()
                    .wrap(tokenBytes))
            );
        });
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccessTokenRequest {
        private int validity;
        private Set<String> scopes;
    }
}
