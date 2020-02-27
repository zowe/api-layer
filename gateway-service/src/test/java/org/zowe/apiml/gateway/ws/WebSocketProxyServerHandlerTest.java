/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.ws;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.socket.WebSocketSession;
import org.zowe.apiml.product.routing.RoutedService;
import org.zowe.apiml.product.routing.RoutedServices;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WebSocketProxyServerHandlerTest {
    private WebSocketProxyServerHandler underTest;
    private DiscoveryClient discoveryClient;
    private SslContextFactoryProvider sslContextFactoryProvider;
    private WebSocketRoutedSessionFactory webSocketRoutedSessionFactory;
    private Map<String, WebSocketRoutedSession> routedSessions;

    @BeforeEach
    public void setup() {
        discoveryClient = mock(DiscoveryClient.class);
        sslContextFactoryProvider = mock(SslContextFactoryProvider.class);
        routedSessions = new HashMap<>();
        webSocketRoutedSessionFactory = mock(WebSocketRoutedSessionFactory.class);

        underTest = new WebSocketProxyServerHandler(
            discoveryClient,
            sslContextFactoryProvider,
            routedSessions,
            webSocketRoutedSessionFactory
        );
    }

    /**
     * Happy Path
     *
     * The Handler is properly created
     * Specified Route is added to the list
     * The connection is established
     * The URI contains the valid service Id
     * The service associated with given URI is retrieved
     * Proper WebSocketSession is stored.
     */
    @Test
    public void givenValidRoute_whenTheConnectionIsEstablished_thenTheValidSessionIsStoredInternally() throws Exception {
        RoutedServices routesForSpecificValidService = mock(RoutedServices.class);
        when(routesForSpecificValidService.findServiceByGatewayUrl("ws/1"))
            .thenReturn(new RoutedService("api-v1", "api/v1", "/api-v1/api/v1"));
        ServiceInstance foundService = validServiceInstance();
        when(webSocketRoutedSessionFactory.session(any(), any(), any())).thenReturn(mock(WebSocketRoutedSession.class));
        when(discoveryClient.getInstances("api-v1")).thenReturn(Collections.singletonList(foundService));
        underTest.addRoutedServices("api-v1", routesForSpecificValidService);

        WebSocketSession establishedSession = mock(WebSocketSession.class);
        String establishedSessionId = "validAndUniqueId";
        when(establishedSession.getId()).thenReturn(establishedSessionId);
        when(establishedSession.getUri()).thenReturn(new URI("wss://gatewayHost:1443/gateway/1/api-v1/api/v1"));
        underTest.afterConnectionEstablished(establishedSession);

        verify(webSocketRoutedSessionFactory, times(1)).session(any(), any(), any());
        WebSocketRoutedSession preparedSession = routedSessions.get(establishedSessionId);
        assertThat(preparedSession, is(notNullValue()));
    }

    private ServiceInstance validServiceInstance() {
        ServiceInstance validService = mock(ServiceInstance.class);
        when(validService.getHost()).thenReturn("gatewayHost");
        when(validService.isSecure()).thenReturn(true);
        when(validService.getPort()).thenReturn(1443);

        return validService;
    }
}
