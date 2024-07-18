/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.junit.jupiter.api.*;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.cloud.gateway.support.RouteMetadataUtils.CONNECT_TIMEOUT_ATTR;
import static org.zowe.apiml.constants.ApimlConstants.HTTP_CLIENT_USE_CLIENT_CERTIFICATE;

class NettyRoutingFilterApimlTest {

    @Nested
    class Parsing {

        @Test
        void givenInteger_whenGetInteger_thenConvert() {
            assertEquals(Integer.valueOf(157), NettyRoutingFilterApiml.getInteger(157));
        }

        @Test
        void givenNumberString_whenGetInteger_thenParse() {
            assertEquals(Integer.valueOf(759), NettyRoutingFilterApiml.getInteger("759"));
        }

        @Test
        void givenNull_whenGetInteger_thenThrowNullPointerException() {
            assertThrows(NullPointerException.class, () -> NettyRoutingFilterApiml.getInteger(null));
        }

        @Test
        void givenNonNumericValue_whenGetInteger_thenThrowNumberFormatException() {
            assertThrows(NumberFormatException.class, () -> NettyRoutingFilterApiml.getInteger("nonNumeric"));
        }

    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class HttpClientChooser {

        SslContext sslContextNoCert;
        SslContext sslContextClientCert;

        HttpClient httpClientNoCert = mock(HttpClient.class);
        HttpClient httpClientWithCert = mock(HttpClient.class);

        @BeforeAll
        void initHttpClients() throws SSLException {
            sslContextNoCert = SslContextBuilder.forClient().build();
            sslContextClientCert = SslContextBuilder.forClient().build();

        }

        void setUpClient(HttpClient client) {
            when(client.option(any(), anyInt())).thenReturn(client);
            when(client.responseTimeout(any())).thenReturn(client);
        }


        @Nested
        class GetHttpClient {

            NettyRoutingFilterApiml nettyRoutingFilterApiml;
            private final Route ROUTE_NO_TIMEOUT = Route.async()
                .id("1").uri("http://localhost/").predicate(serverWebExchange -> true)
                .build();
            private final Route ROUTE_TIMEOUT = Route.async()
                .id("2").uri("http://localhost/").predicate(serverWebExchange -> true).metadata(CONNECT_TIMEOUT_ATTR, "100")
                .build();
            MockServerWebExchange serverWebExchange;

            @BeforeEach
            void initMocks() {
                nettyRoutingFilterApiml = new NettyRoutingFilterApiml(httpClientNoCert, httpClientWithCert, null, null);
                ReflectionTestUtils.setField(nettyRoutingFilterApiml, "requestTimeout", 60000);

                MockServerHttpRequest mockServerHttpRequest = MockServerHttpRequest.get("/path").build();
                serverWebExchange = MockServerWebExchange.from(mockServerHttpRequest);
            }

            @Test
            void givenNoTimeoutAndNoRequirementsForClientCert_whenGetHttpClient_thenCallWithoutClientCert() {
                setUpClient(httpClientNoCert);
                assertSame(httpClientNoCert, nettyRoutingFilterApiml.getHttpClient(ROUTE_NO_TIMEOUT, serverWebExchange));
            }

            @Test
            void givenNoTimeoutAndFalseAsRequirementsForClientCert_whenGetHttpClient_thenCallWithoutClientCert() {
                setUpClient(httpClientNoCert);
                serverWebExchange.getAttributes().put(HTTP_CLIENT_USE_CLIENT_CERTIFICATE, Boolean.FALSE);
                assertSame(httpClientNoCert, nettyRoutingFilterApiml.getHttpClient(ROUTE_NO_TIMEOUT, serverWebExchange));
            }

            @Test
            void givenNoTimeoutAndRequirementsForClientCert_whenGetHttpClient_thenCallWithoutClientCert() {
                setUpClient(httpClientWithCert);
                serverWebExchange.getAttributes().put(HTTP_CLIENT_USE_CLIENT_CERTIFICATE, Boolean.TRUE);
                assertSame(httpClientWithCert, nettyRoutingFilterApiml.getHttpClient(ROUTE_NO_TIMEOUT, serverWebExchange));
            }

            @Test
            void givenTimeoutAndNoRequirementsForClientCert_whenGetHttpClient_thenCallWithoutClientCert() {
                setUpClient(httpClientNoCert);
                nettyRoutingFilterApiml.getHttpClient(ROUTE_TIMEOUT, serverWebExchange);
                verify(httpClientNoCert).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 100);
            }

            @Test
            void givenTimeoutAndRequirementsForClientCert_whenGetHttpClient_thenCallWithoutClientCert() {
                setUpClient(httpClientWithCert);
                serverWebExchange.getAttributes().put(HTTP_CLIENT_USE_CLIENT_CERTIFICATE, Boolean.TRUE);
                nettyRoutingFilterApiml.getHttpClient(ROUTE_TIMEOUT, serverWebExchange);
                verify(httpClientWithCert).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 100);
            }

        }

    }

}
