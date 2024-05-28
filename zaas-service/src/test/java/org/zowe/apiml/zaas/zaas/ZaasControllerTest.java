/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.zaas;

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
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.zaas.security.service.TokenCreationService;
import org.zowe.apiml.zaas.security.service.saf.SafIdtException;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSchemeException;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSource;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSourceService;
import org.zowe.apiml.zaas.security.service.schema.source.JwtAuthSource;
import org.zowe.apiml.zaas.security.service.schema.source.OIDCAuthSource;
import org.zowe.apiml.zaas.security.service.schema.source.ParsedTokenAuthSource;
import org.zowe.apiml.zaas.security.service.zosmf.ZosmfService;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;
import org.zowe.apiml.passticket.IRRPassTicketGenerationException;
import org.zowe.apiml.passticket.PassTicketService;
import org.zowe.apiml.security.common.token.NoMainframeIdentityException;
import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.security.common.token.TokenNotValidException;
import org.zowe.apiml.zaas.ZaasTokenResponse;

import javax.management.ServiceNotFoundException;
import java.util.Date;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.zowe.apiml.zaas.zaas.ExtractAuthSourceFilter.AUTH_SOURCE_ATTR;
import static org.zowe.apiml.zaas.zaas.ExtractAuthSourceFilter.AUTH_SOURCE_PARSED_ATTR;

@ExtendWith(SpringExtension.class)
class ZaasControllerTest {

    @Mock
    private AuthSourceService authSourceService;

    @Mock
    private PassTicketService passTicketService;

    @Mock
    private ZosmfService zosmfService;

    @Mock
    private TokenCreationService tokenCreationService;

    private MockMvc mockMvc;
    private JSONObject ticketBody;
    private AuthSource authSource;
    private AuthSource.Parsed authParsedSource;

    private static final String PASSTICKET_URL = "/gateway/zaas/ticket";
    private static final String ZOSMF_TOKEN_URL = "/gateway/zaas/zosmf";
    private static final String ZOWE_TOKEN_URL = "/gateway/zaas/zoweJwt";
    private static final String SAFIDT_URL = "/gateway/zaas/safIdt";

    private static final String USER = "test_user";
    private static final String PASSTICKET = "test_passticket";
    private static final String APPLID = "test_applid";
    private static final String JWT_TOKEN = "jwt_test_token";
    private static final String SAFIDT = "saf_id_token";

    @BeforeEach
    void setUp() throws IRRPassTicketGenerationException, JSONException {
        when(passTicketService.generate(anyString(), anyString())).thenReturn(PASSTICKET);
        ZaasController zaasController = new ZaasController(authSourceService, passTicketService, zosmfService, tokenCreationService);
        MessageService messageService = new YamlMessageService("/gateway-messages.yml");
        mockMvc = MockMvcBuilders.standaloneSetup(zaasController).setControllerAdvice(new ZaasExceptionHandler(messageService)).build();
        ticketBody = new JSONObject()
            .put("applicationName", APPLID);
    }

    @Nested
    class GivenAuthenticated {

        @BeforeEach
        void setUp() {
            authSource = new JwtAuthSource(JWT_TOKEN);
            authParsedSource = new ParsedTokenAuthSource(USER, new Date(111), new Date(222), AuthSource.Origin.ZOSMF);
        }

        @Test
        void whenRequestZosmfToken_thenResponseOK() throws Exception {
            when(zosmfService.exchangeAuthenticationForZosmfToken(JWT_TOKEN, authParsedSource))
                .thenReturn(ZaasTokenResponse.builder().cookieName(ZosmfService.TokenType.JWT.getCookieName()).token(JWT_TOKEN).build());

            mockMvc.perform(post(ZOSMF_TOKEN_URL)
                    .requestAttr(AUTH_SOURCE_ATTR, authSource)
                    .requestAttr(AUTH_SOURCE_PARSED_ATTR, authParsedSource))
                .andExpect(status().is(SC_OK))
                .andExpect(jsonPath("$.cookieName", is(ZosmfService.TokenType.JWT.getCookieName())))
                .andExpect(jsonPath("$.token", is(JWT_TOKEN)));
        }

        @Test
        void whenRequestZoweJwtToken_thenResponseOK() throws Exception {
            when(authSourceService.getJWT(authSource)).thenReturn(JWT_TOKEN);

            mockMvc.perform(post(ZOWE_TOKEN_URL)
                    .requestAttr(AUTH_SOURCE_ATTR, authSource))
                .andExpect(status().is(SC_OK))
                .andExpect(jsonPath("$.token", is(JWT_TOKEN)));
        }

