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
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Marker;

import java.util.Arrays;
import java.util.List;

@Slf4j
public class ApimlDependencyLogHider extends TurboFilter {

    private static final List<String> IGNORED_MESSAGE_KEYWORDS = Arrays.asList(
        "Tomcat initialized",
        "lease doesn't exist", "Not Found (Renew)",
        "route 53",
        "eureka.server.peer-node-read-timeout-ms",
        "Found more than one MBeanServer instance",
        "dirty timestamp", "Using the existing instanceInfo instead of the new instanceInfo as the registrant",
        "Network level connection to peer",
        "Tomcat started on port(s)");

    private boolean isFilterActive;

    public ApimlDependencyLogHider() {
        String profiles = System.getProperties().getProperty("spring.profiles.include");
        isFilterActive = profiles == null || !profiles.toLowerCase().contains("debug");
    }

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {

        if (isFilterActive
            && !isLowThanInfoLevel(logger.getEffectiveLevel())
            && (format != null && isIgnoredMessage(format))) {
            return FilterReply.DENY;
        }

        return FilterReply.NEUTRAL;
    }

    private boolean isLowThanInfoLevel(Level level) {
        return level.levelInt < Level.INFO.levelInt;
    }

    private boolean isIgnoredMessage(String format) {
        return IGNORED_MESSAGE_KEYWORDS.stream()
            .anyMatch(format::contains);
    }
}
