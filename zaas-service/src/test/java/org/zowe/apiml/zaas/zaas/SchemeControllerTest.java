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
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;
import org.zowe.apiml.passticket.*;
import org.zowe.apiml.security.common.token.NoMainframeIdentityException;
import org.zowe.apiml.security.common.token.TokenExpireException;
import org.zowe.apiml.security.common.token.TokenNotValidException;
import org.zowe.apiml.zaas.ZaasTokenResponse;
import org.zowe.apiml.zaas.security.service.TokenCreationService;
import org.zowe.apiml.zaas.security.service.saf.SafIdtException;
import org.zowe.apiml.zaas.security.service.schema.source.*;
import org.zowe.apiml.zaas.security.service.zosmf.ZosmfService;

import javax.management.ServiceNotFoundException;
import java.util.Date;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.zowe.apiml.zaas.zaas.ExtractAuthSourceFilter.AUTH_SOURCE_ATTR;
import static org.zowe.apiml.zaas.zaas.ExtractAuthSourceFilter.AUTH_SOURCE_PARSED_ATTR;

@ExtendWith(SpringExtension.class)
class SchemeControllerTest {

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

    private static final String PASSTICKET_URL = "/zaas/scheme/ticket";
    private static final String ZOSMF_TOKEN_URL = "/zaas/scheme/zosmf";
    private static final String ZOWE_TOKEN_URL = "/zaas/scheme/zoweJwt";
    private static final String SAFIDT_URL = "/zaas/scheme/safIdt";

    private static final String USER = "test_user";
    private static final String PASSTICKET = "test_passticket";
    private static final String APPLID = "test_applid";
    private static final String JWT_TOKEN = "jwt_test_token";
    private static final String SAFIDT = "saf_id_token";

    @BeforeEach
    void setUp() throws PassTicketException, JSONException {
        when(passTicketService.generate(anyString(), anyString())).thenReturn(PASSTICKET);
        SchemeController zaasController = new SchemeController(authSourceService, passTicketService, zosmfService, tokenCreationService);
        MessageService messageService = new YamlMessageService("/zaas-messages.yml");
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
            when(passTicketService.generate(authParsedSource.getUserId(), "")).thenThrow(new ApplicationNameNotProvidedException());

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
        void givenIncorrectMethod_whenRequestPassticket_thenBadRequest() throws Exception {
            ticketBody.put("applicationName", "");

            mockMvc.perform(get(PASSTICKET_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(ticketBody.toString())
                    .requestAttr(AUTH_SOURCE_PARSED_ATTR, authParsedSource))
                .andExpect(status().is(SC_METHOD_NOT_ALLOWED))
                .andExpect(jsonPath("$.messages[0].messageNumber").value("ZWEAG101E"))
                .andExpect(jsonPath("$.messages[0].messageContent", is("Authentication method 'GET' is not supported for URL '/zaas/scheme/ticket'")));
        }

        @Test
        void givenIncorrectMediaType_whenRequestPassticket_thenUnsupportedMedia() throws Exception {
            ticketBody.put("applicationName", "DUMMY");

            mockMvc.perform(post(PASSTICKET_URL)
                    .contentType(MediaType.TEXT_XML)
                    .content(ticketBody.toString())
                    .requestAttr(AUTH_SOURCE_PARSED_ATTR, authParsedSource))
                .andExpect(status().is(SC_UNSUPPORTED_MEDIA_TYPE))
                .andExpect(jsonPath("$.messages[0].messageNumber").value("ZWEAO415E"))
                .andExpect(jsonPath("$.messages[0].messageContent", is("The media format of the requested data is not supported by the service, so the service has rejected the request.")));
        }

        @Test
        void givenInvalidPath_whenRequestPassticket_thenNotFound() throws Exception {
            ticketBody.put("applicationName", "");

            mockMvc.perform(post("/unknown/url")
                    .contentType(MediaType.TEXT_XML)
                    .content(ticketBody.toString())
                    .requestAttr(AUTH_SOURCE_PARSED_ATTR, authParsedSource))
                .andExpect(status().is(SC_NOT_FOUND))
                .andExpect(jsonPath("$.messages[0].messageNumber").value("ZWEAO404E"))
                .andExpect(jsonPath("$.messages[0].messageContent", is("The service can not find the requested resource.")));
        }

        @Test
        void givenMissingRequestAttribute_whenRequestPassticket_thenInternalError() throws Exception {
            ticketBody.put("applicationName", "");

            mockMvc.perform(post(PASSTICKET_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(ticketBody.toString().getBytes()))
                .andExpect(status().is(SC_INTERNAL_SERVER_ERROR))
                .andExpect(jsonPath("$.messages[0].messageNumber").value("ZWEAO500E"))
                .andExpect(jsonPath("$.messages[0].messageContent", is("The service has encountered a situation it doesn't know how to handle. Please contact support for further assistance. More details are available in the log under the provided message instance ID")));

        }

        @Test
        void whenRequestPassticketAndNoUsername_thenInternalServerError() throws Exception {
            ticketBody.put("applicationName", APPLID);
            when(passTicketService.generate(null, APPLID)).thenThrow(new UsernameNotProvidedException());

            mockMvc.perform(post(PASSTICKET_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(ticketBody.toString())
                    .requestAttr(AUTH_SOURCE_PARSED_ATTR, authParsedSource)
                    .requestAttr(AUTH_SOURCE_PARSED_ATTR, new ParsedTokenAuthSource(null, new Date(111), new Date(222), AuthSource.Origin.ZOSMF)))
                .andExpect(status().is(SC_INTERNAL_SERVER_ERROR))
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages[0].messageType").value("ERROR"))
                .andExpect(jsonPath("$.messages[0].messageNumber").value("ZWEAG141E"))
                .andExpect(jsonPath("$.messages[0].messageContent", is("The generation of the PassTicket failed. Reason: Username not provided")));
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
            when(tokenCreationService.createSafIdTokenWithoutCredentials(USER, "")).thenThrow(new ApplicationNameNotProvidedException());
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
                    .andExpect(jsonPath("$.messages[0].messageNumber").value("ZWEAO402E"))
                    .andExpect(jsonPath("$.messages[0].messageContent", is("The request has not been applied because it lacks valid authentication credentials.")));
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
                    .thenThrow(new NoMainframeIdentityException("No user mapping", null, true));

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
                    .thenThrow(new NoMainframeIdentityException("No user mapping", null, false));

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
                    .andExpect(jsonPath("$.messages[0].messageNumber").value("ZWEAO402E"))
                    .andExpect(jsonPath("$.messages[0].messageContent", is("The request has not been applied because it lacks valid authentication credentials.")));
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
