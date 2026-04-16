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
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.yaml.YamlMessageServiceInstance;
import java.util.regex.Pattern;

/**
 * This filter intercepts Infinispan DEBUG log and transforms it into WARNING message.
 * Specifically, the filter captures the debug message produced when Infinispan
 * fails to open or access a cache segment file (index) in {@code org.infinispan.persistence.sifs.FileProvider}.
 * It enhances the logs by attaching the <b>ZWECS137W</b> message ID and enriching the message text to improve the troubleshooting.
 * Since logback is triggered before Spring, we cannot leverage the {@link org.zowe.apiml.message.log.ApimlLogger}
 * Therefore, we have to use Slf4j, but we still load the message with key 'org.zowe.apiml.cache.errorOpeningCachingFiles'.
 */
public class InfinispanLogsFilter extends TurboFilter {

    private static final String APIML_MARKER = "APIML-LOGGER";
    private static final String TARGET_LOGGER = "org.infinispan.persistence.sifs.FileProvider";
    private static final org.slf4j.Logger customLogger = LoggerFactory.getLogger(TARGET_LOGGER);
    protected static Message customMessage;
    private static final Pattern LOG_PATTERN = Pattern.compile("File \\d{1,5} was not found.*+");

    static {
        try {
            var messageService = YamlMessageServiceInstance.getInstance();
            messageService.loadMessages("/caching-log-messages.yml");
            customMessage = messageService.createMessage("org.zowe.apiml.cache.errorOpeningCachingFiles");
        } catch (Exception e) {
            // set for unit test mocking
            customMessage = null;
        }

    }

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
        if (marker != null && APIML_MARKER.equals(marker.getName())) {
            return FilterReply.NEUTRAL;
        }
        if (logger.getName().equals(TARGET_LOGGER) && format != null && LOG_PATTERN.matcher(format).matches()) {
            Marker bypassMarker = MarkerFactory.getMarker(APIML_MARKER);
            String enhancedMessage;
            try {
                String formattedOriginal = String.format(format, params);
                enhancedMessage = customMessage.mapToLogMessage() + " Exception: " + formattedOriginal;
            } catch (Exception e) {
                enhancedMessage = customMessage.mapToLogMessage() + " | Original message (unformatted): " + format;
            }
            customLogger.warn(bypassMarker, enhancedMessage, t);

            return FilterReply.DENY;
        }
        return FilterReply.NEUTRAL;

    }
}