        @Test
        void whenRequestPassticketAndApplNameProvided_thenPassTicketInResponse() throws Exception {
            mockMvc.perform(post(PASSTICKET_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(ticketBody.toString())
                    .requestAttr(AUTH_SOURCE_PARSED_ATTR, authParsedSource))
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
                    .requestAttr(AUTH_SOURCE_PARSED_ATTR, authParsedSource))
                .andExpect(status().is(SC_BAD_REQUEST))
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages[0].messageType").value("ERROR"))
                .andExpect(jsonPath("$.messages[0].messageNumber").value("ZWEAG140E"))
                .andExpect(jsonPath("$.messages[0].messageContent", is("The 'applicationName' parameter name is missing.")));
        }

        @Test
        void whenRequestSafIdtAndApplNameProvided_thenResponseOk() throws Exception {
            when(tokenCreationService.createSafIdTokenWithoutCredentials(USER, APPLID)).thenReturn(SAFIDT);
            mockMvc.perform(post(SAFIDT_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(ticketBody.toString())
                    .requestAttr(AUTH_SOURCE_PARSED_ATTR, authParsedSource))
                .andExpect(status().is(SC_OK))
                .andExpect(jsonPath("$.token", is(SAFIDT)));
        }

        @Test
        void whenRequestSafIdtAndNoApplNameProvided_thenBadRequest() throws Exception {
            when(tokenCreationService.createSafIdTokenWithoutCredentials(USER, APPLID)).thenReturn(SAFIDT);
            ticketBody.put("applicationName", "");

            mockMvc.perform(post(SAFIDT_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(ticketBody.toString())
                    .requestAttr(AUTH_SOURCE_PARSED_ATTR, authParsedSource))
                .andExpect(status().is(SC_BAD_REQUEST))
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages[0].messageType").value("ERROR"))
                .andExpect(jsonPath("$.messages[0].messageNumber").value("ZWEAG140E"))
                .andExpect(jsonPath("$.messages[0].messageContent", is("The 'applicationName' parameter name is missing.")));
        }

        @Nested
        class WhenExceptionOccurs {

            @Test
            void whenRequestingPassticket_thenInternalServerError() throws Exception {
                when(passTicketService.generate(USER, APPLID)).thenThrow(new IRRPassTicketGenerationException(8, 8, 8));

                mockMvc.perform(post(PASSTICKET_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ticketBody.toString())
                        .requestAttr(AUTH_SOURCE_PARSED_ATTR, authParsedSource))
                    .andExpect(status().is(SC_INTERNAL_SERVER_ERROR))
                    .andExpect(jsonPath("$.messages", hasSize(1)))
                    .andExpect(jsonPath("$.messages[0].messageType").value("ERROR"))
                    .andExpect(jsonPath("$.messages[0].messageNumber").value("ZWEAG141E"))
                    .andExpect(jsonPath("$.messages[0].messageContent", is("The generation of the PassTicket failed. Reason: An internal error was encountered.")));
            }

            @Test
            void whenRequestingZosmfTokens_thenServiceUnavailable() throws Exception {
                String expectedMessage = "Unable to obtain a token from z/OSMF service.";
                when(zosmfService.exchangeAuthenticationForZosmfToken(JWT_TOKEN, authParsedSource))
                    .thenThrow(new ServiceNotFoundException(expectedMessage));

                mockMvc.perform(post(ZOSMF_TOKEN_URL)
                        .requestAttr(AUTH_SOURCE_ATTR, authSource)
                        .requestAttr(AUTH_SOURCE_PARSED_ATTR, authParsedSource))
                    .andExpect(status().is(SC_SERVICE_UNAVAILABLE))
                    .andExpect(jsonPath("$.messages", hasSize(1)))
                    .andExpect(jsonPath("$.messages[0].messageNumber").value("ZWEAZ601W"))
                    .andExpect(jsonPath("$.messages[0].messageContent", containsString(expectedMessage)));
            }

            @Test
            void whenRequestingZoweTokensAndTokenNotValidException_thenUnauthorized() throws Exception {
                when(authSourceService.getJWT(authSource))
                    .thenThrow(new TokenNotValidException("token_not_valid"));

                mockMvc.perform(post(ZOWE_TOKEN_URL)
                        .requestAttr(AUTH_SOURCE_ATTR, authSource))
                    .andExpect(status().is(SC_UNAUTHORIZED))
                    .andExpect(jsonPath("$.messages", hasSize(1)))
                    .andExpect(jsonPath("$.messages[0].messageNumber").value("ZWEAG102E"))
                    .andExpect(jsonPath("$.messages[0].messageContent", is("Token is not valid")));
            }

            @Test
            void whenRequestingZoweTokensAndTokenExpireException_thenUnauthorized() throws Exception {
                when(authSourceService.getJWT(authSource))
                    .thenThrow(new TokenExpireException("token_expired"));

                mockMvc.perform(post(ZOWE_TOKEN_URL)
                        .requestAttr(AUTH_SOURCE_ATTR, authSource))
                    .andExpect(status().is(SC_UNAUTHORIZED))
                    .andExpect(jsonPath("$.messages", hasSize(1)))
                    .andExpect(jsonPath("$.messages[0].messageNumber").value("ZWEAG103E"))
                    .andExpect(jsonPath("$.messages[0].messageContent", is("The token has expired")));
            }

            @Test
            void whenRequestingZoweTokensAndUserMissingMapping_thenOkWithTokenInHeader() throws Exception {
                authSource = new OIDCAuthSource(JWT_TOKEN);
                when(authSourceService.getJWT(authSource))
                    .thenThrow(new NoMainframeIdentityException("No user mappring", null, true));

                mockMvc.perform(post(ZOWE_TOKEN_URL)
                        .requestAttr(AUTH_SOURCE_ATTR, authSource))
                    .andExpect(status().is(SC_OK))
                    .andExpect(jsonPath("$.token", is(JWT_TOKEN)))
                    .andExpect(jsonPath("$.headerName", is(ApimlConstants.HEADER_OIDC_TOKEN)));

            }

            @Test
            void whenRequestingZoweTokensAndUserMissingMappingAndTokenIsInvalid_thenUnauthorized() throws Exception {
                authSource = new OIDCAuthSource(JWT_TOKEN);
                when(authSourceService.getJWT(authSource))
                    .thenThrow(new NoMainframeIdentityException("No user mappring", null, false));

                mockMvc.perform(post(ZOWE_TOKEN_URL)
                        .requestAttr(AUTH_SOURCE_ATTR, authSource))
                    .andExpect(status().is(SC_UNAUTHORIZED));
            }

            @Test
            void whenRequestingZoweTokensAndAuthSchemeException_thenUnauthorized() throws Exception {
                when(authSourceService.getJWT(authSource))
                    .thenThrow(new AuthSchemeException("No mainframe identity found."));

                mockMvc.perform(post(ZOWE_TOKEN_URL)
                        .requestAttr(AUTH_SOURCE_ATTR, authSource))
                    .andExpect(status().is(SC_UNAUTHORIZED))
                    .andExpect(jsonPath("$.messages", hasSize(1)))
                    .andExpect(jsonPath("$.messages[0].messageNumber").value("ZWEAG102E"))
                    .andExpect(jsonPath("$.messages[0].messageContent", is("Token is not valid")));
            }

            @Test
            void whenRequestingZoweTokensAndIllegalStateException_thenInternalServerError() throws Exception {
                String expectedMessage = "The z/OSMF is not configured.";
                when(authSourceService.getJWT(authSource))
                    .thenThrow(new IllegalStateException(expectedMessage));

                mockMvc.perform(post(ZOWE_TOKEN_URL)
                        .requestAttr(AUTH_SOURCE_ATTR, authSource))
                    .andExpect(status().is(SC_INTERNAL_SERVER_ERROR))
                    .andExpect(jsonPath("$.messages", hasSize(1)))
                    .andExpect(jsonPath("$.messages[0].messageNumber").value("ZWEAZ600W"))
                    .andExpect(jsonPath("$.messages[0].messageContent", containsString(expectedMessage)));
            }

            @Test
            void whenRequestingSafIdtAndPassticketException_thenInternalServerError() throws Exception {
                when(tokenCreationService.createSafIdTokenWithoutCredentials(USER, APPLID)).thenThrow(new IRRPassTicketGenerationException(8, 8, 8));

                mockMvc.perform(post(SAFIDT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ticketBody.toString())
                        .requestAttr(AUTH_SOURCE_PARSED_ATTR, authParsedSource))
                    .andExpect(status().is(SC_INTERNAL_SERVER_ERROR))
                    .andExpect(jsonPath("$.messages", hasSize(1)))
                    .andExpect(jsonPath("$.messages[0].messageType").value("ERROR"))
                    .andExpect(jsonPath("$.messages[0].messageNumber").value("ZWEAG141E"))
                    .andExpect(jsonPath("$.messages[0].messageContent", is("The generation of the PassTicket failed. Reason: An internal error was encountered.")));
            }

            @Test
            void whenRequestingSafIdtAndSafIdtException_thenInternalServerError() throws Exception {
                when(tokenCreationService.createSafIdTokenWithoutCredentials(USER, APPLID)).thenThrow(new SafIdtException("Test exception message."));

                mockMvc.perform(post(SAFIDT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ticketBody.toString())
                        .requestAttr(AUTH_SOURCE_PARSED_ATTR, authParsedSource))
                    .andExpect(status().is(SC_INTERNAL_SERVER_ERROR))
                    .andExpect(jsonPath("$.messages", hasSize(1)))
                    .andExpect(jsonPath("$.messages[0].messageType").value("ERROR"))
                    .andExpect(jsonPath("$.messages[0].messageNumber").value("ZWEAG150E"))
                    .andExpect(jsonPath("$.messages[0].messageContent", is("SAF IDT generation failed. Reason: Test exception message.")));
            }
        }
    }
}
