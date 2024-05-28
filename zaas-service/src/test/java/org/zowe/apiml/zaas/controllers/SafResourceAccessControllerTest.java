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

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;
import org.zowe.apiml.security.common.auth.saf.SafResourceAccessVerifying;
import org.zowe.apiml.security.common.token.TokenAuthentication;

import static javax.servlet.http.HttpServletResponse.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ExtendWith(SpringExtension.class)
class SafResourceAccessControllerTest {

    private MockMvc mockMvc;
    private SafResourceAccessVerifying safResourceAccessVerifying = mock(SafResourceAccessVerifying.class);
    MessageService messageService = new YamlMessageService("/gateway-messages.yml");

    private final String validRequestBody = "{\n" +
        "\t\"resourceClass\": \"ZOWE\",\n" +
        "\t\"resourceName\": \"APIML.SERVICES\",\n" +
        "\t\"accessLevel\": \"READ\"\n" +
        "}";

    private final String invalidRequestBody = "{\n" +
        "\t\"resourceClass\": \"ZOWE\",\n" +
        "\t\"resourceName\": \"APIML.SERVICES\",\n" +
        "\t\"accessLevel\": \"WIPE\"\n" +
        "}";

    @BeforeEach
    void setUp() {
        SafResourceAccessController controller = new SafResourceAccessController(safResourceAccessVerifying, messageService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        TokenAuthentication auth = new TokenAuthentication("user", "token");
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Nested
    class givenValidRequest {
        @Test
        void returnsNoContent() throws Exception {
            doReturn(true).when(safResourceAccessVerifying).hasSafResourceAccess(any(), any(), any(), any());
            mockMvc.perform(
                post(SafResourceAccessController.FULL_CONTEXT_PATH)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(validRequestBody)
            ).andExpect(status().is(SC_NO_CONTENT));
        }

        @Test
        void returnsUnauthorizedWhenResourceAccessNotPresent() throws Exception {
            doReturn(false).when(safResourceAccessVerifying).hasSafResourceAccess(any(), any(), any(), any());
            mockMvc.perform(
                post(SafResourceAccessController.FULL_CONTEXT_PATH)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(validRequestBody)
            ).andExpect(status().is(SC_UNAUTHORIZED));
        }
    }

    @Test
    void invalidRequestBodyReturnsBadRequest() throws Exception {
        mockMvc.perform(
            post(SafResourceAccessController.FULL_CONTEXT_PATH)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(invalidRequestBody)
        ).andExpect(status().is(SC_BAD_REQUEST));
    }
}
