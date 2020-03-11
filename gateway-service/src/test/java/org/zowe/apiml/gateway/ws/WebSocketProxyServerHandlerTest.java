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
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketMessage;
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
import static org.hamcrest.Matchers.hasSize;
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
        when(discoveryClient.getInstances("api-v1")).thenReturn(Collections.singletonList(foundService));
        underTest.addRoutedServices("api-v1", routesForSpecificValidService);
        when(webSocketRoutedSessionFactory.session(any(), any(), any())).thenReturn(mock(WebSocketRoutedSession.class));

        WebSocketSession establishedSession = mock(WebSocketSession.class);
        String establishedSessionId = "validAndUniqueId";
        when(establishedSession.getId()).thenReturn(establishedSessionId);
        when(establishedSession.getUri()).thenReturn(new URI("wss://gatewayHost:1443/gateway/1/api-v1/api/v1"));
        underTest.afterConnectionEstablished(establishedSession);

        verify(webSocketRoutedSessionFactory).session(any(), any(), any());
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

    /**
     * Error Path
     *
     * The Handler is properly created
     * The connection is established
     * The URI doesn't contain all needed parts
     * The WebSocketSession is closed
     */
    @Test
    public void givenInvalidURI_whenTheConnectionIsEstablished_thenTheSocketIsClosedAsNotAcceptable() throws Exception {
        WebSocketSession establishedSession = mock(WebSocketSession.class);
        when(establishedSession.isOpen()).thenReturn(true);
        when(establishedSession.getUri()).thenReturn(new URI("wss://gatewayHost:1443/invalidUrl"));

        underTest.afterConnectionEstablished(establishedSession);

        verify(establishedSession).close(new CloseStatus(CloseStatus.NOT_ACCEPTABLE.getCode(), "Invalid URL format"));
    }

    /**
     * Error Path
     *
     * The Handler is properly created
     * The connection is established
     * The URI contains the service Id for which there is no service
     * The WebSocketSession is closed
     */
    @Test
    public void givenInvalidRoute_whenTheConnectionIsEstablished_thenTheSocketIsClosedAsNotAcceptable() throws Exception {
        WebSocketSession establishedSession = mock(WebSocketSession.class);
        when(establishedSession.isOpen()).thenReturn(true);
        when(establishedSession.getUri()).thenReturn(new URI("wss://gatewayHost:1443/api/v1/non_existent_service/api/v1"));

        underTest.afterConnectionEstablished(establishedSession);

        verify(establishedSession).close(new CloseStatus(CloseStatus.NOT_ACCEPTABLE.getCode(), "Requested service non_existent_service is not known by the gateway"));
    }

    /**
     * Error Path
     *
     * The Handler is properly created
     * Specified Route is added to the list
     * The connection is established
     * The URI contains the valid service Id
     * The service associated with given URI is retrieved
     * The service isn't available in the Discovery Service
     * Proper WebSocketSession is stored.
     */
    @Test
    public void givenNoInstanceOfTheServiceIsInTheRepository_whenTheConnectionIsEstablished_thenTheSocketIsClosedAsServiceRestarted() throws Exception {
        WebSocketSession establishedSession = mock(WebSocketSession.class);
        when(establishedSession.isOpen()).thenReturn(true);
        when(establishedSession.getUri()).thenReturn(new URI("wss://gatewayHost:1443/api/v1/api-v1/api/v1"));
        RoutedServices routesForSpecificValidService = mock(RoutedServices.class);
        when(routesForSpecificValidService.findServiceByGatewayUrl("ws/v1"))
            .thenReturn(new RoutedService("api-v1", "api/v1", "/api-v1/api/v1"));
        underTest.addRoutedServices("api-v1", routesForSpecificValidService);

        underTest.afterConnectionEstablished(establishedSession);

        verify(establishedSession).close(new CloseStatus(CloseStatus.SERVICE_RESTARTED.getCode(), "Requested service api-v1 does not have available instance"));
    }

    @Test
    public void givenValidSession_whenTheConnectionIsClosed_thenTheSessionIsClosedAndRemovedFromRepository() throws Exception {
        CloseStatus normalClose = CloseStatus.NORMAL;
        WebSocketSession establishedSession = mock(WebSocketSession.class);
        String validSessionId = "123";
        when(establishedSession.getId()).thenReturn(validSessionId);
        routedSessions.put(validSessionId, mock(WebSocketRoutedSession.class));

        underTest.afterConnectionClosed(establishedSession, normalClose);

        verify(establishedSession).close(normalClose);
        assertThat(routedSessions.entrySet(), hasSize(0));
    }

    @Test
    public void givenValidSession_whenTheMessageIsReceived_thenTheMessageIsPassedToTheSession() throws Exception {
        WebSocketSession establishedSession = mock(WebSocketSession.class);
        String validSessionId = "123";
        when(establishedSession.getId()).thenReturn(validSessionId);
        WebSocketRoutedSession internallyStoredSession = mock(WebSocketRoutedSession.class);
        routedSessions.put(validSessionId, internallyStoredSession);
        WebSocketMessage<String> passedMessage = mock(WebSocketMessage.class);

        underTest.handleMessage(establishedSession, passedMessage);

        verify(internallyStoredSession).sendMessageToServer(passedMessage);
    }
}
