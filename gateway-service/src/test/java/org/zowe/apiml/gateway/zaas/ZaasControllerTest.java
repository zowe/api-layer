/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.zaas;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSource;
import org.zowe.apiml.gateway.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.gateway.security.service.schema.source.ParsedTokenAuthSource;
import org.zowe.apiml.gateway.security.service.zosmf.ZosmfService;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketService;

import java.util.Date;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
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

    @Mock
    private ZosmfService zosmfService;

    private MockMvc mockMvc;

    private JSONObject ticketBody;

    private AuthSource.Parsed authSource;

    private static final String PASSTICKET_URL = "/gateway/zaas/ticket";
    private static final String ZOSMF_TOKEN_URL = "/gateway/zaas/zosmf";

    private static final String USER = "test_user";
    private static final String PASSTICKET = "test_passticket";
    private static final String APPLID = "test_applid";
    private static final String JWT_TOKEN = "jwt_test_token";
    private static final String LTPA_TOKEN = "ltpa_test_token";

    @BeforeEach
    void setUp() throws IRRPassTicketGenerationException, JSONException {
        MessageService messageService = new YamlMessageService("/gateway-messages.yml");

        when(passTicketService.generate(anyString(), anyString())).thenReturn(PASSTICKET);
        ZaasController zaasController = new ZaasController(authSourceService, messageService, passTicketService, zosmfService);
        mockMvc = MockMvcBuilders.standaloneSetup(zaasController).build();
        ticketBody = new JSONObject()
            .put("applicationName", APPLID);
    }

    @Nested
    class GivenAuthenticated {

        @BeforeEach
        void setUp() {
            authSource = new ParsedTokenAuthSource(USER, new Date(111), new Date(222), AuthSource.Origin.ZOSMF);
        }

        @Test
        @Disabled
        void whenRequestZosmfToken_thenResonseOK() throws Exception {
            mockMvc.perform(post(ZOSMF_TOKEN_URL)
                .requestAttr(ZaasController.AUTH_SOURCE_ATTR, authSource))
                .andExpect(status().is(SC_OK))
                .andExpect(jsonPath("$.cookieName", is(ZosmfService.TokenType.JWT.getCookieName())))
                .andExpect(jsonPath("$.token", is(JWT_TOKEN)));
        }

        @Test
        @Disabled
        void whenRequestZoweToken_thenResonseOK() throws Exception {
            mockMvc.perform(post(ZOSMF_TOKEN_URL)
                    .requestAttr(ZaasController.AUTH_SOURCE_ATTR, authSource))
                .andExpect(status().is(SC_OK))
                .andExpect(jsonPath("$.cookieName", is(ZosmfService.TokenType.LTPA.getCookieName())))
                .andExpect(jsonPath("$.token", is(LTPA_TOKEN)));
        }

        @Test
        void whenRequestPassticketAndApplNameProvided_thenPassTicketInResponse() throws Exception {
            mockMvc.perform(post(PASSTICKET_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(ticketBody.toString())
                    .requestAttr(ZaasController.AUTH_SOURCE_ATTR, authSource))
                .andExpect(status().is(SC_OK))
                .andExpect(jsonPath("$.ticket", is(PASSTICKET)))
                .andExpect(jsonPath("$.userId", is(USER)))
                .andExpect(jsonPath("$.applicationName", is(APPLID)));
        }

        @Test
        void whenRequestPassticketAndNoApplNameProvided_thenBadRequest() throws Exception {
            ticketBody.put("applicationName", "");

            mockMvc.perform(post(PASSTICKET_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(ticketBody.toString())
                    .requestAttr(ZaasController.AUTH_SOURCE_ATTR, authSource))
                .andExpect(status().is(SC_BAD_REQUEST))
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages[0].messageType").value("ERROR"))
                .andExpect(jsonPath("$.messages[0].messageNumber").value("ZWEAG140E"))
                .andExpect(jsonPath("$.messages[0].messageContent", is("The 'applicationName' parameter name is missing.")));
        }

        @Nested
        class WhenExceptionOccurs {

            @BeforeEach
            void setUp() throws IRRPassTicketGenerationException {
                when(passTicketService.generate(anyString(), anyString())).thenThrow(new IRRPassTicketGenerationException(8, 8, 8));
            }

            @Test
            void whenRequestingPassticket_thenInternalServerError() throws Exception {
                mockMvc.perform(post(PASSTICKET_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ticketBody.toString())
                        .requestAttr(ZaasController.AUTH_SOURCE_ATTR, authSource))
                    .andExpect(status().is(SC_INTERNAL_SERVER_ERROR))
                    .andExpect(jsonPath("$.messages", hasSize(1)))
                    .andExpect(jsonPath("$.messages[0].messageType").value("ERROR"))
                    .andExpect(jsonPath("$.messages[0].messageNumber").value("ZWEAG141E"))
                    .andExpect(jsonPath("$.messages[0].messageContent", is("The generation of the PassTicket failed. Reason: An internal error was encountered.")));
            }

            @Test
            @Disabled
            void whenRequestingZosmfTokens_thenInternalServerError() throws Exception {
                mockMvc.perform(post(ZOSMF_TOKEN_URL)
                        .requestAttr(ZaasController.AUTH_SOURCE_ATTR, authSource))
                    .andExpect(status().is(SC_INTERNAL_SERVER_ERROR))
                    .andExpect(jsonPath("$.messages", hasSize(1)))
                    .andExpect(jsonPath("$.messages[0].messageType").value("ERROR"))
                    .andExpect(jsonPath("$.messages[0].messageNumber").value("ZWEAG162E"))
                    .andExpect(jsonPath("$.messages[0].messageContent", is("Gateway service failed to obtain token.")));
            }
        }
    }

    @Nested
    class GivenNotAuthenticated {

        @BeforeEach
        void setUp() {
            authSource = new ParsedTokenAuthSource(null, null, null, null);
        }

        @ParameterizedTest
        @ValueSource(strings = {PASSTICKET_URL, ZOSMF_TOKEN_URL})
        void thenRespondUnauthorized(String url) throws Exception {
            mockMvc.perform(post(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(ticketBody.toString())
                    .requestAttr(ZaasController.AUTH_SOURCE_ATTR, authSource))
                .andExpect(status().is(SC_UNAUTHORIZED));
        }
    }
}
