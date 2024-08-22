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

import org.eclipse.jetty.websocket.api.WebSocketException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.zowe.apiml.product.routing.RoutedService;
import org.zowe.apiml.product.routing.RoutedServices;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketProxyServerHandlerTest {
    private WebSocketProxyServerHandler underTest;
    private WebSocketRoutedSessionFactory webSocketRoutedSessionFactory;
    private Map<String, WebSocketRoutedSession> routedSessions;
    private LoadBalancerClient lbClient;

    @BeforeEach
    public void setup() {
        routedSessions = new HashMap<>();
        webSocketRoutedSessionFactory = mock(WebSocketRoutedSessionFactory.class);
        lbClient = mock(LoadBalancerClient.class);

        underTest = new WebSocketProxyServerHandler(
            mock(WebSocketClientFactory.class),
            routedSessions,
            webSocketRoutedSessionFactory,
            lbClient
        );
        ReflectionTestUtils.setField(underTest, "meAsProxy", underTest);
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
            @Test
             void givenValidRoute() throws Exception {
                String path = "wss://gatewayHost:1443/valid-service/ws/v1/valid-path";
                when(webSocketRoutedSessionFactory.session(any(), any(), any())).thenReturn(mock(WebSocketRoutedSession.class));
                ServiceInstance serviceInstance = mock(ServiceInstance.class);
                when(lbClient.choose(any())).thenReturn(serviceInstance);
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
                when(establishedSession.getUri()).thenReturn(new URI("wss://gatewayHost:1443/non_existent_service/ws/v1/valid-path"));

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
                when(lbClient.choose(any())).thenReturn(null);
                RoutedServices routesForSpecificValidService = mock(RoutedServices.class);
                when(routesForSpecificValidService.findServiceByGatewayUrl("ws/v1"))
                    .thenReturn(new RoutedService("api-v1", "api/v1", "/api-v1/api/v1"));
                underTest.addRoutedServices("service-without-instance", routesForSpecificValidService);

                underTest.afterConnectionEstablished(establishedSession);

                verify(establishedSession).close(new CloseStatus(CloseStatus.SERVICE_RESTARTED.getCode(), "Requested service service-without-instance does not have available instance"));
            }

            @Test
            void givenNullService_thenCloseWebSocket() throws Exception {
                when(establishedSession.getUri()).thenReturn(new URI("wss://gatewayHost:1443/service-without-instance/ws/v1/valid-path"));
                when(lbClient.choose(any())).thenReturn(null);
                RoutedServices routesForSpecificValidService = mock(RoutedServices.class);
                when(routesForSpecificValidService.findServiceByGatewayUrl("ws/v1"))
                    .thenReturn(null);
                underTest.addRoutedServices("service-without-instance", routesForSpecificValidService);

                underTest.afterConnectionEstablished(establishedSession);

                verify(establishedSession).close(new CloseStatus(CloseStatus.NOT_ACCEPTABLE.getCode(), "Requested ws/v1 url is not known by the gateway"));
            }
        }
    }

    @Nested
    class GivenValidExistingSession {
        WebSocketSession establishedSession;
        WebSocketRoutedSession internallyStoredSession;
        WebSocketMessage<String> passedMessage;

        @BeforeEach
        void prepareSessionMock() {
            establishedSession = mock(WebSocketSession.class);
            String validSessionId = "123";
            when(establishedSession.getId()).thenReturn(validSessionId);
            passedMessage = mock(WebSocketMessage.class);
            internallyStoredSession = mock(WebSocketRoutedSession.class);
            routedSessions.put(validSessionId, internallyStoredSession);
        }

        @Test
        void whenTheConnectionIsClosed_thenTheSessionIsClosedAndRemovedFromRepository() {
            CloseStatus normalClose = CloseStatus.NORMAL;

            assertThat(routedSessions.entrySet(), not(empty()));

            underTest.afterConnectionClosed(establishedSession, normalClose);

            assertThat(routedSessions.entrySet(), hasSize(0));
        }

        @Test
        void whenTheConnectionIsClosed_thenClientSessionIsAlsoClosed() throws IOException {
            CloseStatus normalClose = CloseStatus.NORMAL;
            WebSocketSession clientSession = mock(WebSocketSession.class);
            when(internallyStoredSession.getWebSocketClientSession()).thenReturn(AsyncResult.forValue(clientSession));

            underTest.afterConnectionClosed(establishedSession, normalClose);
            verify(clientSession, times(1)).close(normalClose);
            assertThat(routedSessions.entrySet(), hasSize(0));
        }

        @Test
        void whenTheMessageIsReceived_thenTheMessageIsPassedToTheSession() throws Exception {
            underTest.handleMessage(establishedSession, passedMessage);

            verify(internallyStoredSession).sendMessageToServer(passedMessage);
        }

        @Test
        void whenExceptionIsThrown_thenRemoveRoutedSession() throws Exception {
            doThrow(new WebSocketException("error")).when(routedSessions.get("123")).sendMessageToServer(passedMessage);
            underTest.handleMessage(establishedSession, passedMessage);
            assertTrue(routedSessions.isEmpty());
        }

        @Test
        void whenSessionIsNull_thenCloseAndReturn() throws IOException {
            routedSessions.replace("123", null);

            underTest.handleMessage(establishedSession, passedMessage);
            assertTrue(routedSessions.isEmpty());
            verify(establishedSession, times(1)).close(CloseStatus.SESSION_NOT_RELIABLE);
        }

        @Test
        void whenClosingSessionThrowException_thenCatchIt() throws IOException {
            CloseStatus status = CloseStatus.SESSION_NOT_RELIABLE;
            doThrow(new IOException()).when(establishedSession).close(status);
            underTest.afterConnectionClosed(establishedSession, status);
            assertTrue(routedSessions.isEmpty());
        }

        @Test
        void whenClosingRoutedSessionThrowException_thenCatchIt() throws IOException {
            CloseStatus status = CloseStatus.SESSION_NOT_RELIABLE;
            doThrow(new IOException()).when(routedSessions.get("123")).close(status);
            underTest.afterConnectionClosed(establishedSession, status);
            assertTrue(routedSessions.isEmpty());
        }

    }

    @Nested
    class WhenGettingRoutedSessions {
        @Test
        void thenReturnThem() {
            Map<String, WebSocketRoutedSession> expectedRoutedSessions = underTest.getRoutedSessions();
            assertThat(expectedRoutedSessions, is(routedSessions));
        }
    }

    @Nested
    class WhenGettingSubProtocols {
        @Test
        void thenReturnThem() {
            List<String> protocol = new ArrayList<>();
            protocol.add("protocol");
            ReflectionTestUtils.setField(underTest, "subProtocols", protocol);
            List<String> subProtocols = underTest.getSubProtocols();
            assertThat(subProtocols, is(protocol));
        }
    }
}
