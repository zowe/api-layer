/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.event;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.SimpleApplicationEventMulticaster;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventErrorHandlerTest {

    @Mock
    private SimpleApplicationEventMulticaster multicaster;

    @Mock
    private Appender<ILoggingEvent> mockedAppender;

    private Logger logger;
    private EventErrorHandler handler;

    @Captor
    private ArgumentCaptor<LoggingEvent> loggingCaptor;

    @BeforeEach
    void init() {
        handler = new EventErrorHandler(multicaster);
        logger = (Logger) LoggerFactory.getLogger(EventErrorHandler.class);
        logger.detachAndStopAllAppenders();
        logger.getLoggerContext().resetTurboFilterList();
        logger.addAppender(mockedAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(mockedAppender);
    }

    @Test
    void givenSimpleApplicationEventMulticaster_whenCreated_thenRegister() {
        verify(multicaster).setErrorHandler(handler);
    }

    @Test
    void givenHandler_whenRuntimeException_thenLogAndThrowTheException() {
        var exception = new IllegalArgumentException("A runtime test exception");
        var exception2 = assertThrows(IllegalArgumentException.class, () -> handler.handleError(exception));
        assertSame(exception, exception2);
        verify(mockedAppender, atLeast(1)).doAppend(loggingCaptor.capture());
        assertTrue(loggingCaptor.getValue().getFormattedMessage().contains(exception.getMessage()));
    }

    @Test
    void givenHandler_whenRuntimeException_thenLogAndThrowRuntimeException() {
        var exception = new IOException("A checked test exception");
        var exception2 = assertThrows(RuntimeException.class, () -> handler.handleError(exception));
        assertSame(exception, exception2.getCause());
        verify(mockedAppender, atLeast(1)).doAppend(loggingCaptor.capture());
        assertTrue(loggingCaptor.getValue().getFormattedMessage().contains(exception.getMessage()));
    }

}
