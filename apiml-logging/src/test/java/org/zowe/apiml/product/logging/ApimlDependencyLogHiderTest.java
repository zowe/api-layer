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
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ApimlDependencyLogHiderTest {

    private final Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.zowe.apiml.logger");
    private ApimlDependencyLogHider apimlDependencyLogHider = new ApimlDependencyLogHider();

    @Before
    public void setUp() {
        System.getProperties().setProperty("spring.profiles.include", "");
    }

    @Test
    public void testDecide_whenApplicationRunningInDebugMode() {
        System.getProperties().setProperty("spring.profiles.include", "debug");
        ApimlDependencyLogHider apimlDependencyLogHider = new ApimlDependencyLogHider();

        FilterReply actualFilterReply = apimlDependencyLogHider.decide(null, logger, null,
            "Message text", null, null);

        assertEquals("Log levels are not same", FilterReply.NEUTRAL, actualFilterReply);
    }


    @Test
    public void testDecide_whenLoggerLevelIsLowThanInfo() {
        logger.setLevel(Level.DEBUG);

        FilterReply actualFilterReply = apimlDependencyLogHider.decide(null, logger, null,
            "Message text", null, null);

        assertEquals("Log levels are not same", FilterReply.NEUTRAL, actualFilterReply);
    }


    @Test
    public void testDecide_whenIgnoredMessagesArePresent() {
        logger.setLevel(Level.INFO);

        Map<String, Boolean> logMessages = new HashMap<>();
        logMessages.put("Tomcat initialized with port(s): 10011 (https)", true);
        logMessages.put("The replica size seems to be empty. Check the route 53 DNS Registry", true);
        logMessages.put("Peer wants us to take the instance information from it, since the timestamp differs," +
            "Id : {} My Timestamp : {}, Peer's timestamp: {}", true);
        logMessages.put("The replication of task {} failed with response code {}", true);
        logMessages.put("Test message", false);

        logMessages.forEach((logMessage, shouldBeIgnored) -> {
            FilterReply actualFilterReply = apimlDependencyLogHider.decide(null, logger, null,
                logMessage, null, null);

            FilterReply expectedFilterReply = shouldBeIgnored ? FilterReply.DENY : FilterReply.NEUTRAL;
            assertEquals("Log levels are not same", expectedFilterReply, actualFilterReply);
        });
    }

    @Test
    public void testDecide_whenIgnoredMessagesArePresentWithException() {
        logger.setLevel(Level.ERROR);

        String format = "Error during filtering";
        RuntimeException filterException = new RuntimeException("Token is not valid");

        FilterReply actualFilterReply = apimlDependencyLogHider.decide(null, logger, null,
            format, null, filterException);

        FilterReply expectedFilterReply =  FilterReply.DENY;
        assertEquals("Log levels are not same", expectedFilterReply, actualFilterReply);
    }

}
