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
import org.springframework.web.socket.WebSocketSession;

import java.net.InetSocketAddress;
import java.net.URI;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        when(serverSession.getRemoteAddress()).thenReturn(new InetSocketAddress("gateway",  8080));
        when(serverSession.getUri()).thenReturn(new URI(serverUriPath));

        assertThat(underTest.getClientId(), is(sessionId));
        assertThat(underTest.getClientUri(), is(clientUriPath));

        assertThat(underTest.getServerRemoteAddress(), is("gateway:8080"));
        assertThat(underTest.getServerUri(), is(serverUriPath));
    }
}
