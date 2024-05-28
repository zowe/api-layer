/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.ws;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.jetty.JettyWebSocketClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class WebSocketRoutedSessionTest {
    private WebSocketSession clientSession;
    private WebSocketSession serverSession;

    private WebSocketRoutedSession underTest;

    @BeforeEach
    void prepareSessionUnderTest() {
        clientSession = mock(WebSocketSession.class);
        serverSession = mock(WebSocketSession.class);

        underTest = new WebSocketRoutedSession(serverSession, clientSession);
    }

    @Test
    void givenValidServerAndClientSession_whenTheDetailsAreRequested_thenTheDetailsAreReturnedAsStrings() throws Exception {
        String sessionId = "123";
        String clientUriPath = "ws://localhost:8080/petstore";
        String serverUriPath = "ws://gateway:8080/petstore";

        when(clientSession.getId()).thenReturn(sessionId);
        when(clientSession.getUri()).thenReturn(new URI(clientUriPath));
        InetSocketAddress unresolvedAddress = mock(InetSocketAddress.class);
        when(unresolvedAddress.toString()).thenReturn("gateway:8080");

        when(serverSession.getRemoteAddress()).thenReturn(unresolvedAddress);
        when(serverSession.getUri()).thenReturn(new URI(serverUriPath));

        assertThat(underTest.getClientId(), is(sessionId));
        assertThat(underTest.getClientUri(), is(clientUriPath));

        assertThat(underTest.getServerRemoteAddress(), is("gateway:8080"));
        assertThat(underTest.getServerUri(), is(serverUriPath));
    }

    @Test
    void givenBrokenServerSession_whenUriIsRequested_NullIsReturned() {
        when(serverSession.getUri()).thenReturn(null);
        assertThat(underTest.getServerUri(), is(nullValue()));
    }

    @Test
    void givenBrokenClientSession_whenUriIsRequested_NullIsReturned() {
        when(clientSession.getUri()).thenReturn(null);
        assertThat(underTest.getClientUri(), is(nullValue()));
    }

    @Nested
    class GivenFirstConstructor {
        @Test
        void whenFailingToCreateSession_thenThrowException() {
            WebSocketClientFactory webSocketClientFactory = mock(WebSocketClientFactory.class);
            JettyWebSocketClient jettyWebSocketClient = mock(JettyWebSocketClient.class);
            when(webSocketClientFactory.getClientInstance()).thenReturn(jettyWebSocketClient);
            HttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("header", "someHeader");
            when(serverSession.getHandshakeHeaders()).thenReturn(headers);
            assertThrows(WebSocketProxyError.class, () -> new WebSocketRoutedSession(serverSession, "", webSocketClientFactory));

        }

        @Test
        void whenFailingOnHandshake_thenThrowException() {
            WebSocketClientFactory webSocketClientFactory = mock(WebSocketClientFactory.class);
            when(webSocketClientFactory.getClientInstance()).thenThrow(new IllegalStateException());
            assertThrows(WebSocketProxyError.class, () -> new WebSocketRoutedSession(serverSession, "", webSocketClientFactory));
        }
    }

    @Nested
    class GivenWSMessage {
        @Test
        void whenAddressNotNull_thenSendMessage() throws IOException {
            underTest.sendMessageToServer(mock(WebSocketMessage.class));
            verify(clientSession, times(1)).sendMessage(any());
        }
    }

    @Nested
    class GivenServerRemoteAddress {
        @Test
        void whenAddressNotNull_thenReturnIt() {
            InetSocketAddress unresolvedAddress = mock(InetSocketAddress.class);
            when(unresolvedAddress.toString()).thenReturn("gateway:8080");
            when(serverSession.getRemoteAddress()).thenReturn(unresolvedAddress);
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
