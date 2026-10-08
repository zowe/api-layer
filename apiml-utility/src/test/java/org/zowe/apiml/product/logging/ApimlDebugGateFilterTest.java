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
import ch.qos.logback.core.spi.FilterReply;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
class ApimlDebugGateFilterTest {

    private ApimlDebugGateFilter filterInstance;

    @BeforeEach
    void setUp() {
        filterInstance = new ApimlDebugGateFilter();
    }

    private static Stream<Arguments> loggerLevelArguments() {
        return Stream.of(
            // logger level, message level, result
            Arguments.of(Level.ERROR, Level.ERROR, FilterReply.NEUTRAL),
            Arguments.of(Level.ERROR, Level.WARN, FilterReply.NEUTRAL),
            Arguments.of(Level.ERROR, Level.INFO, FilterReply.NEUTRAL),
            Arguments.of(Level.ERROR, Level.DEBUG, FilterReply.NEUTRAL),
            Arguments.of(Level.ERROR, Level.TRACE, FilterReply.NEUTRAL),
            Arguments.of(Level.WARN, Level.ERROR, FilterReply.NEUTRAL),
            Arguments.of(Level.WARN, Level.WARN, FilterReply.NEUTRAL),
            Arguments.of(Level.WARN, Level.INFO, FilterReply.NEUTRAL),
            Arguments.of(Level.WARN, Level.DEBUG, FilterReply.NEUTRAL),
            Arguments.of(Level.WARN, Level.TRACE, FilterReply.NEUTRAL),
            Arguments.of(Level.INFO, Level.ERROR, FilterReply.NEUTRAL),
            Arguments.of(Level.INFO, Level.WARN, FilterReply.NEUTRAL),
            Arguments.of(Level.INFO, Level.INFO, FilterReply.NEUTRAL),
            Arguments.of(Level.INFO, Level.DEBUG, FilterReply.NEUTRAL),
            Arguments.of(Level.INFO, Level.TRACE, FilterReply.NEUTRAL),
            Arguments.of(Level.DEBUG, Level.ERROR, FilterReply.ACCEPT),
            Arguments.of(Level.DEBUG, Level.WARN, FilterReply.ACCEPT),
            Arguments.of(Level.DEBUG, Level.INFO, FilterReply.ACCEPT),
            Arguments.of(Level.DEBUG, Level.DEBUG, FilterReply.ACCEPT),
            Arguments.of(Level.DEBUG, Level.TRACE, FilterReply.NEUTRAL),
            Arguments.of(Level.TRACE, Level.ERROR, FilterReply.ACCEPT),
            Arguments.of(Level.TRACE, Level.WARN, FilterReply.ACCEPT),
            Arguments.of(Level.TRACE, Level.INFO, FilterReply.ACCEPT),
            Arguments.of(Level.TRACE, Level.DEBUG, FilterReply.ACCEPT),
            Arguments.of(Level.TRACE, Level.TRACE, FilterReply.ACCEPT)
        );
    }

    @ParameterizedTest(name = "logger level: {0}, message level: {1}, filter reply: {2}")
    @MethodSource("loggerLevelArguments")
    void whenClassLogLevel(Level classLevel, Level messageLevel, FilterReply expectedReply) {
        var logger = mock(Logger.class);
        when(logger.getEffectiveLevel()).thenReturn(classLevel);

        var reply = filterInstance.decide(null, logger, messageLevel, null, null,null);

        assertEquals(expectedReply, reply);
    }

}
