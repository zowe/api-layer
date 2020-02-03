/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.hellospring.controller;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:spring-servlet.xml"})
@WebAppConfiguration
public class MfaasControllerTest {
    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @Test
    public void givenWac_whenServletContext_thenItProvidesMfaasController() {
        ServletContext servletContext = wac.getServletContext();

        Assert.assertNotNull(servletContext);
        Assert.assertTrue(servletContext instanceof MockServletContext);
        Assert.assertNotNull(wac.getBean("mfaasController"));
    }

    @Test
    public void givenHealthUri_whenMockMVC_thenVerifyResponse() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(get("/application/health")
            .accept(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andReturn();

        Assert.assertEquals("application/json;charset=UTF-8",
            mvcResult.getResponse().getContentType());
    }

    @Test
    public void givenInfoUri_whenMockMVC_thenVerifyResponse() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(get("/application/info")
            .accept(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(""))
            .andReturn();

        Assert.assertEquals("application/json;charset=UTF-8",
            mvcResult.getResponse().getContentType());
    }

    @Test
    public void givenApiDocUri_whenMockMVC_thenForwardToSpringFoxDocumentation() throws Exception {
        this.mockMvc.perform(get("/api-doc"))
            .andExpect(forwardedUrl("/v2/api-docs"));

    }

}
