/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.security.common.auth.saf.PlatformReturned;
import org.zowe.apiml.security.common.error.AccessTokenTooManyScopesException;
import org.zowe.apiml.security.common.error.ZosAuthenticationException;
import org.zowe.apiml.zaas.cache.CachingServiceClientException;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ApimlExceptionHandlerTest {

    private ApimlExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = spy(new ApimlExceptionHandler(null, null, null) {
            @Override
            public Mono<Void> setBodyResponse(ServerWebExchange exchange, int responseCode, String messageCode, Object... args) {
                return Mono.empty();
            }
        });
    }

    @Nested
    class GivenApimlExceptionHandler {

        @Nested
        class WhenZosAuthenticationException {

            @Test
            void thenProvideDetails() {
                var request = MockServerHttpRequest.get("https://localhost/some/url").build();
                var exchange = MockServerWebExchange.from(request);
                var ex = new ZosAuthenticationException(PlatformReturned.builder()
                    .errno(139)
                    .errnoMsg("ABC")
                    .build());

                exceptionHandler.handleZosAuthenticationException(exchange, ex);

                verify(exceptionHandler).setBodyResponse(eq(exchange), eq(500), eq("org.zowe.apiml.security.platform.errno.ERROR"), anyString());
            }

        }

        /**
         * The limit is enforced in the provider so that no issuance path can bypass it, which means the only
         * place the caller learns the number is this handler's message.
         */
        @Nested
        class WhenAccessTokenTooManyScopesException {

            @Test
            void thenAnswerBadRequestNamingTheLimit() {
                var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("https://localhost/generate").build());

                exceptionHandler.handleAccessTokenTooManyScopesException(exchange,
                    new AccessTokenTooManyScopesException("too many", 64));

                verify(exceptionHandler).setBodyResponse(eq(exchange), eq(400),
                    eq("org.zowe.apiml.security.token.accessTokenTooManyScopes"), eq(64));
            }
        }

        /**
         * Without this the personal access token endpoints answer 500 with a stack trace for something that is
         * simply the revocation store being unreachable - which is also how a caching service too old to serve
         * point lookups surfaces.
         */
        @Nested
        class WhenCachingServiceClientException {

            @Test
            void thenAnswerServiceUnavailable() {
                var exchange = MockServerWebExchange.from(MockServerHttpRequest.delete("https://localhost/revoke").build());

                exceptionHandler.handleCachingServiceClientException(exchange,
                    new CachingServiceClientException("cannot reach the caching service"));

                verify(exceptionHandler).setBodyResponse(eq(exchange), eq(503),
                    eq("org.zowe.apiml.zaas.pat.cachingServiceUnavailable"), anyString());
            }
        }

    }

}
