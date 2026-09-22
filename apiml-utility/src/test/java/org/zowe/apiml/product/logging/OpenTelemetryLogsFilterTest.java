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
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.spi.FilterReply;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MarkerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenTelemetryLogsFilterTest {

    private OpenTelemetryLogsFilter filter;
    private LoggerContext context;

    @BeforeEach
    void setUp() {
        filter = new OpenTelemetryLogsFilter();
        context = new LoggerContext();
        context.start();
    }

    private ch.qos.logback.classic.Logger logger(String name, Level level) {
        var logger = context.getLogger(name);
        logger.setLevel(level);
        return logger;
    }

    @Test
    void givenTelemetryLogger_whenInfoRecord_thenAccepted() {
        var logger = logger(OpenTelemetryLogsFilter.OTEL_LOGGER_NAME, Level.INFO);
        assertEquals(FilterReply.ACCEPT, filter.decide(null, logger, Level.INFO, "{}", null, null));
    }

    @Test
    void givenTelemetryLogger_whenLevelProbeWithoutMessage_thenAccepted() {
        // logback consults the chain from isInfoEnabled() passing no message at all
        var logger = logger(OpenTelemetryLogsFilter.OTEL_LOGGER_NAME, Level.INFO);
        assertEquals(FilterReply.ACCEPT, filter.decide(null, logger, Level.INFO, null, null, null));
    }

    @Test
    void givenTelemetryLogger_whenMarkerPresent_thenStillAccepted() {
        var logger = logger(OpenTelemetryLogsFilter.OTEL_LOGGER_NAME, Level.INFO);
        assertEquals(FilterReply.ACCEPT, filter.decide(MarkerFactory.getMarker("APIML-LOGGER"), logger, Level.INFO, "{}", null, null));
    }

    @Test
    void givenTelemetryLoggerTurnedOff_whenInfoRecord_thenNotAccepted() {
        var logger = logger(OpenTelemetryLogsFilter.OTEL_LOGGER_NAME, Level.OFF);
        assertEquals(FilterReply.NEUTRAL, filter.decide(null, logger, Level.INFO, "{}", null, null));
    }

    @Test
    void givenTelemetryLoggerRaisedToWarn_whenInfoRecord_thenNotAccepted() {
        var logger = logger(OpenTelemetryLogsFilter.OTEL_LOGGER_NAME, Level.WARN);
        assertEquals(FilterReply.NEUTRAL, filter.decide(null, logger, Level.INFO, "{}", null, null));
    }

    @Test
    void givenOtherApimlLogger_whenInfoRecord_thenLeftToTheOtherFilters() {
        var logger = logger("org.zowe.apiml.gateway.SomeClass", Level.INFO);
        assertEquals(FilterReply.NEUTRAL, filter.decide(null, logger, Level.INFO, "a message", null, null));
    }

    @Test
    void givenLoggerWithTelemetryPrefix_whenInfoRecord_thenLeftToTheOtherFilters() {
        var logger = logger(OpenTelemetryLogsFilter.OTEL_LOGGER_NAME + ".nested", Level.INFO);
        assertEquals(FilterReply.NEUTRAL, filter.decide(null, logger, Level.INFO, "a message", null, null));
    }

}
