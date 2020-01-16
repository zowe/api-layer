/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.apiml.security.common.service;

import com.ca.mfaas.util.ClassOrDefaultProxyUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@ContextConfiguration
public class PassTicketServiceTest {

    private static final String TEST_USERID = "userId";

    @Autowired
    private PassTicketService passTicketService;

    private static String evaluated;

    @Test
    @Order(1)
    public void testInit() {
        assertNotNull(passTicketService);
        assertNotNull(ReflectionTestUtils.getField(passTicketService, "irrPassTicket"));
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
    }

    @Test
    @Order(2)
    public void testCalledMethod() throws IRRPassTicketEvaluationException, IRRPassTicketGenerationException {
        evaluated = null;
        passTicketService.evaluate("userId", "applId", "passTicket");
        assertEquals("userId-applId-passTicket", evaluated);
        passTicketService.evaluate("1", "2", "3");
        assertEquals("1-2-3", evaluated);

        assertEquals("userId-applId", passTicketService.generate("userId", "applId"));
        assertEquals("1-2", passTicketService.generate("1", "2"));
    }

    @Test
    public void testProxy() throws IRRPassTicketGenerationException {
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
    public void testDefaultPassTicketImpl() throws IRRPassTicketEvaluationException, IRRPassTicketGenerationException {
        PassTicketService.DefaultPassTicketImpl dpti = new PassTicketService.DefaultPassTicketImpl();

        try {
            dpti.evaluate(TEST_USERID, "applId", "passticket");
            fail();
        } catch (IRRPassTicketEvaluationException e) {
            assertEquals(8, e.getSafRc());
            assertEquals(16, e.getRacfRc());
            assertEquals(32, e.getRacfRsn());
        }

        String passTicket1 = dpti.generate(TEST_USERID, "applId");
        String passTicket2 = dpti.generate(TEST_USERID, "applId");

        assertNotNull(passTicket1);
        assertNotNull(passTicket2);
        assertNotEquals(passTicket1, passTicket2);

        dpti.evaluate(TEST_USERID, "applId", passTicket1);
        dpti.evaluate(TEST_USERID, "applId", passTicket2);

        try {
            dpti.evaluate("userx", "applId", passTicket1);
            fail();
        } catch (IRRPassTicketEvaluationException e) {
            // different user, should throw exception
        }

        try {
            dpti.evaluate(TEST_USERID, "applIdx", passTicket1);
            fail();
        } catch (IRRPassTicketEvaluationException e) {
            // different applId, should throw exception
        }

        try {
            dpti.generate(PassTicketService.DefaultPassTicketImpl.UNKNOWN_USER, "anyApplId");
            fail();
        } catch (IRRPassTicketGenerationException e) {
            assertEquals(8, e.getSafRc());
            assertEquals(8, e.getRacfRc());
            assertEquals(16, e.getRacfRsn());
            assertNotNull(e.getErrorCode());
            assertEquals(AbstractIRRPassTicketException.ErrorCode.ERR_8_8_16, e.getErrorCode());
            assertEquals(
                "Error on generation of PassTicket: Not authorized to use this service."
                , e.getMessage()
            );
        }

        try {
            dpti.generate("anyUser", PassTicketService.DefaultPassTicketImpl.UNKNOWN_APPLID);
            fail();
        } catch (IRRPassTicketGenerationException e) {
            assertEquals(8, e.getSafRc());
            assertEquals(16, e.getRacfRc());
            assertEquals(28, e.getRacfRsn());
            assertNotNull(e.getErrorCode());
            assertEquals(AbstractIRRPassTicketException.ErrorCode.ERR_8_16_28, e.getErrorCode());
        }
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

    @Configuration
    @ComponentScan(basePackageClasses = {PassTicketService.class})
    public static class SpringConfig {

    }

}
