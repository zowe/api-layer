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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.spi.FilterReply;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UseridFilterTest {
    private final Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.zowe.apiml.logger");
    private UseridFilter useridFilter = new UseridFilter();

    @Nested
    class GivenUserNameProp {
        @Test
        void thenFilter() {
            System.getProperties().setProperty("user.name", "user");
            FilterReply actualFilterReply = useridFilter.decide(null, logger, null,
                "Message text", null, null);
            assertEquals(FilterReply.NEUTRAL, actualFilterReply, "Log levels are not same");
        }
    }

    @Nested
    class GivenNullUserNameProp {
        @Test
        void thenFilter() {
            FilterReply actualFilterReply = useridFilter.decide(null, logger, null,
                "Message text", null, null);
            assertEquals(FilterReply.NEUTRAL, actualFilterReply);
        }
    }

}
