/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.client.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.zowe.apiml.client.configuration.PassTicketConfiguration;
import org.zowe.apiml.passticket.PassTicketService;

import jakarta.servlet.ServletException;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = { PassTicketTestController.class })
@Import(PassTicketTestControllerTest.Context.class)
class PassTicketTestControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private PassTicketService passTicketService;

    private PassTicketTestController passTicketTestController;

    private static final String ZOWE_PASSTICKET_AUTH_HEADER = "Basic "
            + Base64.getEncoder().encodeToString(("user:ZOWE_DUMMY_PASS_TICKET").getBytes());

    private static final String BAD_PASSTICKET_AUTH_HEADER = "Basic "
            + Base64.getEncoder().encodeToString(("user:bad").getBytes());

    @BeforeEach
    void setUp() {
        passTicketTestController = new PassTicketTestController(passTicketService);
        ReflectionTestUtils.setField(passTicketTestController, "defaultApplId", "ZOWEAPPL");
        mockMvc = MockMvcBuilders.standaloneSetup(passTicketTestController).build();
    }

    @Test
    void callToPassTicketTestEndpointWithCorrectTicket() throws Exception {
        this.mockMvc.perform(get("/api/v1/passticketTest").header("Authorization", ZOWE_PASSTICKET_AUTH_HEADER))
                .andExpect(status().isOk());
    }

    @Test
    void callToPassTicketTestEndpointWithoutTicketFails() throws Exception {
        this.mockMvc.perform(get("/api/v1/passticketTest")).andExpect(status().isBadRequest());
    }

    @Test
    void callToPassTicketTestEndpointWitBadTicketFails() throws Exception {
        assertThrows(ServletException.class, () ->
            this.mockMvc.perform(get("/api/v1/passticketTest").header("Authorization", BAD_PASSTICKET_AUTH_HEADER)));
    }

    @TestConfiguration
    @ComponentScan(basePackageClasses = {
        PassTicketConfiguration.class
    })
    public static class Context {

    }

}
