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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.zowe.apiml.client.services.JwtTokenService;

import javax.servlet.http.Cookie;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RealJwtTokenEndpoint.class, properties = {"jwtToken.enableMock=true"})
@ContextConfiguration(classes = {JwtTokenService.class, RealJwtTokenEndpoint.class})
class RealJwtTokenEndpointTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void infoTest() throws Exception {
        mvc.perform(get("/zosmf/info")).andExpect(status().isOk());
    }

    @Test
    void inboxTest() throws Exception {

        MvcResult mvcResult = mvc.perform(
            post("/zosmf/services/authenticate")
                .header(HttpHeaders.AUTHORIZATION, "Basic AMIDOINITRITE=")
        ).andReturn();

        String token = mvcResult.getResponse().getHeader("Set-Cookie").replace("jwtToken=", "").replaceAll(";.*$", "");

        mvc.perform(
            get("/zosmf/notifications/inbox")
                .cookie(new Cookie("jwtToken", token))
        ).andExpect(status().isOk());
    }

    @Test
    void jwkTest() throws Exception {
        mvc.perform(get("/jwt/ibm/api/zOSMFBuilder/jwk")).andExpect(status().isOk());
    }

    @Test
    void authenticateTest() throws Exception {
        mvc.perform(
            post("/zosmf/services/authenticate")
                .header(HttpHeaders.AUTHORIZATION, "Basic AMIDOINITRITE=")
        ).andExpect(status().isNoContent()).andExpect(header().exists(HttpHeaders.SET_COOKIE));
    }

    @Test
    void authenticateDeleteTest() throws Exception {
        mvc.perform(delete("/zosmf/services/authenticate")).andExpect(status().isOk());
    }
}
