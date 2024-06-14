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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.zaas.ZaasTokenResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class TokenFilterFactoryTest {

    @Nested
    class RequestUpdate {

        private MockServerHttpRequest testRequestMutation(AbstractAuthSchemeFactory.AuthorizationResponse<ZaasTokenResponse> tokenResponse) {
            MockServerHttpRequest request = MockServerHttpRequest.get("/url").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            new TokenFilterFactory(null, null, null) {
                @Override
                public String getEndpointUrl(ServiceInstance instance) {
                    return null;
                }
            }.processResponse(exchange, mock(GatewayFilterChain.class), tokenResponse);

            return request;
        }

        @Nested
        class ValidResponse {

            @Test
            void givenHeaderResponse_whenHandling_thenUpdateTheRequest() {
                MockServerHttpRequest request = testRequestMutation(new AbstractAuthSchemeFactory.AuthorizationResponse<>(null,ZaasTokenResponse.builder()
                    .headerName("headerName")
                    .token("headerValue")
                    .build()
                ));
                assertEquals("headerValue", request.getHeaders().getFirst("headerName"));
            }

            @Test
            void givenCookieResponse_whenHandling_thenUpdateTheRequest() {
                MockServerHttpRequest request = testRequestMutation(new AbstractAuthSchemeFactory.AuthorizationResponse<>(null,ZaasTokenResponse.builder()
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
                MockServerHttpRequest request = testRequestMutation(new AbstractAuthSchemeFactory.AuthorizationResponse<>(null,ZaasTokenResponse.builder()
                    .token("jwt")
                    .build()
                ));
                assertEquals(1, request.getHeaders().size());
                assertTrue(request.getHeaders().containsKey(ApimlConstants.AUTH_FAIL_HEADER));
            }

            @Test
            void givenCookieAndHeaderInResponse_whenHandling_thenSetBoth() {
                MockServerHttpRequest request = testRequestMutation(new AbstractAuthSchemeFactory.AuthorizationResponse<>(null,ZaasTokenResponse.builder()
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

}
