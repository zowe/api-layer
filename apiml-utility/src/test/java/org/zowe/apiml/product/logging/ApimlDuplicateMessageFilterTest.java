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
import ch.qos.logback.core.spi.FilterReply;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApimlDuplicateMessageFilterTest {

    private Logger logger;
    private ApimlDuplicateMessagesFilter apimlDuplicateMessagesFilter;

    @Nested
    class GivenApimlDuplicateMessageFilter {

        @BeforeEach
        void setUp() {
            logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.zowe.apiml.product.logging.ApimlDuplicateMessagesFilter");
            apimlDuplicateMessagesFilter = new ApimlDuplicateMessagesFilter();
            logger.setLevel(Level.INFO);
        }

        @AfterEach
        void tearDown() {
            apimlDuplicateMessagesFilter.stop();
        }

        @Test
        void whenLevelIsLowerThanLoggerEffectiveLevel_thenDeny() {
            apimlDuplicateMessagesFilter.setAllowedRepetitions(0);
            apimlDuplicateMessagesFilter.start();

            // No args
            assertEquals(FilterReply.DENY, apimlDuplicateMessagesFilter.decide(null, logger, Level.DEBUG,
                "Message", null, null), "Expected FilterReply.DENY");
        }

        @Test
        void whenFormatIsNull_testDecide() {
            apimlDuplicateMessagesFilter.setAllowedRepetitions(0);
            apimlDuplicateMessagesFilter.start();

            RuntimeException exception = new RuntimeException("my exception");

            // No args
            assertEquals(FilterReply.NEUTRAL, apimlDuplicateMessagesFilter.decide(null, logger, Level.INFO,
                null, null, null), "Expected FilterReply.NEUTRAL");
            assertEquals(FilterReply.NEUTRAL, apimlDuplicateMessagesFilter.decide(null, logger, Level.WARN,
                null, null, exception), "Expected FilterReply.NEUTRAL");

            // With args
            assertEquals(FilterReply.DENY, apimlDuplicateMessagesFilter.decide(null, logger, Level.INFO,
                null, new Object[]{1}, null), "Expected FilterReply.DENY");
            assertEquals(FilterReply.DENY, apimlDuplicateMessagesFilter.decide(null, logger, Level.INFO,
                null, new Object[]{1, exception}, null), "Expected FilterReply.DENY");
            assertEquals(FilterReply.NEUTRAL, apimlDuplicateMessagesFilter.decide(null, logger, Level.INFO,
                null, new Object[]{1, new RuntimeException("exception in arguments")}, new RuntimeException("outside exception")), "Expected FilterReply.NEUTRAL");
        }

        @Test
        void whenFormatIsEmpty_testDecide() {
            apimlDuplicateMessagesFilter.setAllowedRepetitions(0);
            apimlDuplicateMessagesFilter.start();

            RuntimeException exception = new RuntimeException("my exception");

            // No args
            assertEquals(FilterReply.NEUTRAL, apimlDuplicateMessagesFilter.decide(null, logger, Level.WARN,
                "", null, null), "Expected FilterReply.NEUTRAL");
            assertEquals(FilterReply.NEUTRAL, apimlDuplicateMessagesFilter.decide(null, logger, Level.ERROR,
                "", null, exception), "Expected FilterReply.NEUTRAL");

            // With args
            assertEquals(FilterReply.DENY, apimlDuplicateMessagesFilter.decide(null, logger, Level.INFO,
                "", new Object[]{1}, null), "Expected FilterReply.DENY");
            assertEquals(FilterReply.DENY, apimlDuplicateMessagesFilter.decide(null, logger, Level.INFO,
                "", new Object[]{1, exception}, null), "Expected FilterReply.DENY");
            assertEquals(FilterReply.NEUTRAL, apimlDuplicateMessagesFilter.decide(null, logger, Level.INFO,
                "", new Object[]{1, new RuntimeException("exception in arguments")}, new RuntimeException("outside exception")), "Expected FilterReply.NEUTRAL");
        }

        @Test
        void whenArgumentsIsNullOrEmpty_testDecide() {
            apimlDuplicateMessagesFilter.setAllowedRepetitions(0);
            apimlDuplicateMessagesFilter.start();

            RuntimeException exception = new RuntimeException("my exception");

            // No exception
            assertEquals(FilterReply.NEUTRAL, apimlDuplicateMessagesFilter.decide(null, logger, Level.INFO,
                "First Message", null, null), "Expected FilterReply.NEUTRAL");
            assertEquals(FilterReply.NEUTRAL, apimlDuplicateMessagesFilter.decide(null, logger, Level.INFO,
                "Second Message", null, null), "Expected FilterReply.NEUTRAL");
            assertEquals(FilterReply.DENY, apimlDuplicateMessagesFilter.decide(null, logger, Level.INFO,
                "First Message", new Object[0], null), "Expected FilterReply.DENY");

            // With Exception
            assertEquals(FilterReply.NEUTRAL, apimlDuplicateMessagesFilter.decide(null, logger, Level.INFO,
                "Second Message", new Object[]{}, exception), "Expected FilterReply.NEUTRAL");
        }

        @Test
        void whenArgsArePresent_testDecide() {
            apimlDuplicateMessagesFilter.setAllowedRepetitions(0);
            apimlDuplicateMessagesFilter.start();

            RuntimeException exception = new RuntimeException("my exception");

            assertEquals(FilterReply.NEUTRAL, apimlDuplicateMessagesFilter.decide(null, logger, Level.INFO, "Message", new Object[]{1}, null),
                "Expected FilterReply.NEUTRAL");
            assertEquals(FilterReply.NEUTRAL, apimlDuplicateMessagesFilter.decide(null, logger, Level.WARN, "Message {}", new Object[]{1}, null),
                "Expected FilterReply.NEUTRAL");
            assertEquals(FilterReply.DENY, apimlDuplicateMessagesFilter.decide(null, logger, Level.INFO, String.format("Message %s", 1), null, null),
                "Expected FilterReply.DENY");
            assertEquals(FilterReply.NEUTRAL, apimlDuplicateMessagesFilter.decide(null, logger, Level.ERROR, "Message {}", new Object[]{1, exception}, null),
                "Expected FilterReply.NEUTRAL");
            assertEquals(FilterReply.DENY, apimlDuplicateMessagesFilter.decide(null, logger, Level.INFO, "Message {}", new Object[]{1}, exception),
                "Expected FilterReply.DENY");
            assertEquals(FilterReply.NEUTRAL, apimlDuplicateMessagesFilter.decide(null, logger, Level.INFO, "Message {} {}",
                new Object[]{1, new RuntimeException("Exception inside formatted message")}, exception), "Expected FilterReply.NEUTRAL");
        }

        @Test
        void whenCacheReachesCapacity_testDecide() {
            apimlDuplicateMessagesFilter.setAllowedRepetitions(0);
            int cacheSize = 5;
            int margin = 2;
            apimlDuplicateMessagesFilter.setCacheSize(cacheSize);
            apimlDuplicateMessagesFilter.start();

            for (int i = 0; i < cacheSize + margin; i++) {
                // "Message 0", "Message 1", "Message 2", "Message 3", "Message 4", "Message 5", "Message 6" pass filtering.
                // "Message 0" and "Message 1" get dropped from cache because cache reaches the capacity
                // "Message 2", "Message 3", "Message 4", "Message 5", "Message 6" remain in cache
                assertEquals(FilterReply.NEUTRAL, apimlDuplicateMessagesFilter.decide(null, logger, Level.INFO, "Message " + i, null, null),
                    "Expected FilterReply.NEUTRAL");
            }

            for (int i = cacheSize - 1; i >= margin; i--) {
                // "Message 4", "Message 3", "Message 2" do not pass filtering because they are in cache
                assertEquals(FilterReply.DENY, apimlDuplicateMessagesFilter.decide(null, logger, Level.INFO, "Message {}", new Object[]{i}, null),
                    "Expected FilterReply.DENY");
            }

            for (int i = margin - 1; i >= 0; i--) {
                // "Message 1", "Message 0" pass filtering because they are not in cache
                assertEquals(FilterReply.NEUTRAL, apimlDuplicateMessagesFilter.decide(null, logger, Level.INFO, "Message " + i, null, null),
                    "Expected FilterReply.NEUTRAL");
            }
        }

        @Test
        void whenThrowableIsPresent_testDecide_whenThrowableIsPresent() {
            apimlDuplicateMessagesFilter.setAllowedRepetitions(0);
            apimlDuplicateMessagesFilter.start();

            RuntimeException filterException = new RuntimeException("Token is not valid");

            assertEquals(FilterReply.NEUTRAL, apimlDuplicateMessagesFilter.decide(null, logger, Level.INFO, "Message {}", new Object[]{1}, null),
                "Expected FilterReply.NEUTRAL");
            assertEquals(FilterReply.NEUTRAL, apimlDuplicateMessagesFilter.decide(null, logger, Level.INFO, "Message {}", new Object[]{1}, filterException),
                "Expected FilterReply.NEUTRAL");
        }

    }

}
