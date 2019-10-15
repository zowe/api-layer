package com.ca.mfaas.product.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.spi.FilterReply;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ApimlDependencyLogHiderTest {

    private final ApimlDependencyLogHider apimlDependencyLogHider = new ApimlDependencyLogHider();
    private final Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("com.ca.logger");

    @Test
    public void testDecide_whenLoggerLevelIsLowThanInfo() {
        logger.setLevel(Level.DEBUG);

        FilterReply actualFilterReply = apimlDependencyLogHider.decide(null, logger, null,
            "Message text", null, null);

        assertEquals("Log levels are not same", FilterReply.NEUTRAL, actualFilterReply);
    }


    @Test
    public void testDecide_whenLoggerLevelWhenIgnoredMessagesArePresent() {
        logger.setLevel(Level.INFO);

        Map<String, Boolean> logMessages = new HashMap<>();
        logMessages.put("Tomcat initialized with port(s): 10011 (https)", true);
        logMessages.put("The replica size seems to be empty. Check the route 53 DNS Registry", true);
        logMessages.put("Test message", false);

        logMessages.forEach((logMessage, shouldBeIgnored) -> {
            FilterReply actualFilterReply = apimlDependencyLogHider.decide(null, logger, null,
                logMessage, null, null);

            FilterReply expectedFilterReply = shouldBeIgnored ? FilterReply.DENY : FilterReply.NEUTRAL;
            assertEquals("Log levels are not same", expectedFilterReply, actualFilterReply);
        });
    }
}
