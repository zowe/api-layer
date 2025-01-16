/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.zaas.ZaasTokenResponse;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AbstractTokenFilterFactoryTest {

    @Nested
    class RequestUpdate {

        private ServerHttpRequest testRequestMutation(AbstractAuthSchemeFactory.AuthorizationResponse<ZaasTokenResponse> tokenResponse) {
            var chain = mock(GatewayFilterChain.class);
            var request = MockServerHttpRequest.get("/url").build();
            var exchange = MockServerWebExchange.from(request);

            new AbstractTokenFilterFactory<>(AbstractTokenFilterFactory.Config.class, null, null, null) {
                @Override
                public String getEndpointUrl(ServiceInstance instance) {
                    return null;
                }
            }.processResponse(exchange, chain, tokenResponse);

            var modifiedExchange = ArgumentCaptor.forClass(ServerWebExchange.class);
            verify(chain).filter(modifiedExchange.capture());

            return modifiedExchange.getValue().getRequest();
        }

        @Nested
        class ValidResponse {

            @Test
            void givenHeaderResponse_whenHandling_thenUpdateTheRequest() {
                var request = testRequestMutation(new AbstractAuthSchemeFactory.AuthorizationResponse<>(null,ZaasTokenResponse.builder()
                    .headerName("headerName")
                    .token("headerValue")
                    .build()
                ));
                assertEquals("headerValue", request.getHeaders().getFirst("headerName"));
            }

            @Test
            void givenCookieResponse_whenHandling_thenUpdateTheRequest() {
                var request = testRequestMutation(new AbstractAuthSchemeFactory.AuthorizationResponse<>(null,ZaasTokenResponse.builder()
                    .cookieName("cookieName")
                    .token("cookieValue")
                    .build()
                ));
                assertEquals("cookieName=cookieValue", request.getHeaders().getFirst("cookie"));
            }

        }

        @Nested
        class InvalidResponse {

            @Test
            void givenEmptyResponse_whenHandling_thenNoUpdate() {
                var request = testRequestMutation(new AbstractAuthSchemeFactory.AuthorizationResponse<>(null, ZaasTokenResponse.builder()
                    .token("jwt")
                    .build()
                ));
                assertEquals(1, request.getHeaders().size());
                assertTrue(request.getHeaders().containsKey(ApimlConstants.AUTH_FAIL_HEADER));
                assertEquals("Invalid or missing authentication", request.getHeaders().getFirst(ApimlConstants.AUTH_FAIL_HEADER));
            }

            @Test
            void givenEmptyResponseWithError_whenHandling_thenProvideErrorHeader() {
                var request = testRequestMutation(new AbstractAuthSchemeFactory.AuthorizationResponse<>(
                    MockHeaders.builder()
                        .name(ApimlConstants.AUTH_FAIL_HEADER.toLowerCase())
                        .value("anError")
                        .build(),
                    ZaasTokenResponse.builder()
                        .token("jwt")
                        .build()
                ));
                assertEquals(1, request.getHeaders().size());
                assertTrue(request.getHeaders().containsKey(ApimlConstants.AUTH_FAIL_HEADER));
                assertEquals("anError", request.getHeaders().getFirst(ApimlConstants.AUTH_FAIL_HEADER));
            }

            @Test
            void givenCookieAndHeaderInResponse_whenHandling_thenSetBoth() {
                var request = testRequestMutation(new AbstractAuthSchemeFactory.AuthorizationResponse<>(null,ZaasTokenResponse.builder()
                    .cookieName("cookie")
                    .headerName("header")
                    .token("jwt")
                    .build()
                ));
                assertEquals("jwt", request.getHeaders().getFirst("header"));
                assertEquals("cookie=jwt", request.getHeaders().getFirst("cookie"));
            }

        }

    }

    @RequiredArgsConstructor
    @Builder
    static class MockHeaders implements ClientResponse.Headers {

        private final String name;
        private final String value;

        @Override
        public OptionalLong contentLength() {
            return OptionalLong.empty();
        }

        @Override
        public Optional<MediaType> contentType() {
            return Optional.empty();
        }

        @Override
        public List<String> header(String headerName) {
            return List.of(value);
        }

        @Override
        public org.springframework.http.HttpHeaders asHttpHeaders() {
            var headers = new org.springframework.http.HttpHeaders();
            headers.add(name, value);
            return headers;
        }

    }

}
