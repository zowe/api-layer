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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@ContextConfiguration
public class PassTicketServiceTest {

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
    public void testCalledMethod() {
        evaluated = null;
        passTicketService.evaluate("userId", "applId", "passTicket");
        assertEquals("userId-applId-passTicket", evaluated);
        passTicketService.evaluate("1", "2", "3");
        assertEquals("1-2-3", evaluated);

        assertEquals("userId-applId", passTicketService.generate("userId", "applId"));
        assertEquals("1-2", passTicketService.generate("1", "2"));
    }

    @Configuration
    @ComponentScan(basePackageClasses = {PassTicketService.class})
    public static class SpringConfig {

    }

}
