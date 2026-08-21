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
import ch.qos.logback.classic.spi.EventArgUtil;
import ch.qos.logback.classic.turbo.DuplicateMessageFilter;
import ch.qos.logback.core.spi.FilterReply;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Marker;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.util.Objects;

/**
 * This is our custom {@link DuplicateMessageFilter} which prevents excessive duplicate log messages
 * from cluttering logs.
 * In contrast to DuplicateMessageFilter which compares the log message format (before parameter substitution),
 * we want to filter out identical log messages.
 * Similar log messages (but not identical) should pass this filter.
 */
public class ApimlDuplicateMessagesFilter extends DuplicateMessageFilter {

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
        if (isLevelProbe(format, params, t)) {
            return FilterReply.NEUTRAL;
        }
        if (!level.isGreaterOrEqual(logger.getEffectiveLevel())) {
            // logback drops the event on its own, so there is no need to format and hash a message
            // nobody will read; abstaining rather than denying keeps the rest of the chain in play
            return FilterReply.NEUTRAL;
        }
        String formattedMessage = getLogMessage(format, params, t);
        // Sent the entire formatted message to the parent to ensure exact duplicates are filtered out
        return super.decide(marker, logger, level, getMessageHash(formattedMessage), params, t);
    }

    /**
     * Logback consults the turbo filter chain from {@code Logger#isDebugEnabled()} and its siblings,
     * passing no message at all - format, params and throwable are all null. Such a probe carries
     * nothing to de-duplicate and must not consume a cache entry: with allowedRepetitions set to 0 the
     * first probe would exhaust the only allowed repetition and every later isXxxEnabled() call in the
     * whole application would be answered with false, silencing all logging guarded that way
     * (reactor-netty and Netty's LoggingHandler guard every statement like this).
     */
    private boolean isLevelProbe(String format, Object[] params, Throwable t) {
        return format == null && params == null && t == null;
    }

    private String getMessageHash(String message) {
        return String.valueOf(Objects.hashCode(message));
    }

    private String getLogMessage(String format, Object[] params, Throwable t) {
        FormattingTuple formattingTuple = getFormattedMessage(format, params, t);
        return new StringBuilder()
                .append(formattingTuple.getMessage())
                .append(formattingTuple.getThrowable() == null ? "" : "\n")
                .append(String.join("\n", ExceptionUtils.getStackFrames(formattingTuple.getThrowable())))
                .toString();
    }

    private FormattingTuple getFormattedMessage(String format, Object[] params, Throwable t) {
        if (params == null || params.length == 0) {
            return new FormattingTuple(format, params, t);
        }

        if (t == null) {
            Throwable throwable = EventArgUtil.extractThrowable(params);
            if (throwable == null) {
                return MessageFormatter.arrayFormat(format, params);
            }
            return MessageFormatter.arrayFormat(format, EventArgUtil.trimmedCopy(params), throwable);
        }

        return MessageFormatter.arrayFormat(format, params, t);
    }
}
