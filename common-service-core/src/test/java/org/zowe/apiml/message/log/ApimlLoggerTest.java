/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.message.log;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageType;
import org.zowe.apiml.message.template.MessageTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApimlLoggerTest {

    private static final Marker MARKER = (Marker) ReflectionTestUtils.getField(ApimlLogger.class, "marker");

    @Mock
    private Logger logger;

    private ApimlLogger apimlLogger;

    @BeforeEach
    void setUp() {
        apimlLogger = new ApimlLogger(ApimlLoggerTest.class, null);
        ReflectionTestUtils.setField(apimlLogger, "logger", logger);
    }

    @Test
    void testEmpty() {
        ApimlLogger emptyLogger = ApimlLogger.empty();
        Logger defaultLogger = (Logger) ReflectionTestUtils.getField(emptyLogger, "logger");
        assertEquals(ApimlLogger.class.getName(), defaultLogger.getName());
        assertNull(ReflectionTestUtils.getField(emptyLogger, "messageService"));

        assertNull(emptyLogger.log("someKey"));
    }

    @Test
    void testLogLevel() {
        apimlLogger.log(MessageType.TRACE, "traceLog {}", new Object[]{"param1"});
        verify(logger, times(1)).trace(MARKER, "traceLog {}", new Object[]{"param1"});

        apimlLogger.log(MessageType.DEBUG, "debugLog {}", new Object[]{"param2"});
        verify(logger, times(1)).debug(MARKER, "debugLog {}", new Object[]{"param2"});

        apimlLogger.log(MessageType.INFO, "infoLog {}", new Object[]{"param3"});
        verify(logger, times(1)).info(MARKER, "infoLog {}", new Object[]{"param3"});

        apimlLogger.log(MessageType.WARNING, "warningLog {}", new Object[]{"param4"});
        verify(logger, times(1)).warn(MARKER, "warningLog {}", new Object[]{"param4"});

        apimlLogger.log(MessageType.ERROR, "errorLog {}", new Object[]{"param5"});
        verify(logger, times(1)).error(MARKER, "errorLog {}", new Object[]{"param5"});

        verify(logger, times(1)).trace((Marker) any(), anyString(), (Object[]) any());
        verify(logger, times(1)).debug((Marker) any(), anyString(), (Object[]) any());
        verify(logger, times(1)).info((Marker) any(), anyString(), (Object[]) any());
        verify(logger, times(1)).warn((Marker) any(), anyString(), (Object[]) any());
        verify(logger, times(1)).error((Marker) any(), anyString(), (Object[]) any());
    }

    @Test
    void whenArgumentsAreInvalidAndDebugIsEnabledForTheMarker_thenStackTraceIsLogged() {
        when(logger.isDebugEnabled(MARKER)).thenReturn(true);

        apimlLogger.log((MessageType) null, "text");

        verify(logger, times(1)).debug(eq(MARKER), anyString(), any(), any());
        verify(logger, never()).warn(eq(MARKER), anyString(), any(), any());
        verify(logger, never()).isDebugEnabled();
    }

    @Test
    void whenArgumentsAreInvalidAndDebugIsDisabledForTheMarker_thenHintIsLogged() {
        apimlLogger.log((MessageType) null, "text");

        verify(logger, times(1)).warn(eq(MARKER), anyString(), any(), any());
        verify(logger, never()).debug(eq(MARKER), anyString(), any(), any());
    }

    @Nested
    class GivenNullMessageService {

        @Test
        void when_nullMessageService_return_nullMessage() {
            assertNull(ReflectionTestUtils.getField(apimlLogger, "messageService"));
            assertNull(apimlLogger.log("org.zowe.apiml.common.invalidMessageKey"));
        }

        @Test
        void when_nullKey_return_invalidKeyMessage() {
            assertNull(ReflectionTestUtils.getField(apimlLogger, "messageService"));

            Message message = apimlLogger.log(null, new Object[]{});
            MessageTemplate messageTemplate = (MessageTemplate) ReflectionTestUtils.getField(message, "messageTemplate");
            String invalidKeyMessageText = "Internal error: Invalid message key '%s' provided. " +
                "No default message found. Please contact support of further assistance.";
            assertNull(ReflectionTestUtils.getField(message, "requestedKey"));
            assertEquals("org.zowe.apiml.common.invalidMessageKey", messageTemplate.getKey());
            assertEquals("ZWEAM102", messageTemplate.getNumber());
            assertEquals(MessageType.ERROR, messageTemplate.getType());
            assertEquals(invalidKeyMessageText, messageTemplate.getText());

            verify(logger, times(1)).error(MARKER, "ZWEAM102E Internal error: Invalid message key " +
                "'null' provided. No default message found. Please contact support of further assistance.", new Object[0]);
        }

        @Test
        void when_nullMessageArgument_doNotThrowRuntimeException() {
            assertDoesNotThrow(() -> apimlLogger.log(null));
        }

        @Test
        void when_nullArguments_doNotThrowRuntimeException() {
            assertDoesNotThrow(() -> apimlLogger.log((MessageType) null, null));
        }
    }

}
