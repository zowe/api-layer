/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.websocket;

import org.apache.commons.logging.Log;
import org.apache.tomcat.websocket.AuthenticationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.socket.CloseStatus;
import reactor.core.publisher.Sinks;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApimlWebSocketSessionTest {

    @Mock
    private ApimlWebSocketSession webSocketSession;

    @Mock
    private Log logger;

    @BeforeEach
    void setUp() {
        doCallRealMethod().when(webSocketSession).onError(any());
        ReflectionTestUtils.setField(webSocketSession, "logger", logger);
    }

    @Test
    void givenAuthenticationException_WhenError_then1003() {
        webSocketSession.onError(new Throwable(new AuthenticationException("message")));
        verify(webSocketSession, times(1)).close(new CloseStatus(1003, "Invalid login credentials"));
    }

    @Test
    void givenGenericException_WhenError_thenServerError() {
        webSocketSession.onError(new RuntimeException("message"));
        verify(webSocketSession, times(1)).close(CloseStatus.SERVER_ERROR);
    }

    @Test
    void givenCompletionSinkProvided_WhenError_thenEmitError() {
        @SuppressWarnings("unchecked")
        Sinks.Empty<Void> emptyMock = mock(Sinks.Empty.class);
        Exception e = new RuntimeException("message");
        when(emptyMock.tryEmitError(e)).thenReturn(null);

        ReflectionTestUtils.setField(webSocketSession, "completionSink", emptyMock);
        webSocketSession.onError(e);
        verify(emptyMock, times(1)).tryEmitError(e);
    }

}
