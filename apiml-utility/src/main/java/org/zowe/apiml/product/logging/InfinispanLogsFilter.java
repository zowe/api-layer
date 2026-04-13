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
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * This filter's purpose is to hide or show the info level messages (and below log levels like debug etc.)
 * There are info level messages that are meant for debug mode only (originally @Slf4j)
 * There are info level messages that are meant to be displayed (like service startup messages)
 * <p>
 * Because ApimlLogger is using Slf4j in the background, there is conflict of interest.
 * Solution is that ApimlLogger is enhancing its logs with Marker instances and this filter is providing
 * adequate filtering.
 */
public class InfinispanLogsFilter extends TurboFilter {
    static {
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.println("!!! INFINISPAN FILTER LOADED BY LOGBACK !!!");
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
    }

    private static final String APIML_MARKER = "APIML-LOGGER";
    private static final String TARGET_LOGGER = "org.infinispan.persistence.sifs.FileProvider";
    private static final org.slf4j.Logger customLogger = LoggerFactory.getLogger(TARGET_LOGGER);
    private static final String CUSTOM_INFO = "[ZOWE CACHE INFO]: Accessing a segment file that is no longer on disk.";

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
        if (marker != null && APIML_MARKER.equals(marker.getName())) {
            return FilterReply.NEUTRAL;
        }
        if (logger.getName().equals(TARGET_LOGGER) && format != null && format.contains("File") && format.contains("not found")) {
            Marker bypassMarker = MarkerFactory.getMarker(APIML_MARKER);
            if (level.equals(Level.DEBUG)) {
                String enhancedMessage;
                try {
                    String formattedOriginal = String.format(format, params);
                    enhancedMessage = CUSTOM_INFO + " | Original: " + formattedOriginal;
                } catch (Exception e) {
                    enhancedMessage = CUSTOM_INFO + " | Original message (unformatted): " + format;
                }

                customLogger.debug(bypassMarker, enhancedMessage, t);
            } else if (level.isGreaterOrEqual(Level.INFO)) {
                customLogger.info(bypassMarker, CUSTOM_INFO);
            }
            return FilterReply.DENY;
        }
        return FilterReply.NEUTRAL;

    }
}
