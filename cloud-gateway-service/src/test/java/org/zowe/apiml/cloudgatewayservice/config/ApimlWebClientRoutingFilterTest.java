/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.cloud.loadbalancer.support.SimpleObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ALREADY_ROUTED_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.zowe.apiml.constants.ApimlConstants.HTTP_CLIENT_USE_CLIENT_CERTIFICATE;

class ApimlWebClientRoutingFilterTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class HttpClientChooser {


        WebClient httpClientNoCert = mock(WebClient.class);
        WebClient httpClientClientCert = mock(WebClient.class);


        @Test
        void givenDefaultHttpClient_whenCreatingAInstance_thenBothHttpClientsAreCreatedWell() {
            ApimlWebClientRoutingFilter apimlWebClientRoutingFilter = new ApimlWebClientRoutingFilter(httpClientNoCert, httpClientClientCert, null);

            // verify if proper httpClient instances were created
            assertSame(httpClientNoCert, ReflectionTestUtils.getField(apimlWebClientRoutingFilter, "webClient"));
            assertSame(httpClientClientCert, ReflectionTestUtils.getField(apimlWebClientRoutingFilter, "webClientClientCert"));
        }

        @Nested
        class GetHttpClient {

            ApimlWebClientRoutingFilter apimlWebClientRoutingFilter;
            MockServerWebExchange serverWebExchange;
            GatewayFilterChain filterChain;
            ObjectProvider<List<HttpHeadersFilter>> headersFiltersProvider;

            @BeforeEach
            void initMocks() throws URISyntaxException {
                headersFiltersProvider = new SimpleObjectProvider<>(new ArrayList<>());
                apimlWebClientRoutingFilter = new ApimlWebClientRoutingFilter(httpClientNoCert, httpClientClientCert, headersFiltersProvider);

                MockServerHttpRequest mockServerHttpRequest = MockServerHttpRequest.get("/path").build();
                serverWebExchange = MockServerWebExchange.from(mockServerHttpRequest);
                filterChain = mock(GatewayFilterChain.class);
                URI uri = new URI("https://localhost:10010");
                serverWebExchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, uri);

                var requestBodySpec = mock(WebClient.RequestBodyUriSpec.class);
                when(httpClientClientCert.method(any())).thenReturn(requestBodySpec);
                when(httpClientNoCert.method(any())).thenReturn(requestBodySpec);
                var httpHeaders = new HttpHeaders();
                when(requestBodySpec.headers(any())).thenAnswer(invocation -> {
                    Consumer<HttpHeaders> function = invocation.getArgument(0);
                    function.accept(httpHeaders);
                    return requestBodySpec;
                });
                when(requestBodySpec.uri(any(URI.class))).thenReturn(requestBodySpec);
                var clientResp = mock(ClientResponse.class);
                var headers = mock(ClientResponse.Headers.class);
                when(headers.asHttpHeaders()).thenReturn(new HttpHeaders());
                when(clientResp.headers()).thenReturn(headers);
                when(requestBodySpec.exchangeToMono(any())).thenReturn(Mono.just(clientResp));
                when(filterChain.filter(serverWebExchange)).thenReturn(Mono.empty());

            }

            @Test
            void givenRequirementsForClientCert_whenGetHttpClient_thenCallWithClientCert() {
                serverWebExchange.getAttributes().put(HTTP_CLIENT_USE_CLIENT_CERTIFICATE, Boolean.TRUE);
                var result = apimlWebClientRoutingFilter.filter(serverWebExchange, filterChain);
                verify(httpClientClientCert, times(1)).method(any());
                verify(httpClientNoCert, times(0)).method(any());
                result.block();
            }

            @Test
            void givenNoRequirementsForClientCert_whenGetHttpClient_thenDoNotUseClientCert() {

                var result = apimlWebClientRoutingFilter.filter(serverWebExchange, filterChain);
                verify(httpClientClientCert, times(0)).method(any());
                verify(httpClientNoCert, times(1)).method(any());
                result.block();
            }

            @Test
            void givenAlreadyRouted_thenDoNotUseClient() {
                serverWebExchange.getAttributes().put(GATEWAY_ALREADY_ROUTED_ATTR, Boolean.TRUE);
                verify(httpClientClientCert, times(0)).method(any());
                verify(httpClientNoCert, times(0)).method(any());
            }

        }

    }

}
