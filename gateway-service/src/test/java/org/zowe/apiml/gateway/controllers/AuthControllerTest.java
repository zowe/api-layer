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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.security.service.JwtSecurity;
import org.zowe.apiml.gateway.security.service.token.OIDCTokenProvider;
import org.zowe.apiml.gateway.security.service.zosmf.ZosmfService;
import org.zowe.apiml.gateway.security.webfinger.WebFingerProvider;
import org.zowe.apiml.gateway.security.webfinger.WebFingerResponse;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;
import org.zowe.apiml.security.common.token.AccessTokenProvider;
import org.zowe.apiml.security.common.token.TokenAuthentication;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
class AuthControllerTest {

    private AuthController authController;
    private MockMvc mockMvc;

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private JwtSecurity jwtSecurity;

    @Mock
    private ZosmfService zosmfService;

    @Mock
    private AccessTokenProvider tokenProvider;

    @Mock
    private OIDCTokenProvider oidcProvider;

    @Mock
    private WebFingerProvider webFingerProvider;

    private MessageService messageService;

    private JWK zosmfJwk, apimlJwk;
    private JSONObject body;

    @BeforeEach
    void setUp() throws ParseException, JSONException {
        messageService = new YamlMessageService("/gateway-log-messages.yml");
        authController = new AuthController(authenticationService, jwtSecurity, zosmfService, messageService, tokenProvider, oidcProvider, webFingerProvider);
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        body = new JSONObject()
            .put("token", "token")
            .put("serviceId", "service");

        zosmfJwk = getJwk(1);
        apimlJwk = getJwk(2);
    }

    @Test
    void invalidateJwtToken() throws Exception {
        when(authenticationService.invalidateJwtToken("a/b", false)).thenReturn(Boolean.TRUE);
        this.mockMvc.perform(delete("/gateway/auth/invalidate/a/b")).andExpect(status().is(SC_OK));

        when(authenticationService.invalidateJwtToken("abcde", false)).thenReturn(Boolean.TRUE);
        this.mockMvc.perform(delete("/gateway/auth/invalidate/abcde")).andExpect(status().is(SC_OK));

        this.mockMvc.perform(delete("/gateway/auth/invalidate/xyz")).andExpect(status().is(SC_SERVICE_UNAVAILABLE));

        verify(authenticationService, times(1)).invalidateJwtToken("abcde", false);
        verify(authenticationService, times(1)).invalidateJwtToken("a/b", false);
    }

    @Test
    void distributeInvalidate() throws Exception {
        when(authenticationService.distributeInvalidate("instance/1")).thenReturn(true);
        this.mockMvc.perform(get("/gateway/auth/distribute/instance/1")).andExpect(status().is(SC_OK));

        when(authenticationService.distributeInvalidate("instance2")).thenReturn(false);
        this.mockMvc.perform(get("/gateway/auth/distribute/instance2")).andExpect(status().is(SC_NO_CONTENT));
    }

    private JWK getJwk(int i) throws ParseException {
        return JWK.parse("{" +
            "\"e\":\"AQAB\"," +
            "\"n\":\"kWp2zRA23Z3vTL4uoe8kTFptxBVFunIoP4t_8TDYJrOb7D1iZNDXVeEsYKp6ppmrTZDAgd-cNOTKLd4M39WJc5FN0maTAVKJc7NxklDeKc4dMe1BGvTZNG4MpWBo-taKULlYUu0ltYJuLzOjIrTHfarucrGoRWqM0sl3z2-fv9k\",\n" +
            "\"kty\":\"RSA\",\n" +
            "\"kid\":\"" + i + "\"" +
            "}");
    }

    private void initPublicKeys() {
        JWKSet zosmf = mock(JWKSet.class);
        when(zosmf.getKeys()).thenReturn(
            Collections.singletonList(zosmfJwk)
        );

        when(zosmfService.getPublicKeys()).thenReturn(zosmf);
        when(jwtSecurity.getPublicKeyInSet()).thenReturn(new JWKSet(Collections.singletonList(apimlJwk)));
        when(jwtSecurity.getJwkPublicKey()).thenReturn(Optional.of(apimlJwk));
    }

