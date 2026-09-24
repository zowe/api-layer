/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.client.ws;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class HeaderSocketServerHandlerTest {
    private final HeaderSocketServerHandler handler = new HeaderSocketServerHandler();
    private WebSocketSession session;

    @BeforeEach
    void setUp() {
        session = mock(WebSocketSession.class);
        HttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("header", "TEXT");
        when(session.getHandshakeHeaders()).thenReturn(headers);
    }

    @Nested
    class GivenMessageInHeader {
        @Test
        void whenSendingMessage_thenSendIt() throws Exception {
            handler.handleMessage(session, new TextMessage("text"));
            ArgumentCaptor<WebSocketMessage<?>> messageCaptor = ArgumentCaptor.forClass(WebSocketMessage.class);
            verify(session).sendMessage(messageCaptor.capture());

            assertEquals("[header:\"TEXT\"]", messageCaptor.getValue().getPayload().toString());
        }

        @Test
        void whenSendingByeMessage_thenSendAndClose() throws Exception {
            handler.handleMessage(session, new TextMessage("bye"));
            ArgumentCaptor<WebSocketMessage<?>> messageCaptor = ArgumentCaptor.forClass(WebSocketMessage.class);
            verify(session).sendMessage(messageCaptor.capture());

            assertEquals("[header:\"TEXT\"]", messageCaptor.getValue().getPayload().toString());
            verify(session, times(1)).close();
        }
    }

    @Nested
    class GivenHandshakeHeaders {

        /**
         * Tomcat 11 hands back the header names exactly as the client spelled them, while the endpoint's
         * output is matched case-insensitively by the integration tests, so the handler has to normalise
         * them itself.
         */
        @Test
        void whenHeaderNameIsMixedCase_thenReportItLowerCase() throws Exception {
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("X-Test", "value");
            when(session.getHandshakeHeaders()).thenReturn(headers);

            handler.handleMessage(session, new TextMessage("gimme those headers"));

            ArgumentCaptor<WebSocketMessage<?>> messageCaptor = ArgumentCaptor.forClass(WebSocketMessage.class);
            verify(session).sendMessage(messageCaptor.capture());
            assertEquals("[x-test:\"value\"]", messageCaptor.getValue().getPayload().toString());
        }

        @Test
        void whenHeaderHasMultipleValues_thenReportAllOfThem() throws Exception {
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("X-Test", "one");
            headers.add("X-Test", "two");
            when(session.getHandshakeHeaders()).thenReturn(headers);

            handler.handleMessage(session, new TextMessage("gimme those headers"));

            ArgumentCaptor<WebSocketMessage<?>> messageCaptor = ArgumentCaptor.forClass(WebSocketMessage.class);
            verify(session).sendMessage(messageCaptor.capture());
            assertEquals("[x-test:\"one,two\"]", messageCaptor.getValue().getPayload().toString());
        }

        /**
         * The Turkish locale lower-cases "I" to a dotless "ı", which would make the reported name
         * unrecognisable on a host running with that locale.
         */
        @Test
        void whenHostLocaleIsTurkish_thenStillReportTheAsciiName() {
            Locale original = Locale.getDefault();
            try {
                Locale.setDefault(Locale.forLanguageTag("tr-TR"));

                WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
                headers.add("X-Id", "value");

                assertEquals("[x-id:\"value\"]", HeaderSocketServerHandler.describe(headers));
            } finally {
                Locale.setDefault(original);
            }
        }

    }

}
