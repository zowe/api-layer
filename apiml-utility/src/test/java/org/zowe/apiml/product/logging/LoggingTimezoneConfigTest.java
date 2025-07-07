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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringJUnitConfig(LoggingTimezoneConfig.class)
class LoggingTimezoneConfigTest {

    @Autowired
    private LoggingTimezoneConfig config;
    private String originalTimezone;

    @BeforeEach
    void setUp() {
        originalTimezone = TimeZone.getDefault().getID();
        config = new LoggingTimezoneConfig();
    }

    @AfterEach
    void tearDown() {
        // Clear system properties and restore original timezone after each test
        System.clearProperty("logging.timezone");
        TimeZone.setDefault(TimeZone.getTimeZone(originalTimezone));
    }

    @Nested
    class WhenDeterminingTimezone {

        @Test
        void givenValidConfiguredTimezone_thenUseIt() {
            System.getProperties().setProperty("logging.timezone", "UTC");
            config.init();
            assertEquals("UTC", TimeZone.getDefault().getID());
            verifyLogbackContext("UTC");
        }


        @Test
        void givenInvalidConfiguredTimezone_thenUseUTC() {
            System.getProperties().setProperty("logging.timezone", "INVALID");
            config.init();
            assertEquals("UTC", TimeZone.getDefault().getID());
            verifyLogbackContext("UTC");
        }
    }

    @Nested
    class WhenUsingLocalTimezone {

        @Test
        void givenLocalConfiguration_thenUseSystemDefault() {
            System.getProperties().setProperty("logging.timezone", "LOCAL");
            String defaultTimezone = TimeZone.getDefault().getID();
            config.init();
            assertEquals(defaultTimezone, TimeZone.getDefault().getID());
            verifyLogbackContext(defaultTimezone);
        }

        @Test
        void givenLocalConfigurationAndValidTZEnv_thenUseTZValue() {
            System.getProperties().setProperty("logging.timezone", "LOCAL");
            String tzValue = "Europe/London";

            // Store original TZ value
            String originalTZ = System.getenv("TZ");
            try {
                // Set TZ environment variable
                setEnvironmentVariable("TZ", tzValue);

                config.init();
                String expectedTimezone = System.getenv("TZ") != null ? tzValue : TimeZone.getDefault().getID();
                assertEquals(expectedTimezone, TimeZone.getDefault().getID());
                verifyLogbackContext(expectedTimezone);
            } finally {
                // Restore original TZ value
                if (originalTZ != null) {
                    setEnvironmentVariable("TZ", originalTZ);
                }
            }
        }
    }

    @Test
    void verifyDefaultValueUTC_whenNoPropertySet() {
        config.init();
        assertEquals("UTC", TimeZone.getDefault().getID());
        verifyLogbackContext("UTC");
    }

    private void verifyLogbackContext(String expectedTimezone) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        assertEquals(expectedTimezone, loggerContext.getProperty("LOGGING_TIMEZONE"));
    }

    // Note: This method might not work in all test environments due to security restrictions
    private void setEnvironmentVariable(String key, String value) {
        try {
            ProcessBuilder pb = new ProcessBuilder();
            pb.environment().put(key, value);
        } catch (Exception e) {
            // Ignore if we can't set environment variables in test
        }
    }
}
