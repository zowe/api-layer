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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.web.server.WebFilterChain;
import org.zowe.apiml.controller.ReactiveAuthenticationController.AccessTokenRequest;
import org.zowe.apiml.security.common.error.AccessTokenBodyNotValidException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreAccessTokenInfoWebFilterTest {

    @Mock private ServerAuthenticationFailureHandler failureHandler;
    @Mock private WebFilterChain chain;

    private final ObjectMapper mapper = new ObjectMapper();
    private StoreAccessTokenInfoWebFilter filter;

    private MockServerWebExchange exchange;
    private MockServerHttpRequest request;

    @BeforeEach
    void setUp() {
        this.filter = new StoreAccessTokenInfoWebFilter(failureHandler, mapper);
        this.request = null;
        this.exchange = null;
    }

    @Nested
    class GivenFilter {

        @Nested
        class WhenValidBody {

            @BeforeEach
            void setUp() {
                request = MockServerHttpRequest.post("/access-token/generate")
                    .body("""
                        {
                            "validity": 50,
                            "scopes": ["gateway", "discovery"]
                        }
                        """);
                exchange = MockServerWebExchange.builder(request).build();
            }

            @Test
            void thenSetAttribute() {
                when(chain.filter(exchange)).thenReturn(Mono.empty());
                var accessTokenRequest = new AccessTokenRequest(50, Set.of("gateway", "discovery"));

                StepVerifier.create(filter.filter(exchange, chain))
                    .expectComplete()
                    .verify();

                assertEquals(accessTokenRequest, exchange.getAttribute("tokenRequest"));

                verifyNoInteractions(failureHandler);
            }

        }

        @Nested
        class WhenInvalidBody {

            @Test
            void whenUnparseable_thenCallFailureHandler() {
                request = MockServerHttpRequest.post("/access-token/generate")
                    .body("this is not a valid json");
                exchange = MockServerWebExchange.builder(request).build();

                when(failureHandler.onAuthenticationFailure(
                    argThat(webExchange -> webExchange.getChain() == chain && webExchange.getExchange() == exchange),
                    argThat(e -> e instanceof AccessTokenBodyNotValidException ate && ate.getMessageId().equals("org.zowe.apiml.security.query.invalidAccessTokenBody"))))
                .thenReturn(Mono.empty());

                StepVerifier.create(filter.filter(exchange, chain))
                    .expectComplete()
                    .verify();

                assertNull(exchange.getAttribute("tokenRequest"));

                verifyNoInteractions(chain);
            }

            @Test
            void whenMissingScopes_thenCallFailureHandler() {
                request = MockServerHttpRequest.post("/access-token/generate")
                    .body("""
                        {
                            "validity": 50
                        }
                        """);
                exchange = MockServerWebExchange.builder(request).build();

                when(failureHandler.onAuthenticationFailure(
                    argThat(webExchange -> webExchange.getChain() == chain && webExchange.getExchange() == exchange),
                    argThat(e -> e instanceof AccessTokenBodyNotValidException ate && ate.getMessageId().equals("org.zowe.apiml.security.token.accessTokenBodyMissingScopes"))))
                .thenReturn(Mono.empty());

                StepVerifier.create(filter.filter(exchange, chain))
                    .expectComplete()
                    .verify();

                assertNull(exchange.getAttribute("tokenRequest"));

                verifyNoInteractions(chain);
            }

            @Test
            void whenEmpty_thenCallFailureHandler() {
                request = MockServerHttpRequest.post("/access-token/generate")
                    .body("");
                exchange = MockServerWebExchange.builder(request).build();

                when(failureHandler.onAuthenticationFailure(
                    argThat(webExchange -> webExchange.getChain() == chain && webExchange.getExchange() == exchange),
                    argThat(e -> e instanceof AccessTokenBodyNotValidException ate && ate.getMessageId().equals("org.zowe.apiml.security.query.invalidAccessTokenBody"))))
                .thenReturn(Mono.empty());

                StepVerifier.create(filter.filter(exchange, chain))
                    .expectComplete()
                    .verify();

                assertNull(exchange.getAttribute("tokenRequest"));

                verifyNoInteractions(chain);
            }

        }

    }

}
