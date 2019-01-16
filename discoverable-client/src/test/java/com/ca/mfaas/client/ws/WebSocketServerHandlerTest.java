/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.client.ws;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class WebSocketServerHandlerTest {

    @Test
    public void handleMessageTest() throws Exception {
        WebSocketServerHandler handler = new WebSocketServerHandler();
        WebSocketSession session = mock(WebSocketSession.class);

        handler.handleMessage(session, new TextMessage("text"));

        ArgumentCaptor<WebSocketMessage<?>> messageCaptor = ArgumentCaptor.forClass(WebSocketMessage.class);
        verify(session).sendMessage(messageCaptor.capture());

        assertEquals("TEXT", messageCaptor.getValue().getPayload().toString());
    }

    @Test
    public void handleByeMessage() throws Exception {
        WebSocketServerHandler handler = new WebSocketServerHandler();
        WebSocketSession session = mock(WebSocketSession.class);

        handler.handleMessage(session, new TextMessage("BYE"));

        ArgumentCaptor<WebSocketMessage<?>> messageCaptor = ArgumentCaptor.forClass(WebSocketMessage.class);
        verify(session).sendMessage(messageCaptor.capture());

        assertEquals("BYE", messageCaptor.getValue().getPayload().toString());
        verify(session, times(1)).close();
    }
}
