/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.passticket;

import org.zowe.apiml.passticket.PassTicketService.DefaultPassTicketImpl;
import org.zowe.apiml.util.ClassOrDefaultProxyUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.zowe.apiml.passticket.PassTicketService.DefaultPassTicketImpl.*;

class PassTicketServiceTest {

    private static final String TEST_USERID = "userId";

    private PassTicketService passTicketService;
    private DefaultPassTicketImpl dpti = new DefaultPassTicketImpl();

    private static String evaluated;

    @BeforeEach
    void setUp() {
        passTicketService = new PassTicketService();
    }

    @Test
    void testIsUsingSafImplementation() {
        IRRPassTicket irrPassTicket = (IRRPassTicket) ReflectionTestUtils.getField(passTicketService, "irrPassTicket");
        ClassOrDefaultProxyUtils.ClassOrDefaultProxyState stateInterface = (ClassOrDefaultProxyUtils.ClassOrDefaultProxyState) irrPassTicket;
        assertEquals(stateInterface.isUsingBaseImplementation(), passTicketService.isUsingSafImplementation());
    }

    @Test
    void testInit() throws IRRPassTicketEvaluationException, IRRPassTicketGenerationException {
        PassTicketService passTicketService = new PassTicketService();
        ReflectionTestUtils.setField(passTicketService, "irrPassTicket", new IRRPassTicket() {
            @Override
            public void evaluate(String userId, String applId, String passTicket) {
                evaluated = userId + "-" + applId + "-" + passTicket;
            }

            @Override
            public String generate(String userId, String applId) {
                return userId + "-" + applId;
            }
        });

        evaluated = null;
        passTicketService.evaluate("userId", "applId", "passTicket");
        assertEquals("USERID-APPLID-PASSTICKET", evaluated);
        passTicketService.evaluate("1", "2", "3");
        assertEquals("1-2-3", evaluated);

        assertEquals("USERID-APPLID", passTicketService.generate("userId", "applId"));
        assertEquals("1-2", passTicketService.generate("1", "2"));
    }

    @Test
    void testProxy() throws IRRPassTicketGenerationException {
        IRRPassTicket irrPassTicket = ClassOrDefaultProxyUtils.createProxy(
            IRRPassTicket.class,
            "notExistingClass",
            Impl::new
        );

        try {
            irrPassTicket.evaluate(TEST_USERID, "applId", "passTicket");
            fail();
        } catch (Exception e) {
            assertEquals("Dummy implementation of evaluate : userId x applId x passTicket", e.getMessage());
        }

        assertEquals("success", irrPassTicket.generate(TEST_USERID, "applId"));
    }

    @Test
    void testDefaultPassTicketImpl_EvaluatePassticket() {
        IRRPassTicketEvaluationException e = assertThrows(IRRPassTicketEvaluationException.class, () -> {
            dpti.evaluate(TEST_USERID, "applId", "passticket");
        });
        assertEquals(8, e.getSafRc());
        assertEquals(16, e.getRacfRc());
        assertEquals(32, e.getRacfRsn());
    }

    @Test
    void testDefaultPassTicketImpl_EvaluateTwoPassTickets() {
        String passTicket1 = assertDoesNotThrow(() -> dpti.generate(TEST_USERID, "applId"));
        String passTicket2 = assertDoesNotThrow(() -> dpti.generate(TEST_USERID, "applId"));

        assertNotNull(passTicket1);
        assertNotNull(passTicket2);
        assertNotEquals(passTicket1, passTicket2);

        assertDoesNotThrow(() -> dpti.evaluate(TEST_USERID, "applId", passTicket1));
        assertDoesNotThrow(() -> dpti.evaluate(TEST_USERID, "applId", passTicket2));

        // different user, should throw exception
        assertThrows(IRRPassTicketEvaluationException.class, () -> dpti.evaluate("userx", "applId", passTicket1));
        // different applId, should throw exception
        assertThrows(IRRPassTicketEvaluationException.class, () -> dpti.evaluate(TEST_USERID, "applIdx", passTicket1));
    }

