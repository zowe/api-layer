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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
    private WebSocketRoutedSessionFactory webSocketRoutedSessionFactory;
    private Map<String, WebSocketRoutedSession> routedSessions;

    @BeforeEach
    public void setup() {
        discoveryClient = mock(DiscoveryClient.class);
        routedSessions = new HashMap<>();
        webSocketRoutedSessionFactory = mock(WebSocketRoutedSessionFactory.class);

        underTest = new WebSocketProxyServerHandler(
            discoveryClient,
            mock(WebSocketClientFactory.class),
            routedSessions,
            webSocketRoutedSessionFactory
        );
    }



    private ServiceInstance validServiceInstance() {
        ServiceInstance validService = mock(ServiceInstance.class);
        when(validService.getHost()).thenReturn("gatewayHost");
        when(validService.isSecure()).thenReturn(true);
        when(validService.getPort()).thenReturn(1443);

        return validService;
    }

    @Nested
    class WhenTheConnectionIsEstablished {
        WebSocketSession establishedSession;

        @BeforeEach
        void prepareSessionMock() {
            establishedSession = mock(WebSocketSession.class);
        }

        @Nested
        class ThenTheValidSessionIsStoredInternally {
            @BeforeEach
            void prepareRoutedService() {
                String serviceId = "valid-service";

                RoutedServices routesForSpecificValidService = mock(RoutedServices.class);
                when(routesForSpecificValidService.findServiceByGatewayUrl("ws/v1"))
                    .thenReturn(new RoutedService("ws-v1", "ws/v1", "/valid-service/ws/v1"));
                ServiceInstance foundService = validServiceInstance();
                when(discoveryClient.getInstances(serviceId)).thenReturn(Collections.singletonList(foundService));

                underTest.addRoutedServices(serviceId, routesForSpecificValidService);
            }

            /**
             * Happy Path
             * <p>
             * The Handler is properly created
             * Specified Route is added to the list
             * The connection is established
             * The URI contains the valid service Id
             * The service associated with given URI is retrieved
             * Proper WebSocketSession is stored.
             */
            @ParameterizedTest(name = "WhenTheConnectionIsEstablished.ThenTheValidSessionIsStoredInternally#givenValidRoute {0}")
            @ValueSource(strings = {"wss://gatewayHost:1443/valid-service/ws/v1/valid-path", "wss://gatewayHost:1443/ws/v1/valid-service/valid-path"})
            void givenValidRoute(String path) throws Exception {
                when(webSocketRoutedSessionFactory.session(any(), any(), any())).thenReturn(mock(WebSocketRoutedSession.class));

                String establishedSessionId = "validAndUniqueId";
                when(establishedSession.getId()).thenReturn(establishedSessionId);
                when(establishedSession.getUri()).thenReturn(new URI(path));

                underTest.afterConnectionEstablished(establishedSession);

                verify(webSocketRoutedSessionFactory).session(any(), any(), any());
                WebSocketRoutedSession preparedSession = routedSessions.get(establishedSessionId);
                assertThat(preparedSession, is(notNullValue()));
            }
        }


        @Nested
        class ThenTheSocketIsClosed {
            @BeforeEach
            void sessionIsOpen() {
                when(establishedSession.isOpen()).thenReturn(true);
            }

            /**
             * Error Path
             * <p>
             * The Handler is properly created
             * The connection is established
             * The URI doesn't contain all needed parts
             * The WebSocketSession is closed
             */
            @Test
            void givenInvalidURI() throws Exception {
                when(establishedSession.getUri()).thenReturn(new URI("wss://gatewayHost:1443/invalidUrl"));

                underTest.afterConnectionEstablished(establishedSession);

                verify(establishedSession).close(new CloseStatus(CloseStatus.NOT_ACCEPTABLE.getCode(), "Invalid URL format"));
            }

            /**
             * Error Path
             * <p>
             * The Handler is properly created
             * The connection is established
             * The URI contains the service Id for which there is no service
             * The WebSocketSession is closed
             */
            @Test
            void givenInvalidRoute() throws Exception {
                when(establishedSession.getUri()).thenReturn(new URI("wss://gatewayHost:1443/ws/v1/non_existent_service/valid-path"));

                underTest.afterConnectionEstablished(establishedSession);

                verify(establishedSession).close(new CloseStatus(CloseStatus.NOT_ACCEPTABLE.getCode(), "Requested service non_existent_service is not known by the gateway"));
            }

            /**
             * Error Path
             * <p>
             * The Handler is properly created
             * Specified Route is added to the list
             * The connection is established
             * The URI contains the valid service Id
             * The service associated with given URI is retrieved
             * The service isn't available in the Discovery Service
             * Proper WebSocketSession is stored.
             */
            @Test
            void givenNoInstanceOfTheServiceIsInTheRepository() throws Exception {
                when(establishedSession.getUri()).thenReturn(new URI("wss://gatewayHost:1443/service-without-instance/ws/v1/valid-path"));

                RoutedServices routesForSpecificValidService = mock(RoutedServices.class);
                when(routesForSpecificValidService.findServiceByGatewayUrl("ws/v1"))
                    .thenReturn(new RoutedService("api-v1", "api/v1", "/api-v1/api/v1"));
                underTest.addRoutedServices("service-without-instance", routesForSpecificValidService);

                underTest.afterConnectionEstablished(establishedSession);

                verify(establishedSession).close(new CloseStatus(CloseStatus.SERVICE_RESTARTED.getCode(), "Requested service service-without-instance does not have available instance"));
            }
        }
    }

    @Nested
    class GivenValidExistingSession {
        WebSocketSession establishedSession;
        WebSocketRoutedSession internallyStoredSession;

        @BeforeEach
        void prepareSessionMock() {
            establishedSession = mock(WebSocketSession.class);
            String validSessionId = "123";
            when(establishedSession.getId()).thenReturn(validSessionId);

            internallyStoredSession = mock(WebSocketRoutedSession.class);
            routedSessions.put(validSessionId, internallyStoredSession);
        }

        @Test
        void whenTheConnectionIsClosed_thenTheSessionIsClosedAndRemovedFromRepository() throws Exception {
            CloseStatus normalClose = CloseStatus.NORMAL;

            underTest.afterConnectionClosed(establishedSession, normalClose);

            verify(establishedSession).close(normalClose);
            assertThat(routedSessions.entrySet(), hasSize(0));
        }

        @Test
        void whenTheMessageIsReceived_thenTheMessageIsPassedToTheSession() throws Exception {
            WebSocketMessage<String> passedMessage = mock(WebSocketMessage.class);

            underTest.handleMessage(establishedSession, passedMessage);

            verify(internallyStoredSession).sendMessageToServer(passedMessage);
        }

    }
}
