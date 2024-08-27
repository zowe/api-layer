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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
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

        @Mock
        WebSocketSession establishedSession;

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
                WebSocketRoutedSession routedSession = mock(WebSocketRoutedSession.class);
                when(webSocketRoutedSessionFactory.session(any(), any(), any(), any(), any())).thenReturn(routedSession);
                ServiceInstance serviceInstance = mock(ServiceInstance.class);
                when(lbClient.choose(any())).thenReturn(serviceInstance);
                String establishedSessionId = "validAndUniqueId";
                when(establishedSession.getId()).thenReturn(establishedSessionId);
                when(establishedSession.getUri()).thenReturn(new URI(path));

                underTest.onClientSessionSuccess(routedSession, establishedSession, establishedSession);
                underTest.afterConnectionEstablished(establishedSession);

                verify(webSocketRoutedSessionFactory).session(any(), any(), any(), any(), any());
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
                RoutedServices routesForSpecificValidService = mock(RoutedServices.class);
                when(routesForSpecificValidService.findServiceByGatewayUrl("ws/v1"))
                    .thenReturn(null);
                underTest.addRoutedServices("service-without-instance", routesForSpecificValidService);

                underTest.afterConnectionEstablished(establishedSession);

                verify(establishedSession).close(new CloseStatus(CloseStatus.NOT_ACCEPTABLE.getCode(), "Requested ws/v1 url is not known by the gateway"));
            }
        }

        @Test
        void testOnClientSessionSuccess() {
            assumeTrue(underTest.getRoutedSessions().isEmpty());
            when(establishedSession.getId()).thenReturn("mockId");

            underTest.onClientSessionSuccess(mock(WebSocketRoutedSession.class), establishedSession, establishedSession);

            verifyNoMoreInteractions(establishedSession);
            assertNotNull(underTest.getRoutedSessions().get("mockId"));
        }

    }

    @Nested
    class GivenInvalidClientSession {

        @Mock
        private WebSocketSession session;
        @Mock
        private WebSocketRoutedSession routedSession;

        @Test
        void whenHandleMessage_thenThrowNotAvailable() {
            assumeTrue(underTest.getRoutedSessions().isEmpty());
            assertThrows(ServerNotYetAvailableException.class, () -> underTest.handleMessage(session, new TextMessage("null")));
        }

        @Test
        void whenRecover_thenCloseServerSession() throws IOException {
            doNothing().when(session).close(CloseStatus.SESSION_NOT_RELIABLE);
            when(session.getId()).thenReturn("mockId");
            underTest.closeServerSession(null, session, null);

            verifyNoMoreInteractions(session);
        }

        @Test
        void whenFailedToObtain_thenCloseServerSession() throws Exception {
            assumeTrue(underTest.getRoutedSessions().isEmpty());

            underTest.getRoutedSessions().put("mockId", routedSession);

            WebSocketProxyClientHandler clientHandler = mock(WebSocketProxyClientHandler.class);
            when(routedSession.getClientHandler()).thenReturn(clientHandler);
            when(routedSession.getTargetUrl()).thenReturn("targetUrl");
            doNothing().when(clientHandler).handleMessage(eq(session), argThat(tm -> tm.getPayload() instanceof String && String.valueOf(tm.getPayload()).contains("a message")));
            doNothing().when(session).close(CloseStatus.SERVER_ERROR);
            when(session.getId()).thenReturn("mockId");

            underTest.onClientSessionFailure(routedSession, session, new RuntimeException("a message"));

            verifyNoMoreInteractions(session);
            verifyNoMoreInteractions(routedSession);

            assertTrue(underTest.getRoutedSessions().isEmpty());
        }

    }

    @Nested
    class GivenValidExistingSession {

        @Mock
        WebSocketSession establishedSession;
        @Mock
        WebSocketRoutedSession internallyStoredSession;
        @Mock
        WebSocketMessage<String> passedMessage;

        @BeforeEach
        void prepareSessionMock() {
            String validSessionId = "123";
            when(establishedSession.getId()).thenReturn(validSessionId);
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
            when(internallyStoredSession.isClientConnected()).thenReturn(true);
            when(internallyStoredSession.getClientSession()).thenReturn(clientSession);

            underTest.afterConnectionClosed(establishedSession, normalClose);

            verify(clientSession, times(1)).close(normalClose);
            assertThat(routedSessions.entrySet(), hasSize(0));
        }

        @Test
        void whenTheMessageIsReceived_thenTheMessageIsPassedToTheSession() throws Exception {
            WebSocketSession clientSession = mock(WebSocketSession.class);
            when(clientSession.isOpen()).thenReturn(true);
            when(internallyStoredSession.isClientConnected()).thenReturn(true);
            when(internallyStoredSession.getClientSession()).thenReturn(clientSession);

            underTest.handleMessage(establishedSession, passedMessage);

            verify(internallyStoredSession).sendMessageToServer(passedMessage);
        }

        @Test
        void givenClientNotConnected_whenHandleMessage_thenThrowException() {            when(internallyStoredSession.isClientConnected()).thenReturn(false);
            assertThrows(ServerNotYetAvailableException.class, () -> underTest.handleMessage(establishedSession, passedMessage));
        }

        @Test
        void givenClientConnectionClosed_whenHandleMessage_thenCloseServerSession() throws IOException {
            WebSocketSession clientSession = mock(WebSocketSession.class);

            when(clientSession.isOpen()).thenReturn(false);
            when(internallyStoredSession.isClientConnected()).thenReturn(true);
            when(internallyStoredSession.getClientSession()).thenReturn(clientSession);

            underTest.handleMessage(establishedSession, passedMessage);

            verify(establishedSession, times(1)).close(CloseStatus.SESSION_NOT_RELIABLE);
        }

        @Test
        void whenExceptionIsThrown_thenRemoveRoutedSession() throws Exception {
            doThrow(new WebSocketException("error")).when(routedSessions.get("123")).sendMessageToServer(passedMessage);
            when(internallyStoredSession.getClientSession()).thenReturn(establishedSession);
            when(internallyStoredSession.getClientSession()).thenReturn(establishedSession);
            when(internallyStoredSession.isClientConnected()).thenReturn(true);
            when(establishedSession.isOpen()).thenReturn(true);

            underTest.handleMessage(establishedSession, passedMessage);

            assertTrue(routedSessions.isEmpty());
        }

        @Test
        /**
         * This scenario is now handled by Spring Retry
         */
        void whenSessionIsNull_thenCloseAndReturn() {
            routedSessions.replace("123", null);

            assertThrows(ServerNotYetAvailableException.class, () -> underTest.handleMessage(establishedSession, passedMessage));
        }

        @Test
        void whenClosingSessionThrowException_thenCatchIt() throws IOException {
            CloseStatus status = CloseStatus.SESSION_NOT_RELIABLE;
            doThrow(new IOException()).when(establishedSession).close(status);
            when(internallyStoredSession.isClientConnected()).thenReturn(true);
            when(internallyStoredSession.getClientSession()).thenReturn(establishedSession);

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
