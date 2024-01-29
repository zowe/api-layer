/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zss.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.zowe.apiml.zss.model.Token;
import org.zowe.apiml.zss.services.SafIdtProvider;

import java.util.Optional;

import static jakarta.servlet.http.HttpServletResponse.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
class SafIdtControllerTest {
    private MockMvc mockMvc;

    @Mock
    private SafIdtProvider safProvider;

    @BeforeEach
    void setUp() {
        SafIdtController safIdtController = new SafIdtController(safProvider);
        mockMvc = MockMvcBuilders.standaloneSetup(safIdtController).build();
    }

    @Test
    void whenCallAuthenticateEndpointWithoutPayload_thenReturnBadRequest() throws Exception {
        mockMvc
            .perform(post("/zss/saf/authenticate"))
            .andExpect(status().is(SC_BAD_REQUEST));
    }

    @Test
    void whenCallAuthenticateEndpointWithWrongPayload_thenReturnBadRequest() throws Exception {
        mockMvc
            .perform(post("/zss/saf/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"\", \"pass\": \"\"}"))
            .andExpect(status().is(SC_BAD_REQUEST));
    }

    @Test
    void whenCallAuthenticateEndpointWithValidPayload_thenReturnOkAndToken() throws Exception {
        Token valid = new Token();
        valid.setJwt("safJwt");

        when(safProvider.authenticate(any())).thenReturn(Optional.of(valid));
        mockMvc
            .perform(
                post("/zss/saf/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\": \"validName\", \"pass\": \"validPass\", \"appl\": \"ZOWEAPPL\"}"))
            .andExpect(status().is(SC_CREATED))
            .andExpect(content().json("{\"jwt\": \"safJwt\"}"));
    }

    @Test
    void whenCallAuthenticateEndpointWithInvalidToken_thenReturnUnauthorized() throws Exception {
        mockMvc
            .perform(post("/zss/saf/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"validUser\", \"pass\": \"invalidPass\", \"appl\": \"ZOWEAPPL\"}"))
            .andExpect(status().is(SC_UNAUTHORIZED));
    }

    @Test
    void whenCallVerifyEndpointWithoutPayload_thenReturnBadRequest() throws Exception {
        mockMvc
            .perform(post("/zss/saf/verify"))
            .andExpect(status().is(SC_BAD_REQUEST));
    }

    @Test
    void whenCallVerifyEndpointWithWrongPayload_thenReturnBadRequest() throws Exception {
        mockMvc
            .perform(post("/zss/saf/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"jwt\": \"\"}"))
            .andExpect(status().is(SC_BAD_REQUEST));
    }

    @Test
    void whenCallVerifyEndpointWithPost_thenReturnToken() throws Exception {
        when(safProvider.verify(any())).thenReturn(true);

        mockMvc
            .perform(post("/zss/saf/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"jwt\": \"safJwt\"}"))
            .andExpect(status().is(SC_OK));
    }

    @Test
    void whenCallVerifyEndpointWithInvalidToken_thenReturnUnauthorized() throws Exception {
        when(safProvider.verify(any())).thenReturn(false);

        mockMvc
            .perform(post("/zss/saf/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"jwt\": \"invalid\"}"))
            .andExpect(status().is(SC_UNAUTHORIZED));
    }
}
