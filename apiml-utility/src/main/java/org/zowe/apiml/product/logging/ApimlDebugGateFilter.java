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
import org.slf4j.Marker;

/**
 * Accept all log messages if the class logger is set to DEBUG or TRACE bypassing all other turbo-filters.
 * Necessary to enable component level logging configuration. Message log levels are respected.
 * The filter must be placed as the first filter in the chain.
 * For instance - if the class log level is set to DEBUG, and the message is ERROR, WARN, INFO or DEBUG,
 * the log message is accepted right away and does not pass through other filters.
 */
public class ApimlDebugGateFilter extends TurboFilter {

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
        if (logger.getEffectiveLevel().isGreaterOrEqual(Level.INFO)) {
            return FilterReply.NEUTRAL;
        }

        if (level.isGreaterOrEqual(logger.getEffectiveLevel())) {
            return FilterReply.ACCEPT;
        }

        return FilterReply.NEUTRAL;
    }
}
