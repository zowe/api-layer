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

import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;

import javax.annotation.PostConstruct;
import java.time.ZoneId;
import java.util.TimeZone;

/**
 * Configuration class for logging timezone settings
 */
@Component
public class LoggingTimezoneConfig {

    private static final String UT_COMMON_TIMEZONE_SET_LOG_KEY = "org.zowe.apiml.common.timezone.set";
    private static final String UT_COMMON_TIMEZONE_INVALID_LOG_KEY = "org.zowe.apiml.common.timezone.invalid";
    private static final String UTC_TIMEZONE = "UTC";
    private static final String LOGGING_TIMEZONE = "LOGGING_TIMEZONE";
    private static final String LOCAL = "LOCAL";
    private static final String TZ = "TZ";

    @InjectApimlLogger
    private ApimlLogger apimlLog = ApimlLogger.empty();

    @Value("${logging.timezone:UTC}")
    private String configuredTimezone;

    @PostConstruct
    public void init() {
        String timezone = determineTimezone();
        TimeZone.setDefault(TimeZone.getTimeZone(timezone));

        // Update Logback context timezone
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.putProperty(LOGGING_TIMEZONE, timezone);

        apimlLog.log(UT_COMMON_TIMEZONE_SET_LOG_KEY, timezone);
    }

    private String determineTimezone() {
        if (LOCAL.equalsIgnoreCase(configuredTimezone)) {
            // Check for TZ environment variable first
            String tzEnv = System.getenv(TZ);
            if (tzEnv != null && !tzEnv.isEmpty()) {
                try {
                    // Validate if the TZ value is a valid timezone
                    ZoneId.of(tzEnv);
                    return tzEnv;
                } catch (Exception e) {
                    apimlLog.log(UT_COMMON_TIMEZONE_INVALID_LOG_KEY, tzEnv);
                    return TimeZone.getDefault().getID();
                }
            }
            return TimeZone.getDefault().getID();
        }

        try {
            // Validate configured timezone
            ZoneId.of(configuredTimezone);
            return configuredTimezone;
        } catch (Exception e) {
            apimlLog.log(UT_COMMON_TIMEZONE_INVALID_LOG_KEY, configuredTimezone);
            return UTC_TIMEZONE;
        }
    }
}
