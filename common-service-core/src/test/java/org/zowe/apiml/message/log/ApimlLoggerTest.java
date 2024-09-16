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

import org.junit.jupiter.api.Nested;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageType;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.message.template.MessageTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ApimlLoggerTest {

    @Test
    void testEmpty() {
        ApimlLogger apimlLogger = ApimlLogger.empty();
        Logger logger = (Logger) ReflectionTestUtils.getField(apimlLogger, "logger");
        assertEquals(ApimlLogger.class.getName(), logger.getName());
        assertNull(ReflectionTestUtils.getField(apimlLogger, "messageService"));

        assertNull(apimlLogger.log("someKey"));
    }

    @Test
    void testLogLevel() {
        ApimlLogger apimlLogger = new ApimlLogger(ApimlLoggerTest.class, null);

        Logger logger = mock(Logger.class);
        ReflectionTestUtils.setField(apimlLogger, "logger", logger);

        Marker marker = (Marker) ReflectionTestUtils.getField(apimlLogger, "marker");

        apimlLogger.log(MessageType.TRACE, "traceLog", new Object[]{"param1"});
        verify(logger, times(1)).trace(marker, "traceLog", new Object[]{"param1"});

        apimlLogger.log(MessageType.DEBUG, "debugLog", new Object[]{"param2"});
        verify(logger, times(1)).debug(marker, "debugLog", new Object[]{"param2"});

        apimlLogger.log(MessageType.INFO, "infoLog", new Object[]{"param3"});
        verify(logger, times(1)).info(marker, "infoLog", new Object[]{"param3"});

        apimlLogger.log(MessageType.WARNING, "warningLog", new Object[]{"param4"});
        verify(logger, times(1)).warn(marker, "warningLog", new Object[]{"param4"});

        apimlLogger.log(MessageType.ERROR, "errorLog", new Object[]{"param5"});
        verify(logger, times(1)).error(marker, "errorLog", new Object[]{"param5"});

        verify(logger, times(1)).trace((Marker) any(), anyString(), (Object[]) any());
        verify(logger, times(1)).debug((Marker) any(), anyString(), (Object[]) any());
        verify(logger, times(1)).info((Marker) any(), anyString(), (Object[]) any());
        verify(logger, times(1)).warn((Marker) any(), anyString(), (Object[]) any());
        verify(logger, times(1)).error((Marker) any(), anyString(), (Object[]) any());
    }

    @Nested
    class GivenNullMessageService {
        ApimlLogger apimlLogger = ApimlLogger.empty();

        @Test
        void when_nullMessageService_return_nullMessage() {
            assertNull(ReflectionTestUtils.getField(apimlLogger, "messageService"));
            assertNull(apimlLogger.log("org.zowe.apiml.common.invalidMessageKey"));
        }

        @Test
        void when_nullKey_return_invalidKeyMessage() {
            assertNull(ReflectionTestUtils.getField(apimlLogger, "messageService"));

            Logger logger = mock(Logger.class);
            ReflectionTestUtils.setField(apimlLogger, "logger", logger);

            Marker marker = (Marker) ReflectionTestUtils.getField(apimlLogger, "marker");

            Message message = apimlLogger.log(null, new Object[]{});
            MessageTemplate messageTemplate = (MessageTemplate) ReflectionTestUtils.getField(message, "messageTemplate");
            String invalidKeyMessageText = "Internal error: Invalid message key '%s' provided. " +
                "No default message found. Please contact support of further assistance.";
            assertNull(ReflectionTestUtils.getField(message, "requestedKey"));
            assertEquals("org.zowe.apiml.common.invalidMessageKey", messageTemplate.getKey());
            assertEquals("ZWEAM102", messageTemplate.getNumber());
            assertEquals(MessageType.ERROR, messageTemplate.getType());
            assertEquals(invalidKeyMessageText, messageTemplate.getText());

            verify(logger, times(1)).error(marker, "ZWEAM102E Internal error: Invalid message key " +
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
