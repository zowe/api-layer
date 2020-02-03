package org.zowe.apiml.passticket;/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
@RunWith(JUnit4.class)
public class AbstractIRRPassTicketExceptionTest {

    @Test
    public void testErrorCode() {
        TestException te;

        te = new TestException(-1, -1, -1);
        assertSame(AbstractIRRPassTicketException.ErrorCode.ERR_UNKNOWN, te.getErrorCode());

        te = new TestException(8, 16, -1);
        assertSame(AbstractIRRPassTicketException.ErrorCode.ERR_8_16_X, te.getErrorCode());

        te = new TestException(8, 16, 32);
        assertSame(AbstractIRRPassTicketException.ErrorCode.ERR_8_16_32, te.getErrorCode());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, te.getHttpStatus());

        te = new TestException(8, 16, 28);
        assertSame(AbstractIRRPassTicketException.ErrorCode.ERR_8_16_28, te.getErrorCode());
        assertEquals(HttpStatus.SC_BAD_REQUEST, te.getHttpStatus());
    }

    class TestException extends AbstractIRRPassTicketException {

        public TestException(int safRc, int racfRc, int racfRsn) {
            super(safRc, racfRc, racfRsn);
        }

    }

}
