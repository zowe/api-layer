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
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wires the turbo filters exactly as logback-spring.xml does for the default (non debug) profiles.
 * <p>
 * The turbo filters are global, they are consulted from inside Logger#info before any appender routing,
 * and the chain short circuits on the first ACCEPT or DENY. Testing them one by one therefore cannot
 * show whether a record survives the whole chain, which is how the telemetry records ended up being
 * dropped before reaching the OpenTelemetry appender.
 */
class ProductionTurboFilterChainTest {

    private static final String TELEMETRY_RECORD =
        "{\"http.request.method\":\"GET\",\"url.path\":\"/\",\"service.id\":\"gateway\"}";
    private static final String OTHER_LOGGER = "org.zowe.apiml.gateway.SomeClass";

    private LoggerContext context;

    @BeforeEach
    void setUp() {
        context = new LoggerContext();
        context.start();

        context.addTurboFilter(new InfinispanLogsFilter());
        context.addTurboFilter(new UseridFilter());
        context.addTurboFilter(new OpenTelemetryLogsFilter());
        context.addTurboFilter(new ApimlDependencyLogHider());
        context.addTurboFilter(new LogLevelInfoFilter());

        var duplicates = new ApimlDuplicateMessagesFilter();
        duplicates.setAllowedRepetitions(0);
        duplicates.setCacheSize(3000);
        duplicates.setContext(context);
        duplicates.start();
        context.addTurboFilter(duplicates);
    }

    private ListAppender<ILoggingEvent> attach(String loggerName) {
        var appender = new ListAppender<ILoggingEvent>();
        appender.setContext(context);
        appender.start();

        var logger = context.getLogger(loggerName);
        logger.setLevel(Level.INFO);
        logger.addAppender(appender);
        return appender;
    }

    @Test
    void givenTelemetryRecord_whenIssued_thenItReachesTheAppender() {
        var appender = attach(OpenTelemetryLogsFilter.OTEL_LOGGER_NAME);
        var logger = context.getLogger(OpenTelemetryLogsFilter.OTEL_LOGGER_NAME);

        // issue() skips the record entirely when this reports false
        assertTrue(logger.isInfoEnabled(), "telemetry logger must report INFO as enabled");

        logger.info(TELEMETRY_RECORD);

        assertEquals(1, appender.list.size(), "telemetry record was dropped by the turbo filter chain");
        assertEquals(TELEMETRY_RECORD, appender.list.get(0).getFormattedMessage());
    }

    @Test
    void givenIdenticalTelemetryRecords_whenIssued_thenNoneIsDeduplicated() {
        var appender = attach(OpenTelemetryLogsFilter.OTEL_LOGGER_NAME);
        var logger = context.getLogger(OpenTelemetryLogsFilter.OTEL_LOGGER_NAME);

        for (int i = 0; i < 5; i++) {
            logger.info(TELEMETRY_RECORD);
        }

        assertEquals(5, appender.list.size(), "every request is a distinct event and must not be deduplicated");
    }

    @Test
    void givenTelemetryLoggerTurnedOff_whenIssued_thenNothingIsLogged() {
        var appender = attach(OpenTelemetryLogsFilter.OTEL_LOGGER_NAME);
        var logger = context.getLogger(OpenTelemetryLogsFilter.OTEL_LOGGER_NAME);
        logger.setLevel(Level.OFF);

        assertFalse(logger.isInfoEnabled(), "turning the telemetry logger off must remain effective");
        logger.info(TELEMETRY_RECORD);

        assertEquals(0, appender.list.size());
    }

    @Test
    void givenUnmarkedApimlInfoMessage_whenLogged_thenStillSuppressed() {
        var appender = attach(OTHER_LOGGER);

        context.getLogger(OTHER_LOGGER).info("a chatty message meant for debug mode only");

        assertEquals(0, appender.list.size(), "LogLevelInfoFilter must keep suppressing unmarked INFO messages");
    }

    @Test
    void givenIdenticalUnmarkedApimlWarnings_whenLogged_thenStillDeduplicated() {
        var appender = attach(OTHER_LOGGER);
        var logger = context.getLogger(OTHER_LOGGER);

        logger.warn("a repeated warning");
        logger.warn("a repeated warning");

        assertEquals(1, appender.list.size(), "deduplication must stay in place for the human readable logs");
    }

}
