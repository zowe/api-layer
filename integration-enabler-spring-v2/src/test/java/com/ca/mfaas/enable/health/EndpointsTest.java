/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.enable.health;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@Ignore
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "management.endpoints.web.base-path=/application",
        "spring.application.name=test-endpoints",
    })
@DirtiesContext
public class EndpointsTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    /**
     * Called before each test.
     */
    @Before
    public void setUp() {
        this.mvc = MockMvcBuilders.webAppContextSetup(this.context).build();
    }

    /**
     * Test for info page.
     *
     * @throws Exception On failure.
     */
    @Test
    public void info()
            throws Exception {
        this.mvc.perform(MockMvcRequestBuilders.get("/application/info"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    /**
     * Test for health page.
     *
     * @throws Exception On failure.
     */
    @Test
    public void health()
        throws Exception {
        this.mvc.perform(MockMvcRequestBuilders.get("/application/health"))
            .andExpect(MockMvcResultMatchers.status().isOk());
    }
}
