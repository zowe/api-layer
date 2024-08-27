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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.jetty.JettyWebSocketClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketRoutedSessionTest {

    @Mock
    private WebSocketSession clientSession;
    @Mock
    private WebSocketSession serverSession;
    @Mock
    private WebSocketProxyClientHandler clientHandler;
    @Mock
    private ClientSessionSuccessCallback successCallback;
    @Mock
    private ClientSessionFailureCallback failureCallback;

    private WebSocketRoutedSession underTest;

    @BeforeEach
    void prepareSessionUnderTest() {
        underTest = new WebSocketRoutedSession(serverSession, clientSession, clientHandler, "ws://localhost:8080/petstore");
    }

    @Test
    void givenValidServerAndClientSession_whenTheDetailsAreRequested_thenTheDetailsAreReturnedAsStrings() throws Exception {
        String sessionId = "123";
        String clientUriPath = "ws://localhost:8080/petstore";
        String serverUriPath = "ws://gateway:8080/petstore";

        when(clientSession.isOpen()).thenReturn(true);
        when(clientSession.getId()).thenReturn(sessionId);
        when(serverSession.getRemoteAddress()).thenReturn(new InetSocketAddress("gateway",  8080));
        when(serverSession.getUri()).thenReturn(new URI(serverUriPath));

        assertThat(underTest.getClientId(), is(sessionId));
        assertThat(underTest.getClientUri(), is(clientUriPath));

        assertThat(underTest.getServerRemoteAddress(), is("gateway:8080"));
        assertThat(underTest.getServerUri(), is(serverUriPath));
    }

    @Test
    void givenNoClientSession_whenGetClientId_thenThrowException() {
        ReflectionTestUtils.setField(underTest, "clientSession", new AtomicReference<>());
        assertThrows(ServerNotYetAvailableException.class, () -> underTest.getClientId());
    }

    @Test
    void givenClientSessionNotConnected_whenGetClientId_thenNull() {
        when(clientSession.isOpen()).thenReturn(false);
        assertNull(underTest.getClientId());
    }

    @Test
    void givenBrokenServerSession_whenUriIsRequested_NullIsReturned() {
        when(serverSession.getUri()).thenReturn(null);
        assertThat(underTest.getServerUri(), is(nullValue()));
    }

    @Nested
    class GivenFirstConstructor {

        @Test
        void whenFailingToCreateSession_thenThrowException() {
            WebSocketClientFactory webSocketClientFactory = mock(WebSocketClientFactory.class);
            JettyWebSocketClient jettyWebSocketClient = mock(JettyWebSocketClient.class);
            when(webSocketClientFactory.getClientInstance("key")).thenReturn(jettyWebSocketClient);
            HttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("header", "someHeader");
            when(serverSession.getHandshakeHeaders()).thenReturn(headers);
            assertThrows(WebSocketProxyError.class, () -> new WebSocketRoutedSession(serverSession, "", webSocketClientFactory, successCallback, failureCallback));

        }

        @Test
        void whenFailingOnHandshake_thenThrowException() {
            WebSocketClientFactory webSocketClientFactory = mock(WebSocketClientFactory.class);
            when(webSocketClientFactory.getClientInstance("key")).thenThrow(new IllegalStateException());
            assertThrows(WebSocketProxyError.class, () -> new WebSocketRoutedSession(serverSession, "", webSocketClientFactory, successCallback, failureCallback));
        }
    }

    @Nested
    class GivenWSMessage {

        @Test
        void whenAddressNotNull_thenSendMessage() throws IOException {
            when(clientSession.isOpen()).thenReturn(true);

            underTest.sendMessageToServer(mock(WebSocketMessage.class));

            verify(clientSession, times(1)).sendMessage(any());
        }

        @Test
        void whenClientNotConnected_throwNotAvailable() throws IOException {
            when(clientSession.isOpen()).thenReturn(false);

            assertThrows(ServerNotYetAvailableException.class, () -> underTest.sendMessageToServer(mock(WebSocketMessage.class)));
        }

        @Test
        void whenClientConnectedAndException_thenThrowIt() throws IOException {
            when(clientSession.isOpen()).thenReturn(true);
            Exception e = new IOException("message");
            doThrow(e).when(clientSession).sendMessage(any());

            assertThrowsExactly(IOException.class, () -> underTest.sendMessageToServer(mock(WebSocketMessage.class)), "message");
        }

    }

    @Nested
    class GivenServerRemoteAddress {

        @Test
        void whenAddressNotNull_thenReturnIt() {
            when(serverSession.getRemoteAddress()).thenReturn(new InetSocketAddress("gateway",  8080));
            String serverRemoteAddress = underTest.getServerRemoteAddress();
            assertThat(serverRemoteAddress, is("gateway:8080"));
        }

        @Test
        void whenAddressNull_thenReturnNull() {
            when(serverSession.getRemoteAddress()).thenReturn(null);
            assertNull((underTest.getServerRemoteAddress()));
        }

    }

    @Nested
    class GivenCloseCall {

        @Test
        void whenClosingSession_thenClose() throws IOException {
            CloseStatus closeStatus = new CloseStatus(CloseStatus.NOT_ACCEPTABLE.getCode());
            when(clientSession.isOpen()).thenReturn(true);
            underTest.close(closeStatus);
            verify(clientSession, times(1)).close(closeStatus);
        }

    }

}
