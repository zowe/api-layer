package org.zowe.apiml.message.log;/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import org.zowe.apiml.message.core.MessageType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class ApimlLoggerTest {

    @Test
    public void testEmpty() {
        ApimlLogger apimlLogger = ApimlLogger.empty();
        Logger logger = (Logger) ReflectionTestUtils.getField(apimlLogger, "logger");
        assertEquals(ApimlLogger.class.getName(), logger.getName());
        assertNull(ReflectionTestUtils.getField(apimlLogger, "messageService"));

        assertNull(apimlLogger.log("someKey"));
    }

    @Test
    public void testLogLevel() {
        ApimlLogger apimlLogger = new ApimlLogger(ApimlLoggerTest.class, null);

        Logger logger = mock(Logger.class);
        ReflectionTestUtils.setField(apimlLogger, "logger", logger);

        Marker marker = (Marker) ReflectionTestUtils.getField(apimlLogger, "marker");

        apimlLogger.log(MessageType.TRACE, "traceLog", new Object[] {"param1"});
        verify(logger, times(1)).trace(marker, "traceLog", new Object[] {"param1"});

        apimlLogger.log(MessageType.DEBUG, "debugLog", new Object[] {"param2"});
        verify(logger, times(1)).debug(marker, "debugLog", new Object[] {"param2"});

        apimlLogger.log(MessageType.INFO, "infoLog", new Object[] {"param3"});
        verify(logger, times(1)).info(marker, "infoLog", new Object[] {"param3"});

        apimlLogger.log(MessageType.WARNING, "warningLog", new Object[] {"param4"});
        verify(logger, times(1)).warn(marker, "warningLog", new Object[] {"param4"});

        apimlLogger.log(MessageType.ERROR, "errorLog", new Object[] {"param5"});
        verify(logger, times(1)).error(marker, "errorLog", new Object[] {"param5"});

        verify(logger, times(1)).trace((Marker) any(), anyString(), (Object[]) any());
        verify(logger, times(1)).debug((Marker) any(), anyString(), (Object[]) any());
        verify(logger, times(1)).info((Marker) any(), anyString(), (Object[]) any());
        verify(logger, times(1)).warn((Marker) any(), anyString(), (Object[]) any());
        verify(logger, times(1)).error((Marker) any(), anyString(), (Object[]) any());
    }

}
