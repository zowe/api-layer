/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.controllers;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.zowe.apiml.gateway.security.service.schema.source.*;
import org.zowe.apiml.gateway.security.service.zosmf.ZosmfService;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketService;

import java.util.Date;
import java.util.Optional;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
class ZaasControllerTest {

    @Mock
    private AuthSourceService authSourceService;

    @Mock
    private PassTicketService passTicketService;

    private MockMvc mockMvc;

    private JSONObject body;

    private AuthSource.Parsed authSource;

    private static final String URL = "/gateway/zaas/ticket";
    private static final String ZOWE_JWT_URL = "/gateway/zaas/zoweJwt";
    private static final String JWT_TOKEN = "jwt_test_token";

    private static final String PASSTICKET = "test_passticket";
    private static final String APPLID = "test_applid";

    @BeforeEach
    void setUp() throws IRRPassTicketGenerationException, JSONException {
        MessageService messageService = new YamlMessageService("/gateway-messages.yml");

        when(passTicketService.generate(anyString(), anyString())).thenReturn(PASSTICKET);
        ZaasController zaasController = new ZaasController(authSourceService, passTicketService, messageService);
        mockMvc = MockMvcBuilders.standaloneSetup(zaasController).build();
        body = new JSONObject()
            .put("applicationName", APPLID);
    }

    @Nested
    class GivenAuthenticated {

        private static final String USER = "test_user";

        @BeforeEach
        void setUp() {
            authSource = new ParsedTokenAuthSource(USER, new Date(111), new Date(222), AuthSource.Origin.ZOSMF);
        }

        @Test
        void whenApplNameProvided_thenPassTicketInResponse() throws Exception {
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString())
                    .requestAttr("zaas.auth.source", authSource))
                .andExpect(status().is(SC_OK))
                .andExpect(jsonPath("$.ticket", is(PASSTICKET)))
                .andExpect(jsonPath("$.userId", is(USER)))
                .andExpect(jsonPath("$.applicationName", is(APPLID)));
        }

        @Test
        void whenNoApplNameProvided_thenBadRequest() throws Exception {
            body.put("applicationName", "");

            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString())
                    .requestAttr("zaas.auth.source", authSource))
                .andExpect(status().is(SC_BAD_REQUEST))
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages[0].messageType").value("ERROR"))
                .andExpect(jsonPath("$.messages[0].messageNumber").value("ZWEAG140E"))
                .andExpect(jsonPath("$.messages[0].messageContent", is("The 'applicationName' parameter name is missing.")));
        }

        @Test
        void whenErrorGeneratingPassticket_thenInternalServerError() throws Exception {
            when(passTicketService.generate(anyString(), anyString())).thenThrow(new IRRPassTicketGenerationException(8, 8, 8));

            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString())
                    .requestAttr("zaas.auth.source", authSource))
                .andExpect(status().is(SC_INTERNAL_SERVER_ERROR))
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages[0].messageType").value("ERROR"))
                .andExpect(jsonPath("$.messages[0].messageNumber").value("ZWEAG141E"))
                .andExpect(jsonPath("$.messages[0].messageContent", is("The generation of the PassTicket failed. Reason: An internal error was encountered.")));
        }

        @Test
        void whenRequestZoweJwtToken_thenResponseOK() throws Exception {
            JwtAuthSource jwtAuthSource = new JwtAuthSource(JWT_TOKEN);
            when(authSourceService.getAuthSourceFromRequest(any())).thenReturn(Optional.of(jwtAuthSource));
            when(authSourceService.getJWT(jwtAuthSource)).thenReturn(JWT_TOKEN);

            mockMvc.perform(post(ZOWE_JWT_URL))
                .andExpect(status().is(SC_OK))
                .andExpect(jsonPath("$.token", is(JWT_TOKEN)));
        }

    }

    @Nested
    class GivenNotAuthenticated {

        @BeforeEach
        void setUp() {
            authSource = new ParsedTokenAuthSource(null, null, null, null);
        }

        @Test
        void thenRespondUnauthorized() throws Exception {
            mockMvc.perform(post(URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString())
                    .requestAttr("zaas.auth.source", authSource))
                .andExpect(status().is(SC_UNAUTHORIZED));
        }

        @Test
        void whenRequestZoweJwtTokenAndInvalidSource_thenResponseUnauthorized() throws Exception {
            mockMvc.perform(post(ZOWE_JWT_URL))
                .andExpect(status().is(SC_UNAUTHORIZED));
        }

    }
}
