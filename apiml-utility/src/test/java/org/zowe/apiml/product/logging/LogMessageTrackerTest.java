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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LogMessageTrackerTest {
    private static final String LOG_MESSAGE = "This is a log message.";
    private static final Pattern MESSAGE_REGEX = Pattern.compile("^This.*");

    private final LogMessageTracker logMessageTracker = new LogMessageTracker(this.getClass());
    private final Logger log = (Logger) LoggerFactory.getLogger(this.getClass());

    @BeforeEach
    public void setup() {
        logMessageTracker.startTracking();
        log.trace(LOG_MESSAGE);
        log.debug(LOG_MESSAGE);
        log.info(LOG_MESSAGE);
        log.warn(LOG_MESSAGE);
        log.error(LOG_MESSAGE);
    }

    @AfterEach
    void cleanUp() {
        logMessageTracker.stopTracking();
    }

    @Test
    void testSearch() {
        assertEquals(5, logMessageTracker.search(LOG_MESSAGE).size());
        assertEquals(1, logMessageTracker.search(LOG_MESSAGE, Level.INFO).size());
        assertEquals(5, logMessageTracker.search(MESSAGE_REGEX).size());
        assertEquals(1, logMessageTracker.search(MESSAGE_REGEX, Level.INFO).size());
    }

    @Test
    void testContains() {
        assertTrue(logMessageTracker.contains(LOG_MESSAGE));
        assertTrue(logMessageTracker.contains(LOG_MESSAGE, Level.TRACE));
        assertTrue(logMessageTracker.contains(MESSAGE_REGEX));
        assertTrue(logMessageTracker.contains(MESSAGE_REGEX, Level.TRACE));
    }

    @Test
    void testAllLogEventsTracked() {
        assertEquals(5, logMessageTracker.countEvents());
        List<ILoggingEvent> logEvents = logMessageTracker.getAllLoggedEvents();
        logEvents.forEach(event -> assertEquals(event.getMessage(), LOG_MESSAGE));
    }

    @Test
    void testCleanup() {
        // Test LogMessageTracker doesn't keep logs after running LogMessageTracker.clear in @AfterEach method
        assertEquals(5, logMessageTracker.countEvents());
    }
}