    @Test
    void testGetAllPublicKeys() throws Exception {
        initPublicKeys();
        when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.ZOSMF);
        JWKSet jwkSet = new JWKSet(Arrays.asList(zosmfJwk, apimlJwk));
        this.mockMvc.perform(get("/gateway/auth/keys/public/all"))
            .andExpect(status().is(SC_OK))
            .andExpect(content().json(jwkSet.toString()));
    }

    @Test
    void givenAPIMLJWTProducer_whenGetAllPublicKeys_thenReturnsOnlyAPIMLKeys() throws Exception {
        initPublicKeys();
        when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.APIML);
        JWKSet jwkSet = new JWKSet(Collections.singletonList(apimlJwk));
        this.mockMvc.perform(get("/gateway/auth/keys/public/all"))
            .andExpect(status().is(SC_OK))
            .andExpect(content().json(jwkSet.toString()));
    }

    @Test
    void givenOIDCJWKSet_whenGetAllPublicKeys_thenIncludeOIDCInResult() throws Exception {
        initPublicKeys();
        JWKSet mockedJwkSet = mock(JWKSet.class);
        JWK oidcJwk = getJwk(3);
        when(oidcProvider.getJwkSet()).thenReturn(mockedJwkSet);
        when(mockedJwkSet.getKeys()).thenReturn(Collections.singletonList(oidcJwk));

        JWKSet jwkSet = new JWKSet(Arrays.asList(apimlJwk, oidcJwk));
        this.mockMvc.perform(get("/gateway/auth/keys/public/all"))
            .andExpect(status().is(SC_OK))
            .andExpect(content().json(jwkSet.toString()));
    }

    @Nested
    class WhenGettingActiveKey {
        @Test
        void useZoweJwt() throws Exception {
            initPublicKeys();
            when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.APIML);
            JWKSet jwkSet = new JWKSet(Collections.singletonList(apimlJwk));
            mockMvc.perform(get("/gateway/auth/keys/public/current"))
                .andExpect(status().is(SC_OK))
                .andExpect(content().json(jwkSet.toString()));
        }

        @Test
        void returnEmptyWhenUnknown() throws Exception {
            initPublicKeys();
            when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.UNKNOWN);
            JWKSet jwkSet = new JWKSet(Collections.emptyList());
            mockMvc.perform(get("/gateway/auth/keys/public/current"))
                .andExpect(status().is(SC_OK))
                .andExpect(content().json(jwkSet.toString()));
        }

        @Test
        void useZosmf() throws Exception {
            initPublicKeys();
            JWKSet jwkSet = new JWKSet(Collections.singletonList(zosmfJwk));
            when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.ZOSMF);
            mockMvc.perform(get("/gateway/auth/keys/public/current"))
                .andExpect(status().is(SC_OK))
                .andExpect(content().json(jwkSet.toString()));
        }
    }

    @Nested
    class GetPublicKeyUsedForSigning {
        @Nested
        class GivenZosmfIsProducer {
            @Test
            void whenOnlineAndSupportJwt_returnValidPemKey() throws Exception {
                when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.ZOSMF);
                when(zosmfService.getPublicKeys()).thenReturn(new JWKSet(getJwk(0)));

                mockMvc.perform(get("/gateway/auth/keys/public"))
                    .andExpect(status().is(SC_OK));
            }

            @Test
            void whenNotReady_returnInternalServerError() throws Exception {
                when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.UNKNOWN);

                mockMvc.perform(get("/gateway/auth/keys/public"))
                    .andExpect(status().is(SC_INTERNAL_SERVER_ERROR))
                    .andExpect(jsonPath("$.messageNumber", is("ZWEAG716E")));
            }

            @Test
            void whenZosmfReturnsIncorrectAmountOfKeys_returnInternalServerError() throws Exception {
                List<JWK> jwkList = Arrays.asList(mock(JWK.class), mock(JWK.class));
                when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.ZOSMF);
                when(zosmfService.getPublicKeys()).thenReturn(new JWKSet(jwkList));

                mockMvc.perform(get("/gateway/auth/keys/public"))
                    .andExpect(status().is(SC_INTERNAL_SERVER_ERROR))
                    .andExpect(jsonPath("$.messageNumber", is("ZWEAG715E")));
            }
        }

        @Nested
        class GivenApiMlIsProducer {
            @Test
            void returnValidPemKey() throws Exception {
                when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.APIML);
                when(jwtSecurity.getPublicKeyInSet()).thenReturn(new JWKSet(getJwk(0)));

                mockMvc.perform(get("/gateway/auth/keys/public"))
                    .andExpect(status().is(SC_OK));
            }
        }

        @Nested
        class GivenValidateAccessTokenRequest {

            @Nested
            class WhenValidateToken {
                @Test
                void validateAccessToken() throws Exception {
                    when(tokenProvider.isValidForScopes("token", "service")).thenReturn(true);
                    when(tokenProvider.isInvalidated("token")).thenReturn(false);
                    mockMvc.perform(post("/gateway/auth/access-token/validate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body.toString()))
                        .andExpect(status().is(SC_NO_CONTENT));
                }

                @Test
                void return401() throws Exception {
                    when(tokenProvider.isValidForScopes("token", "service")).thenReturn(true);
                    when(tokenProvider.isInvalidated("token")).thenReturn(true);
                    mockMvc.perform(post("/gateway/auth/access-token/validate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body.toString()))
                        .andExpect(status().is(SC_UNAUTHORIZED));
                }
            }
        }

        @Nested
        class GivenRevokeAccessTokenRequest {

            @BeforeEach
            void setUp() throws JSONException {
                body = new JSONObject()
                    .put("token", "token");
            }

            @Nested
            class WhenTokenAlreadyInvalidated {

                @Test
                void thenInvalidateAgain() throws Exception {
                    when(tokenProvider.isInvalidated("token")).thenReturn(true);

                    mockMvc.perform(delete("/gateway/auth/access-token/revoke")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body.toString()))
                        .andExpect(status().is(SC_UNAUTHORIZED));
                }
            }

            @Nested
            class WhenNotInvalidated {

                @Test
                void thenInvalidate() throws Exception {
                    when(tokenProvider.isInvalidated("token")).thenReturn(false);

                    mockMvc.perform(delete("/gateway/auth/access-token/revoke")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body.toString()))
                        .andExpect(status().is(SC_NO_CONTENT));
                }
            }
        }

        @Nested
        class GivenRevokeAccessTokenWithRulesRequest {

            @Nested
            class WhenNotInvalidated {

                @ParameterizedTest
                @ValueSource(strings = {"/gateway/auth/access-token/revoke/tokens/user", "/gateway/auth/access-token/revoke/tokens/scope"})
                void thenInvalidateForScope(String url) throws Exception {
                    body = new JSONObject()
                        .put("userId", "user")
                        .put("serviceId", "user")
                        .put("timestamp", "1234");
                    mockMvc.perform(delete(url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body.toString()))
                        .andExpect(status().is(SC_NO_CONTENT));
                }

                @Test
                void thenInvalidateOwnTokens() throws Exception {
                    SecurityContext context = new SecurityContextImpl();
                    context.setAuthentication(TokenAuthentication.createAuthenticated("user", "token"));
                    SecurityContextHolder.setContext(context);
                    body = new JSONObject()
                        .put("timestamp", "1234");
                    mockMvc.perform(delete("/gateway/auth//access-token/revoke/tokens")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body.toString()))
                        .andExpect(status().is(SC_NO_CONTENT));
                }

                @ParameterizedTest
                @ValueSource(strings = {"scope", "user"})
                void thenReturnErrorMessage(String endpoint) throws Exception {
                    body = new JSONObject();
                    mockMvc.perform(delete("/gateway/auth//access-token/revoke/tokens/" + endpoint)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body.toString()))
                        .andExpect(status().is(SC_BAD_REQUEST)).andExpect(jsonPath("$.messages[0].messageNumber", is("ZWEAT607E")));
                }
            }
        }
    }

    @Nested
    class WhenCallingEvictionRequest {

        @Test
        void thenRemoveRulesAndTokens() throws Exception {
            mockMvc.perform(delete("/gateway/auth//access-token/evict")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(SC_NO_CONTENT));
        }
    }

    @Nested
    class GivenValidateOIDCTokenRequest {

        private static final String TOKEN = "token";

        @Nested
        class WhenValidateToken {
            @Test
            void validateOIDCToken() throws Exception {
                when(oidcProvider.isValid(TOKEN)).thenReturn(true);
                mockMvc.perform(post("/gateway/auth/oidc-token/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                    .andExpect(status().is(SC_OK));
            }

            @Test
            void return401() throws Exception {
                when(oidcProvider.isValid(TOKEN)).thenReturn(false);
                mockMvc.perform(post("/gateway/auth/oidc-token/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                    .andExpect(status().is(SC_UNAUTHORIZED));
            }
        }
    }

    @Nested
    class GivenWebfingerEnabled {
        @BeforeEach
        void setup() {
            when(webFingerProvider.isEnabled()).thenReturn(true);
        }

        @Test
        void givenListedClientId_thenReturnWebfingerRecords() throws Exception {
            WebFingerResponse webFingerResponse = new WebFingerResponse();
            webFingerResponse.setSubject("foobar");
            webFingerResponse.setLinks(Arrays.asList(new WebFingerResponse.Link("http://openid.net/specs/connect/1.0/issuer", "https://foo.org/.well-known")));
            when(webFingerProvider.getWebFingerConfig("foobar")).thenReturn(webFingerResponse);
            MvcResult result = mockMvc.perform(get("/gateway/auth/oidc/webfinger?resource=foobar"))
                .andExpect(status().is(SC_OK)).andReturn();
            ObjectMapper mapper = new ObjectMapper();
            WebFingerResponse res = mapper.readValue(result.getResponse().getContentAsString(), WebFingerResponse.class);
            assertEquals(webFingerResponse, res);
        }

        @Test
        void givenNoClientId_thenReturnEmptyList() throws Exception {
            WebFingerResponse webFingerResponse = new WebFingerResponse();
            when(webFingerProvider.getWebFingerConfig("")).thenReturn(webFingerResponse);
            MvcResult result = mockMvc.perform(get("/gateway/auth/oidc/webfinger?resource="))
                .andExpect(status().is(SC_OK)).andReturn();
            ObjectMapper mapper = new ObjectMapper();
            WebFingerResponse res = mapper.readValue(result.getResponse().getContentAsString(), WebFingerResponse.class);
            assertEquals(webFingerResponse, res);
        }

        @Test
        void givenExceptionThrownByWebfingerProvider_thenReturnErrorMessage() throws Exception {
            body = new JSONObject();
            when(webFingerProvider.getWebFingerConfig("foobar")).thenThrow(new IOException("some error"));
            mockMvc.perform(
                    get("/gateway/auth/oidc/webfinger?resource=foobar")
                        .contentType(MediaType.APPLICATION_JSON).content(body.toString())
                )
                .andExpect(status().is(SC_INTERNAL_SERVER_ERROR))
                .andExpect(jsonPath("$.messages[0].messageNumber", is("ZWEAG180E")));

        }
    }

    @Nested
    class GivenWebfingerDisabled {
        @Test
        void whenRequestWithUserid_thenReturnNotFound() throws Exception {
            when(webFingerProvider.isEnabled()).thenReturn(false);
            mockMvc.perform(get("/gateway/auth/oidc/webfinger?resource=foobar"))
                .andExpect(status().is(SC_NOT_FOUND));
        }

    }

}
