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

import org.eclipse.jetty.websocket.api.CloseException;
import org.eclipse.jetty.websocket.api.UpgradeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WebSocketProxyClientHandlerTest {

    @Mock
    private WebSocketSession serverSession;

    private WebSocketProxyClientHandler webSocketProxyClientHandler;

    @BeforeEach
    void setUp() {
        webSocketProxyClientHandler = new WebSocketProxyClientHandler(serverSession);
    }

    @Nested
    class GivenHandler {

        @Nested
        class AndConnectionIsClosed {

            @Test
            void thenCloseServer() throws Exception {
                webSocketProxyClientHandler.afterConnectionClosed(mock(WebSocketSession.class), CloseStatus.NORMAL);
                verify(serverSession, times(1)).close(CloseStatus.NORMAL);
            }

        }

        @Nested
        class AndConnectionTransportError {

            @BeforeEach
            void setUp() {
                doReturn(true).when(serverSession).isOpen();
            }

            @Test
            void andTimeout_thenCloseNormal() throws Exception {
                webSocketProxyClientHandler.handleTransportError(mock(WebSocketSession.class), new CloseException(0, new TimeoutException("null")));
                verify(serverSession, times(1)).close(CloseStatus.NORMAL);
            }

            @Test
            void andCloseException_thenForwardError() throws Exception {
                webSocketProxyClientHandler.handleTransportError(mock(WebSocketSession.class), new CloseException(CloseStatus.PROTOCOL_ERROR.getCode(), new Exception("message")));
                verify(serverSession, times(1)).close(new CloseStatus(1002, "java.lang.Exception: message"));
            }

        }

    }

    @Nested
    class CloseStatuses {

        @Test
        void whenResponseCode5xx_thenServerErrorWithMessage() {
            CloseStatus closeStatus = WebSocketProxyClientHandler.getCloseStatusByResponseStatus(500, "An error message");
            assertEquals(CloseStatus.SERVER_ERROR.getCode(), closeStatus.getCode());
            assertEquals("An error message", closeStatus.getReason());
        }

        @Test
        void whenResponseCode401_thenNotAcceptableWithMessage() {
            CloseStatus closeStatus = WebSocketProxyClientHandler.getCloseStatusByResponseStatus(401, "A default message");
            assertEquals(CloseStatus.NOT_ACCEPTABLE.getCode(), closeStatus.getCode());
            assertEquals("Invalid login credentials", closeStatus.getReason());
        }

        @Test
        void whenUnknownResponseCode_thenNotAcceptableWithMessage() {
            CloseStatus closeStatus = WebSocketProxyClientHandler.getCloseStatusByResponseStatus(405, "A default message");
            assertEquals(CloseStatus.NOT_ACCEPTABLE.getCode(), closeStatus.getCode());
            assertEquals("A default message", closeStatus.getReason());
        }

        @Test
        void whenCloseExceptionWithTimeout_thenNormal() {
            assertEquals(CloseStatus.NORMAL, WebSocketProxyClientHandler.getCloseStatusByError(
                new CloseException(500, mock(TimeoutException.class)))
            );
        }

        @Test
        void whenCloseExceptionWith401_thenNotAcceptable() {
            assertEquals(
                CloseStatus.NOT_ACCEPTABLE.withReason("Invalid login credentials"),
                WebSocketProxyClientHandler.getCloseStatusByError(new CloseException(401, "unauthMsg"))
            );
        }

        @Test
        void whenCloseExceptionWith500_thenServerError() {
            assertEquals(
                CloseStatus.SERVER_ERROR.withReason("null"),
                WebSocketProxyClientHandler.getCloseStatusByError(new CloseException(502, "errorMsg"))
            );
        }

        @Test
        void whenUpgradeExceptionWith401_thenNotAcceptable() {
            assertEquals(
                CloseStatus.NOT_ACCEPTABLE.withReason("Invalid login credentials"),
                WebSocketProxyClientHandler.getCloseStatusByError(new UpgradeException(null, 401, "unauthMsg"))
            );
        }

        @Test
        void whenUpgradeExceptionWith500_thenServerError() {
            assertEquals(
                CloseStatus.SERVER_ERROR.withReason("org.eclipse.jetty.websocket.api.UpgradeException: errorMsg"),
                WebSocketProxyClientHandler.getCloseStatusByError(new UpgradeException(null, 503, "errorMsg"))
            );
        }

        @Test
        void whenUnknownException_thenServerError() {
            assertEquals(
                CloseStatus.SERVER_ERROR.withReason("java.lang.RuntimeException: errorMsg"),
                WebSocketProxyClientHandler.getCloseStatusByError(new RuntimeException("errorMsg"))
            );
        }

    }

    @Nested
    class ClosingWebSocket {

        private final WebSocketSession session = mock(WebSocketSession.class);
        private final Throwable exception = new Exception("reason");

        @Test
        void whenClosed_thenDontClose() throws Exception {
            WebSocketSession webSocketSession = mock(WebSocketSession.class);
            new WebSocketProxyClientHandler(webSocketSession).handleTransportError(session, exception);
            verify(webSocketSession, never()).close(any());
        }

        @Test
        void whenOpened_thenClose() throws Exception {
            WebSocketSession session = mock(WebSocketSession.class);
            doReturn(true).when(session).isOpen();
            new WebSocketProxyClientHandler(session).handleTransportError(session, exception);
            verify(session).close(any());

        }

    }

}
