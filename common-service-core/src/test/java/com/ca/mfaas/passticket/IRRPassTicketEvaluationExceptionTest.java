package com.ca.mfaas.passticket;/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
@RunWith(JUnit4.class)
public class IRRPassTicketEvaluationExceptionTest {

    @Test
    public void testInit() {
        IRRPassTicketEvaluationException exception = new IRRPassTicketEvaluationException(8, 12, 20);
        assertEquals(8, exception.getSafRc());
        assertEquals(12, exception.getRacfRc());
        assertEquals(20, exception.getRacfRsn());
        assertEquals(AbstractIRRPassTicketException.ErrorCode.ERR_8_12_20, exception.getErrorCode());
        assertEquals("Error on evaluation of PassTicket: Invocation of the Security Server Network Authentication Service Program Call (PC) interface failed with an 'abend in the PC service routine' return code. The symptom record associated with this abend can be found in the logrec data set.", exception.getMessage());

        IRRPassTicketEvaluationException exception2 = new IRRPassTicketEvaluationException(AbstractIRRPassTicketException.ErrorCode.ERR_8_16_28);
        assertEquals(8, exception2.getSafRc());
        assertEquals(16, exception2.getRacfRc());
        assertEquals(28, exception2.getRacfRsn());
        assertEquals(AbstractIRRPassTicketException.ErrorCode.ERR_8_16_28, exception2.getErrorCode());
    }

}
