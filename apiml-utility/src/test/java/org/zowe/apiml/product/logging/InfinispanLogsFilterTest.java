/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.core.spi.FilterReply;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.zowe.apiml.message.core.Message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InfinispanLogsFilterTest {

    private InfinispanLogsFilter filterInstance;
    private static final String APIML_MARKER = "APIML-LOGGER";
    private static final String TARGET_LOGGER_PATH = "org.infinispan.persistence.sifs.FileProvider";
    private static final String OTHER_LOGGER_PATH = "org.zowe.apiml.SomeClass";
    private static final String TARGET_FORMAT = "File 101 was not found";

    private final Logger targetLogger = (Logger) LoggerFactory.getLogger(TARGET_LOGGER_PATH);
    private final Logger otherLogger = (Logger) LoggerFactory.getLogger(OTHER_LOGGER_PATH);

    @BeforeEach
    void setUp() {
        filterInstance = new InfinispanLogsFilter();

        Message mockMessage = mock(Message.class);
        when(mockMessage.mapToLogMessage()).thenReturn("ZWECS137W: Mocked Message");
    }

    @Test
    void testNeutralForApimlMarker() {
        Marker marker = MarkerFactory.getMarker(APIML_MARKER);

        FilterReply reply = filterInstance.decide(marker, targetLogger, Level.DEBUG, TARGET_FORMAT, new Object[]{1}, null);
        assertEquals(FilterReply.NEUTRAL, reply, "Filter should be NEUTRAL when APIML marker is present");
    }

    @Test
    void testDenyForTargetInfinispanLogWhenDebug() {
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        targetLogger.addAppender(listAppender);

        try {
            FilterReply reply = filterInstance.decide(null, targetLogger, Level.DEBUG, TARGET_FORMAT, new Object[]{101}, null);
            assertEquals(FilterReply.DENY, reply, "Filter should DENY the original Infinispan log after re-logging it");
            assertEquals(1, listAppender.list.size(), "One enriched log should have been issued");
            ILoggingEvent event = listAppender.list.get(0);
            assertEquals(Level.WARN, event.getLevel(), "The enriched log should be a WARNING");
            String logMessage = event.getFormattedMessage();
            assertTrue(logMessage.contains("ZWECS137W"), "Message should contain message ID");
            assertTrue(logMessage.contains("File 101 was not found"), "Message should contain original details");
        } finally {
            targetLogger.detachAppender(listAppender);
        }
    }

    @Test
    void testDenyForTargetInfinispanLogWhenRootInfo() {
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        targetLogger.addAppender(listAppender);

        try {
            FilterReply reply = filterInstance.decide(null, targetLogger, Level.INFO, TARGET_FORMAT, new Object[]{101}, null);
            assertEquals(FilterReply.DENY, reply, "Filter should DENY the original Infinispan log after re-logging it");
            assertEquals(1, listAppender.list.size(), "One enriched log should have been issued");
            ILoggingEvent event = listAppender.list.get(0);
            assertEquals(Level.WARN, event.getLevel(), "The enriched log should be a WARNING");
            String logMessage = event.getFormattedMessage();
            assertTrue(logMessage.contains("ZWECS137W"), "Message should contain message ID");
            assertTrue(logMessage.contains("File 101 was not found"), "Message should contain original details");
        } finally {
            targetLogger.detachAppender(listAppender);
        }
    }

    @Test
    void testNeutralForOtherLogger() {
        FilterReply reply = filterInstance.decide(null, otherLogger, Level.DEBUG, TARGET_FORMAT, new Object[]{101}, null);
        assertEquals(FilterReply.NEUTRAL, reply, "Filter should be NEUTRAL for loggers other than " + TARGET_LOGGER_PATH);
    }

    @Test
    void testDenyForGenericLogWhenDebugDisabled() {
        Logger infinispanLogger = (Logger) LoggerFactory.getLogger("org.infinispan");
        Level originalLevel = infinispanLogger.getLevel();

        try {
            infinispanLogger.setLevel(Level.INFO);
            String genericFormat = "Opening channel for file 123";
            FilterReply reply = filterInstance.decide(null, targetLogger, Level.INFO, genericFormat, null, null);

            assertEquals(FilterReply.DENY, reply,
                "Generic DEBUG logs should be NEUTRAL when root debug is disabled");

        } finally {
            infinispanLogger.setLevel(originalLevel);
        }
    }

    @Test
    void testNeutralForSpecificLogWhenDebugDisabled() {
        Logger infinispanLogger = (Logger) LoggerFactory.getLogger("org.infinispan");
        Level originalLevel = infinispanLogger.getLevel();

        try {
            infinispanLogger.setLevel(Level.INFO);
            FilterReply reply = filterInstance.decide(null, targetLogger, Level.INFO, TARGET_FORMAT, null, null);

            assertEquals(FilterReply.DENY, reply,
                "Specific DEBUG logs should be DENY when root debug is disabled");

        } finally {
            infinispanLogger.setLevel(originalLevel);
        }
    }

    @Test
    void testNeutralForGenericLogWhenDebugEnabled() {
        Logger infinispanLogger = (Logger) LoggerFactory.getLogger("org.infinispan");
        Level originalLevel = infinispanLogger.getLevel();

        try {
            infinispanLogger.setLevel(Level.DEBUG);
            String genericFormat = "Opening channel for file 123";
            FilterReply reply = filterInstance.decide(null, targetLogger, Level.DEBUG, genericFormat, null, null);

            assertEquals(FilterReply.NEUTRAL, reply,
                "Generic DEBUG logs should be NEUTRAL when root debug is enabled");

        } finally {
            infinispanLogger.setLevel(originalLevel);
        }
    }

    @Test
    void testDenyForSpecificLogWhenDebugEnabled() {
        Logger infinispanLogger = (Logger) LoggerFactory.getLogger("org.infinispan");
        Level originalLevel = infinispanLogger.getLevel();

        try {
            infinispanLogger.setLevel(Level.DEBUG);
            FilterReply reply = filterInstance.decide(null, targetLogger, Level.DEBUG, TARGET_FORMAT, null, null);

            assertEquals(FilterReply.DENY, reply,
                "Specific DEBUG logs should be DENY when root debug is enabled");

        } finally {
            infinispanLogger.setLevel(originalLevel);
        }
    }
}
