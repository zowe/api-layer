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
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Marker;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class ApimlDependencyLogHider extends TurboFilter {

    private static final List<String> IGNORED_MESSAGE_KEYWORDS = Arrays.asList(
        "Tomcat initialized", "Tomcat started on port(s)",
        "lease doesn't exist", "Not Found (Renew)",
        "route 53",
        "dirty timestamp", "Using the existing instanceInfo instead of the new instanceInfo as the registrant",
        "eureka.server.peer-node-read-timeout-ms",
        "Found more than one MBeanServer instance",
        "Network level connection to peer",
        "DS: Registry: expired lease for",
        "The replication of task {} failed with response code {}",
        "Peer wants us to take the instance information from it, since the timestamp differs",

        "No routes found from RouteLocator",
        "Exception Processing ErrorPage",
        "Error while sending response to client",
        "Request execution error",
        "The Hystrix timeout",
        ".*Error during filtering.*Token is not valid.*");

    private boolean isFilterActive;

    public ApimlDependencyLogHider() {
        String profiles = System.getProperties().getProperty("spring.profiles.include");
        isFilterActive = profiles == null || !profiles.toLowerCase().contains("debug");
    }

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
        if (!isFilterActive || format == null || isLowThanInfoLevel(logger.getEffectiveLevel())) {
            return FilterReply.NEUTRAL;
        }

        if (t != null) {
            format += Stream.of(ExceptionUtils.getStackFrames(t)).collect(Collectors.joining());
        }

        return getFilterReply(format);
    }

    private boolean isLowThanInfoLevel(Level level) {
        return level.levelInt < Level.INFO.levelInt;
    }

    private FilterReply getFilterReply(String format) {
        boolean ignored =  IGNORED_MESSAGE_KEYWORDS.stream()
            .anyMatch(keyword -> {
                if (keyword.contains(".*")) {
                    return format.matches(keyword);
                } else {
                    return format.contains(keyword);
                }
            });
        return ignored ? FilterReply.DENY : FilterReply.NEUTRAL;
    }
}
