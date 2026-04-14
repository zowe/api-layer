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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InfinispanLogsFilterTest {

    private InfinispanLogsFilter filterInstance;
    private static final String APIML_MARKER = "APIML-LOGGER";
    private static final String TARGET_LOGGER_PATH = "org.infinispan.persistence.sifs.FileProvider";
    private static final String OTHER_LOGGER_PATH = "org.zowe.apiml.SomeClass";
    private static final String TARGET_FORMAT = "File %d was not found";

    private final Logger targetLogger = (Logger) LoggerFactory.getLogger(TARGET_LOGGER_PATH);
    private final Logger otherLogger = (Logger) LoggerFactory.getLogger(OTHER_LOGGER_PATH);

    @BeforeEach
    void setUp() {
        filterInstance = new InfinispanLogsFilter();
    }

    @Test
    void testNeutralForApimlMarker() {
        Marker marker = MarkerFactory.getMarker(APIML_MARKER);

        FilterReply reply = filterInstance.decide(marker, targetLogger, Level.DEBUG, TARGET_FORMAT, new Object[]{1}, null);
        assertEquals(FilterReply.NEUTRAL, reply, "Filter should be NEUTRAL when APIML marker is present");
    }

    @Test
    void testDenyForTargetInfinispanLog() {
        FilterReply reply = filterInstance.decide(null, targetLogger, Level.DEBUG, TARGET_FORMAT, new Object[]{101}, null);
        assertEquals(FilterReply.DENY, reply, "Filter should DENY the original Infinispan log after re-logging it");
    }

    @Test
    void testNeutralForOtherLogger() {
        FilterReply reply = filterInstance.decide(null, otherLogger, Level.DEBUG, TARGET_FORMAT, new Object[]{101}, null);
        assertEquals(FilterReply.NEUTRAL, reply, "Filter should be NEUTRAL for loggers other than " + TARGET_LOGGER_PATH);
    }

    @Test
    void testNeutralForDifferentFormat() {
        String differentFormat = "Index segment file %d opened successfully";
        FilterReply reply = filterInstance.decide(null, targetLogger, Level.DEBUG, differentFormat, new Object[]{101}, null);
        assertEquals(FilterReply.NEUTRAL, reply, "Filter should be NEUTRAL for different log messages");
    }

    @Test
    void testInfoLevelHandling() {
        FilterReply reply = filterInstance.decide(null, targetLogger, Level.INFO, TARGET_FORMAT, new Object[]{101}, null);
        assertEquals(FilterReply.DENY, reply, "Filter should DENY even at INFO level for the target message");
    }

    @Test
    void testFormatErrorSafety() {
        FilterReply reply = filterInstance.decide(null, targetLogger, Level.DEBUG, TARGET_FORMAT, null, null);
        assertEquals(FilterReply.DENY, reply, "Filter should handle formatting exceptions safely and still return DENY");
    }
}
