/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.config;

import com.sun.net.httpserver.HttpServer;
import io.netty.handler.ssl.SslContext;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.cloud.gateway.config.HttpClientProperties;
import org.springframework.cloud.gateway.config.HttpClientSslConfigurer;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.product.web.HttpConfig;
import org.zowe.apiml.security.common.util.ConnectionUtil;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;

import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebClientConfigTest {

    private static final String OWNED_ADDRESS = "127.0.0.1";
    private static final String NOT_OWNED_ADDRESS = "192.0.2.1";

    @Mock
    private NetworkInterface interface0;
    @Mock
    private NetworkInterface interface1;
    @Mock
    private InterfaceAddress interfaceAddress0;
    @Mock
    private InterfaceAddress interfaceAddress1;

    @Mock
    private HttpConfig httpConfig;
    @Mock
    private HttpClientSslConfigurer sslConfigurer;

    private WebClientConfig webClientConfig;
    private HttpServer localServer;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(interface0.getInterfaceAddresses()).thenReturn(List.of(interfaceAddress0));
        lenient().when(interface1.getInterfaceAddresses()).thenReturn(List.of(interfaceAddress1));
        lenient().when(interfaceAddress0.getAddress()).thenReturn(InetAddress.getByName(NOT_OWNED_ADDRESS));
        lenient().when(interfaceAddress1.getAddress()).thenReturn(InetAddress.getByName(OWNED_ADDRESS));

        webClientConfig = new WebClientConfig(httpConfig);
        when(sslConfigurer.configureSsl(any())).thenAnswer(invocation -> invocation.getArgument(0));

        localServer = HttpServer.create(new InetSocketAddress(OWNED_ADDRESS, 0), 0);
        localServer.createContext("/", exchange -> {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        localServer.start();
    }

    @AfterEach
    void tearDown() {
        localServer.stop(0);
    }

    @Test
    void givenSpecificAddress_whenHttpClientConnects_thenItBindsToTheMatchingInterfaceAndConnects() throws Exception {
        try (
            var mockedNetworkInterface = mockStatic(NetworkInterface.class);
            var mockedConnectionUtil = mockStatic(ConnectionUtil.class)
        ) {
            mockedNetworkInterface.when(NetworkInterface::getNetworkInterfaces)
                .thenReturn(Collections.enumeration(List.of(interface0, interface1)));
            mockedConnectionUtil.when(() -> ConnectionUtil.getSslContext(httpConfig, false))
                .thenReturn(mock(SslContext.class));

            var selectedAddress = OWNED_ADDRESS;
            ReflectionTestUtils.setField(webClientConfig, "listenAddress", selectedAddress);

            var httpClient = buildClientWithFactory();
            var response = connect(httpClient).block(Duration.ofSeconds(5));

            assertEquals(200, response.status().code());
        }
    }

    @Test
    void givenAddressNotOwnedByThisHost_whenHttpClientConnects_thenBindFails() throws Exception {
        try (
            var mockedNetworkInterface = mockStatic(NetworkInterface.class);
            var mockedConnectionUtil = mockStatic(ConnectionUtil.class)
        ) {
            mockedNetworkInterface.when(NetworkInterface::getNetworkInterfaces)
                .thenReturn(Collections.enumeration(List.of(interface0, interface1)));
            mockedConnectionUtil.when(() -> ConnectionUtil.getSslContext(httpConfig, false))
                .thenReturn(mock(SslContext.class));

            var otherAddress = NOT_OWNED_ADDRESS;
            ReflectionTestUtils.setField(webClientConfig, "listenAddress", otherAddress);

            var httpClient = buildClientWithFactory();

            var connectionAttempt = assertThrows(Exception.class, () -> connect(httpClient).block(Duration.ofSeconds(5)));

            assertInstanceOf(BindException.class, ExceptionUtils.getRootCause(connectionAttempt));
        }
    }

    private Mono<HttpClientResponse> connect(HttpClient httpClient) {
        return httpClient.get()
            .uri("http://" + OWNED_ADDRESS + ":" + localServer.getAddress().getPort() + "/")
            .response();
    }

    private HttpClient buildClientWithFactory() throws Exception {
        var properties = new HttpClientProperties();
        properties.getPool().setType(HttpClientProperties.Pool.PoolType.DISABLED);

        var factory = webClientConfig.gatewayHttpClientFactory(
            properties, new ServerProperties(), Collections.emptyList(), sslConfigurer
        );
        factory.afterPropertiesSet();
        return (HttpClient) factory.getObject();
    }

}
