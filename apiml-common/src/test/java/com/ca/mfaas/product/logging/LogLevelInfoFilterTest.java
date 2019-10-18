/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package com.ca.mfaas.product.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.core.spi.FilterReply;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@Slf4j
public class LogLevelInfoFilterTest {

    private LogLevelInfoFilter filterInstance;
    private static final String APIML_MARKER = "APIML-LOGGER";

    @Before
    public void setUp() {
        System.getProperties().setProperty("spring.profiles.include", "");
        filterInstance = new LogLevelInfoFilter();
    }

    @Test
    public void createActiveFilterInNormalMode() {
        FilterReply reply = filterInstance.decide(null, (ch.qos.logback.classic.Logger) log, Level.INFO, "", null,
                null);
        assertEquals("Filter does not filter INFO level messages", FilterReply.DENY, reply);
    }

    @Test
    public void createInactiveFilterInDebugMode() {
        System.getProperties().setProperty("spring.profiles.include", "blueberries,debug,diag");
        LogLevelInfoFilter filter = new LogLevelInfoFilter();
        FilterReply reply = filter.decide(null, (ch.qos.logback.classic.Logger) log, Level.INFO, "", null, null);
        assertEquals("Filter should not filter when service not in debug mode", FilterReply.NEUTRAL, reply);
    }

    @Test
    public void filtersLevelTest() {

        Map<Level, Boolean> filteringMap = new HashMap<>();
        filteringMap.put(Level.TRACE, true);
        filteringMap.put(Level.DEBUG, true);
        filteringMap.put(Level.INFO, true);
        filteringMap.put(Level.WARN, false);
        filteringMap.put(Level.ERROR, false);

        filteringMap.forEach((level, shouldFilter) -> {
            FilterReply reply = filterInstance.decide(null, (ch.qos.logback.classic.Logger) log, level, "", null, null);
            assertEquals("Logging level " + level.toString() + " not filtered correctly",
                    shouldFilter ? FilterReply.DENY : FilterReply.NEUTRAL, reply);
        });
    }

    @Test
    public void filtersLevelTestWithMarker() {
        Marker marker = MarkerFactory.getMarker(APIML_MARKER);
        Map<Level, Boolean> filteringMap = new HashMap<>();
        filteringMap.put(Level.TRACE, false);
        filteringMap.put(Level.DEBUG, false);
        filteringMap.put(Level.INFO, false);
        filteringMap.put(Level.WARN, false);
        filteringMap.put(Level.ERROR, false);

        filteringMap.forEach((level, shouldFilter) -> {
            FilterReply reply = filterInstance.decide(marker, (ch.qos.logback.classic.Logger) log, level, "", null,
                    null);
            assertEquals("Logging level with apiml marker " + level.toString() + " not filtered correctly",
                    shouldFilter ? FilterReply.DENY : FilterReply.NEUTRAL, reply);
        });
    }

    @Test
    public void packageFilterTest() {
        Map<String, Boolean> loggerMap = new HashMap<>();
        loggerMap.put("com.ca.mfaas.product.logging.LogLevelFilterTest", true);
        loggerMap.put("com.ca.apiml.security.common.config.AuthConfigurationProperties", true);
        loggerMap.put("com.amazonaws", false);
        loggerMap.put("bucharest.com.ca.mfaas", false);
        loggerMap.put("COM.CA.MFAAS.CRAZYPEOPLE", false);

        loggerMap.forEach((loggerName, shouldFilter) -> {
            org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(loggerName);
            FilterReply reply = filterInstance.decide(null, (ch.qos.logback.classic.Logger) logger, Level.DEBUG, "",
                    null, null);
            assertEquals("Logger of name " + logger.getName() + " is not filtered correctly",
                    shouldFilter ? FilterReply.DENY : FilterReply.NEUTRAL, reply);
        });

    }

}