    @Test
    void testDefaultPassTicketImpl_EvaluateWrongPassticket() {
        IRRPassTicketEvaluationException e = assertThrows(IRRPassTicketEvaluationException.class, () -> {
            dpti.evaluate(TEST_USERID, "applId", "wrongPassTicket");
        });
        assertEquals(AbstractIRRPassTicketException.ErrorCode.ERR_8_16_32, e.getErrorCode());
    }

    @Test
    void testDefaultPassTicketImpl_EvaluateAnyPassticket() {
        IRRPassTicketEvaluationException e = assertThrows(IRRPassTicketEvaluationException.class, () -> {
            dpti.evaluate("anyUser", UNKNOWN_APPLID, "anyPassTicket");
        });
        assertEquals(AbstractIRRPassTicketException.ErrorCode.ERR_8_16_28, e.getErrorCode());
    }

    @Test
    void testDefaultPassTicketImpl_EvaluateDummyPassticket() {
        assertDoesNotThrow(() -> dpti.evaluate(ZOWE_DUMMY_USERID, "anyApplId", ZOWE_DUMMY_PASS_TICKET_PREFIX));
        assertDoesNotThrow(() -> dpti.evaluate(ZOWE_DUMMY_USERID, "anyApplId", ZOWE_DUMMY_PASS_TICKET_PREFIX + "xyz"));

        assertThrows(IRRPassTicketEvaluationException.class, () -> {
            dpti.evaluate("unknownUser", "anyApplId", ZOWE_DUMMY_PASS_TICKET_PREFIX);
        });

        assertThrows(IRRPassTicketEvaluationException.class, () -> {
            dpti.evaluate(ZOWE_DUMMY_USERID, "anyApplId", "wrongPassticket");
        });
    }

    @Test
    void testDefaultPassTicketImpl_GenerateUnknownUser() {
        IRRPassTicketGenerationException e = assertThrows(IRRPassTicketGenerationException.class, () -> {
            dpti.generate(PassTicketService.DefaultPassTicketImpl.UNKNOWN_USER, "anyApplId");
        });
        assertEquals(8, e.getSafRc());
        assertEquals(8, e.getRacfRc());
        assertEquals(16, e.getRacfRsn());
        assertNotNull(e.getErrorCode());
        assertEquals(AbstractIRRPassTicketException.ErrorCode.ERR_8_8_16, e.getErrorCode());
        assertEquals("Error on generation of PassTicket: Not authorized to use this service. Verify that the user and the application name are valid, and check that corresponding permissions have been set up.", e.getMessage());
    }

    @Test
    void testDefaultPassTicketImpl_GenerateDummyUser() {
        String passTicket = assertDoesNotThrow(() -> dpti.generate(DUMMY_USER, "anyApplid"));
        assertEquals(ZOWE_DUMMY_PASS_TICKET_PREFIX, passTicket);
    }

    @Test
    void testDefaultPassTicketImpl_GenerateAnyUser() {
        IRRPassTicketGenerationException e = assertThrows(IRRPassTicketGenerationException.class, () -> {
            dpti.generate("anyUser", UNKNOWN_APPLID);
        });
        assertEquals(8, e.getSafRc());
        assertEquals(16, e.getRacfRc());
        assertEquals(28, e.getRacfRsn());
        assertNotNull(e.getErrorCode());
        assertEquals(AbstractIRRPassTicketException.ErrorCode.ERR_8_16_28, e.getErrorCode());
    }

    public static class Impl implements IRRPassTicket {

        @Override
        public void evaluate(String userId, String applId, String passTicket) {
            throw new RuntimeException("Dummy implementation of evaluate : " + userId + " x " + applId + " x " + passTicket);
        }

        @Override
        public String generate(String userId, String applId) {
            return "success";
        }

    }

}
