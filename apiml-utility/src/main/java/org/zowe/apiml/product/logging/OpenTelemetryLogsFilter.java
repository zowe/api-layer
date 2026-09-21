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

public class OpenTelemetryLogsFilter extends TurboFilter {

    public static final String OTEL_LOGGER_NAME = "org.zowe.apiml.opentelemetry";

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
        if (logger != null && OTEL_LOGGER_NAME.equals(logger.getName()) && level.isGreaterOrEqual(logger.getEffectiveLevel())) {
            return FilterReply.ACCEPT;
        }
        return FilterReply.NEUTRAL;
    }

}
