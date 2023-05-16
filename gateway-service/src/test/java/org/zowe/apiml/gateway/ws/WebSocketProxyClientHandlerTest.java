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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.websocket.api.CloseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

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

}
