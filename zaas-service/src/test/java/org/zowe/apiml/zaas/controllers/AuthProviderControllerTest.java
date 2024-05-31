/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.controllers;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.zowe.apiml.zaas.security.config.CompoundAuthProvider;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
class AuthProviderControllerTest {

    private MockMvc mockMvc;
    JSONObject body;

    @BeforeEach
    void init() throws JSONException {
        AuthProviderController authProviderController = new AuthProviderController(Mockito.mock(CompoundAuthProvider.class));
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders
            .standaloneSetup(authProviderController)
            .build();
        body = new JSONObject().put("provider", "test");
    }

    @Nested
    class GivenAuthenticationProviderRequest {
        @Test
        void whenCalling_thenReturnNoContent() throws Exception {
            mockMvc.perform(post("/zaas/authentication")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString()))
                .andExpect(status().isNoContent());
        }
    }

}
