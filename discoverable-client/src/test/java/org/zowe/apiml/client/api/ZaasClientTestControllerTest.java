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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.web.servlet.MockMvc;
import org.zowe.apiml.client.configuration.ApplicationConfiguration;
import org.zowe.apiml.client.configuration.SpringComponentsConfiguration;
import org.zowe.apiml.client.service.ZaasClientService;
import org.zowe.apiml.zaasclient.exception.ZaasClientErrorCodes;
import org.zowe.apiml.zaasclient.exception.ZaasClientException;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = {ZaasClientTestController.class})
@Import(value = {SpringComponentsConfiguration.class, ApplicationConfiguration.class, AnnotationConfigContextLoader.class})
public class ZaasClientTestControllerTest {
    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper mapper = new ObjectMapper();

    @MockBean
    private ZaasClientService zaasClientService;

    private static final String TOKEN_PREFIX = "apimlAuthenticationToken";

    @Test
    public void forwardLoginTest_successfulLogin() throws Exception {
        LoginRequest loginRequest = new LoginRequest("username", "password");
        when(zaasClientService.login("username", "password")).thenReturn("token");

        this.mockMvc.perform(
            post("/api/v1/zaasClient/login")
                .content(mapper.writeValueAsString(loginRequest))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(content().string("token"));
    }

    @Test
    public void forwardLoginTest_invalidCredentials() throws Exception {
        LoginRequest loginRequest = new LoginRequest("incorrectUser", "incorrectPass");
        when(zaasClientService.login("incorrectUser", "incorrectPass"))
            .thenThrow(new ZaasClientException(ZaasClientErrorCodes.INVALID_AUTHENTICATION));

        this.mockMvc.perform(
            post("/api/v1/zaasClient/login")
                .content(mapper.writeValueAsString(loginRequest))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(status().is(401))
            .andExpect(content().string("Invalid username or password"));
    }

    @Test
    public void givenValidToken_whenPerformingLogout_thenSuccessLogout() throws Exception {
        String token = "token";
        this.mockMvc.perform(
            post("/api/v1/zaasClient/logout")
                .header("Cookie", TOKEN_PREFIX + "=" + token)
                .contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(status().is(204));
    }

    @Test
    public void givenEmptyToken_whenPerformingLogout_thenFailLogout() throws Exception {
        this.mockMvc.perform(
            post("/api/v1/zaasClient/logout")
                .contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(status().is(400));
    }

//    @Test
    public void test() throws Exception {
        this.mockMvc.perform(
            post("/api/v1/zaasClient/logout")
                .header("Cookie", TOKEN_PREFIX + "=" + 1234)
                .contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(status().is(400))
            .andExpect(content().string("Invalid token provided"));

    }
}
