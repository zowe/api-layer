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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.lang.JoseException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;
import org.zowe.apiml.security.common.token.AccessTokenProvider;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.security.common.token.TokenNotValidException;
import org.zowe.apiml.zaas.security.service.AuthenticationService;
import org.zowe.apiml.zaas.security.service.JwtSecurity;
import org.zowe.apiml.zaas.security.service.token.OIDCTokenProvider;
import org.zowe.apiml.zaas.security.service.zosmf.ZosmfService;
import org.zowe.apiml.zaas.security.webfinger.WebFingerProvider;
import org.zowe.apiml.zaas.security.webfinger.WebFingerResponse;

import java.io.IOException;
import java.math.BigInteger;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
class AuthControllerTest {

    private static final String INVALIDATE = "/zaas/api/v1/auth/invalidate";
    private static final String BEARER = "Bearer ";
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

    private JsonWebKey zosmfJwk, apimlJwk;
    private JSONObject body;

    @BeforeEach
    void setUp() throws JSONException, JoseException {
        messageService = new YamlMessageService("/zaas-log-messages.yml");
        authController = new AuthController(authenticationService, jwtSecurity, zosmfService, messageService, tokenProvider, oidcProvider, webFingerProvider);
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        body = new JSONObject()
            .put("token", "token")
            .put("serviceId", "service");

        zosmfJwk = getJwk(1);
        apimlJwk = getJwk(2);
    }

    @Test
    void distributeInvalidate() throws Exception {
        when(authenticationService.distributeInvalidate("instance/1")).thenReturn(true);
        this.mockMvc.perform(get("/zaas/api/v1/auth/distribute/instance/1")).andExpect(status().is(SC_OK));

        when(authenticationService.distributeInvalidate("instance2")).thenReturn(false);
        this.mockMvc.perform(get("/zaas/api/v1/auth/distribute/instance2")).andExpect(status().is(SC_NO_CONTENT));
    }

    @ParameterizedTest
    @MethodSource("jwtArguments")
    void invalidateJwt(String header, HttpMethod method, int status, String tokenToMock, Class<? extends Throwable> exceptionToThrow) throws Exception {

        if (tokenToMock != null) {
            if (exceptionToThrow != null) {
                when(authenticationService.invalidateJwtToken(tokenToMock, false))
                    .thenThrow(exceptionToThrow);
            } else {
                when(authenticationService.invalidateJwtToken(tokenToMock, false))
                    .thenReturn(Boolean.TRUE);
            }
        }
        var request = request(method, INVALIDATE);
        if (header != null) {
            request.header(AUTHORIZATION, header);
        }
        mockMvc.perform(request)
            .andExpect(status().is(status));
        if (tokenToMock != null) {
            verify(authenticationService, times(1)).invalidateJwtToken(tokenToMock, false);
        }
    }

    private static Stream<Arguments> jwtArguments() {
        return Stream.of(
            arguments(null, DELETE, SC_BAD_REQUEST, null, null),
            arguments("wibble", DELETE, SC_BAD_REQUEST, null, null),
            arguments(BEARER, DELETE, SC_BAD_REQUEST, null, null),
            arguments(BEARER + "xyz", GET, SC_METHOD_NOT_ALLOWED, null, null),
            arguments(BEARER + "xyz", DELETE, SC_SERVICE_UNAVAILABLE, null, null),
            arguments(BEARER + "abcde", DELETE, SC_OK, "abcde", null),
            arguments(BEARER + "fghij", DELETE, SC_BAD_REQUEST, "fghij", TokenNotValidException.class)        );
    }

    private JsonWebKey getJwk(int i) throws JoseException {
        return JsonWebKey.Factory.newJwk("{" +
            "\"e\":\"AQAB\"," +
            "\"n\":\"kWp2zRA23Z3vTL4uoe8kTFptxBVFunIoP4t_8TDYJrOb7D1iZNDXVeEsYKp6ppmrTZDAgd-cNOTKLd4M39WJc5FN0maTAVKJc7NxklDeKc4dMe1BGvTZNG4MpWBo-taKULlYUu0ltYJuLzOjIrTHfarucrGoRWqM0sl3z2-fv9k\",\n" +
            "\"kty\":\"RSA\",\n" +
            "\"kid\":\"" + i + "\"" +
            "}");
    }

    private void initPublicKeys() {
        var zosmf = mock(JsonWebKeySet.class);
        when(zosmf.getJsonWebKeys()).thenReturn(
            Collections.singletonList(zosmfJwk)
        );

        when(zosmfService.getPublicKeys()).thenReturn(zosmf);
        when(jwtSecurity.getPublicKeyInSet()).thenReturn(new JsonWebKeySet(Collections.singletonList(apimlJwk)));
        when(jwtSecurity.getJwkPublicKey()).thenReturn(Optional.of(apimlJwk));
    }

    @Test
    void testGetAllPublicKeys() throws Exception {
        initPublicKeys();
        when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.ZOSMF);
        var jwkSet = new JsonWebKeySet(Arrays.asList(zosmfJwk, apimlJwk));
        this.mockMvc.perform(get("/zaas/api/v1/auth/keys/public/all"))
            .andExpect(status().is(SC_OK))
            .andExpect(content().json(jwkSet.toJson()));
    }

    @Test
    void givenAPIMLJWTProducer_whenGetAllPublicKeys_thenReturnsOnlyAPIMLKeys() throws Exception {
        initPublicKeys();
        when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.APIML);
        var jwkSet = new JsonWebKeySet(Collections.singletonList(apimlJwk));
        this.mockMvc.perform(get("/zaas/api/v1/auth/keys/public/all"))
            .andExpect(status().is(SC_OK))
            .andExpect(content().json(jwkSet.toJson()));
    }

    @Test
    void givenOIDCJWKSet_whenGetAllPublicKeys_thenIncludeOIDCInResult() throws Exception {
        initPublicKeys();
        var mockedJwkSet = mock(JsonWebKeySet.class);
        var oidcJwk = getJwk(3);
        when(oidcProvider.getJwkSet()).thenReturn(mockedJwkSet);
        when(mockedJwkSet.getJsonWebKeys()).thenReturn(Collections.singletonList(oidcJwk));

        var jwkSet = new JsonWebKeySet(Arrays.asList(apimlJwk, oidcJwk));
        this.mockMvc.perform(get("/zaas/api/v1/auth/keys/public/all"))
            .andExpect(status().is(SC_OK))
            .andExpect(content().json(jwkSet.toJson()));
    }

    @Nested
    class WhenGettingActiveKey {
        @Test
        void useZoweJwt() throws Exception {
            initPublicKeys();
            when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.APIML);
            var jwkSet = new JsonWebKeySet(Collections.singletonList(apimlJwk));
            mockMvc.perform(get("/zaas/api/v1/auth/keys/public/current"))
                .andExpect(status().is(SC_OK))
                .andExpect(content().json(jwkSet.toJson()));
        }

        @Test
        void returnEmptyWhenUnknown() throws Exception {
            initPublicKeys();
            when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.UNKNOWN);
            var jwkSet = new JsonWebKeySet(Collections.emptyList());
            mockMvc.perform(get("/zaas/api/v1/auth/keys/public/current"))
                .andExpect(status().is(SC_OK))
                .andExpect(content().json(jwkSet.toJson()));
        }

        @Test
        void useZosmf() throws Exception {
            initPublicKeys();
            var jwkSet = new JsonWebKeySet(Collections.singletonList(zosmfJwk));
            when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.ZOSMF);
            mockMvc.perform(get("/zaas/api/v1/auth/keys/public/current"))
                .andExpect(status().is(SC_OK))
                .andExpect(content().json(jwkSet.toJson()));
        }
    }

    @Nested
    class GetPublicKeyUsedForSigning {
        @Nested
        class GivenZosmfIsProducer {
            @Test
            void whenOnlineAndSupportJwt_returnValidPemKey() throws Exception {
                when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.ZOSMF);
                when(zosmfService.getPublicKeys()).thenReturn(new JsonWebKeySet(getJwk(0)));

                mockMvc.perform(get("/zaas/api/v1/auth/keys/public"))
                    .andExpect(status().is(SC_OK));
            }

            @Test
            void whenToPublicKeyThrowsException_thenReturnsInternalServerError() throws Exception {
                byte[] badModulus = new byte[]{0};

                var badKey = mock(RSAPublicKey.class);
                when(badKey.getModulus()).thenReturn(new BigInteger(badModulus));
                when(badKey.getPublicExponent()).thenReturn(BigInteger.ONE);
                when(badKey.getAlgorithm()).thenReturn("RSA");
                when(badKey.getFormat()).thenReturn(null);
                when(badKey.getEncoded()).thenReturn(new byte[0]);

                var badJwk = JsonWebKey.Factory.newJwk(badKey);

                when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.APIML);
                when(jwtSecurity.getPublicKeyInSet()).thenReturn(new JsonWebKeySet(List.of(badJwk)));

                mockMvc.perform(get("/zaas/api/v1/auth/keys/public"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.messageNumber").value("ZWEAG717E"));
            }

                @Test
            void whenNotReady_returnInternalServerError() throws Exception {
                when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.UNKNOWN);

                mockMvc.perform(get("/zaas/api/v1/auth/keys/public"))
                    .andExpect(status().is(SC_INTERNAL_SERVER_ERROR))
                    .andExpect(jsonPath("$.messageNumber", is("ZWEAG716E")));
            }

            @Test
            void whenZosmfReturnsIncorrectAmountOfKeys_returnInternalServerError() throws Exception {
                var jwkList = Arrays.asList(mock(JsonWebKey.class), mock(JsonWebKey.class));
                when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.ZOSMF);
                when(zosmfService.getPublicKeys()).thenReturn(new JsonWebKeySet(jwkList));

                mockMvc.perform(get("/zaas/api/v1/auth/keys/public"))
                    .andExpect(status().is(SC_INTERNAL_SERVER_ERROR))
                    .andExpect(jsonPath("$.messageNumber", is("ZWEAG715E")));
            }
        }

        @Nested
        class GivenApiMlIsProducer {
            @Test
            void returnValidPemKey() throws Exception {
                when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.APIML);
                when(jwtSecurity.getPublicKeyInSet()).thenReturn(new JsonWebKeySet(getJwk(0)));

                mockMvc.perform(get("/zaas/api/v1/auth/keys/public"))
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
                    mockMvc.perform(post("/zaas/api/v1/auth/access-token/validate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body.toString()))
                        .andExpect(status().is(SC_NO_CONTENT));
                }

                @Test
                void return401() throws Exception {
                    when(tokenProvider.isValidForScopes("token", "service")).thenReturn(true);
                    when(tokenProvider.isInvalidated("token")).thenReturn(true);
                    mockMvc.perform(post("/zaas/api/v1/auth/access-token/validate")
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

                    mockMvc.perform(delete("/zaas/api/v1/auth/access-token/revoke")
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

                    mockMvc.perform(delete("/zaas/api/v1/auth/access-token/revoke")
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
                @ValueSource(strings = {"/zaas/api/v1/auth/access-token/revoke/tokens/user", "/zaas/api/v1/auth/access-token/revoke/tokens/scope"})
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
                    var tokenAuthenticationMock = mock(TokenAuthentication.class);
                    when(tokenAuthenticationMock.getPrincipal()).thenReturn("user");
                    when(tokenAuthenticationMock.getType()).thenReturn(TokenAuthentication.Type.JWT);
                    context.setAuthentication(tokenAuthenticationMock);
                    SecurityContextHolder.setContext(context);
                    body = new JSONObject()
                        .put("timestamp", "1234");
                    mockMvc.perform(delete("/zaas/api/v1/auth//access-token/revoke/tokens")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body.toString()))
                        .andExpect(status().is(SC_NO_CONTENT));
                }

                @ParameterizedTest
                @ValueSource(strings = {"scope", "user"})
                void thenReturnErrorMessage(String endpoint) throws Exception {
                    body = new JSONObject();
                    mockMvc.perform(delete("/zaas/api/v1/auth//access-token/revoke/tokens/" + endpoint)
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
            mockMvc.perform(delete("/zaas/api/v1/auth//access-token/evict")
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(SC_NO_CONTENT));
        }
    }

    @Nested
    class GivenValidateOIDCTokenRequest {

        @Nested
        class WhenValidateToken {

            private static final String TOKEN = "ewogICJ0eXAiOiAiSldUIiwKICAibm9uY2UiOiAiYVZhbHVlVG9CZVZlcmlmaWVkIiwKICAiYWxnIjogIlJTMjU2IiwKICAia2lkIjogIlNlQ1JldEtleSIKfQ.ewogICJhdWQiOiAiMDAwMDAwMDMtMDAwMC0wMDAwLWMwMDAtMDAwMDAwMDAwMDAwIiwKICAiaXNzIjogImh0dHBzOi8vb2lkYy5wcm92aWRlci5vcmcvYXBwIiwKICAiaWF0IjogMTcyMjUxNDEyOSwKICAibmJmIjogMTcyMjUxNDEyOSwKICAiZXhwIjogODcyMjUxODEyNSwKICAic3ViIjogIm9pZGMudXNlcm5hbWUiCn0.c29tZVNpZ25lZEhhc2hDb2Rl";

            private String getBody() throws JSONException {
                return new JSONObject()
                    .put("token", TOKEN)
                    .toString();
            }

            @Test
            void validateOIDCToken() throws Exception {
                when(oidcProvider.isValid(TOKEN)).thenReturn(true);
                mockMvc.perform(post("/zaas/api/v1/auth/oidc-token/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getBody()))
                    .andExpect(status().is(SC_OK))
                    .andExpect(jsonPath("sub", is("oidc.username")))
                    .andExpect(jsonPath("iss", is("https://oidc.provider.org/app")));
            }

            @Test
            void return401() throws Exception {
                when(oidcProvider.isValid(TOKEN)).thenReturn(false);
                mockMvc.perform(post("/zaas/api/v1/auth/oidc-token/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getBody()))
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
            MvcResult result = mockMvc.perform(get("/zaas/api/v1/auth/oidc/webfinger?resource=foobar"))
                .andExpect(status().is(SC_OK)).andReturn();
            ObjectMapper mapper = new ObjectMapper();
            WebFingerResponse res = mapper.readValue(result.getResponse().getContentAsString(), WebFingerResponse.class);
            assertEquals(webFingerResponse, res);
        }

        @Test
        void givenNoClientId_thenReturnEmptyList() throws Exception {
            WebFingerResponse webFingerResponse = new WebFingerResponse();
            when(webFingerProvider.getWebFingerConfig("")).thenReturn(webFingerResponse);
            MvcResult result = mockMvc.perform(get("/zaas/api/v1/auth/oidc/webfinger?resource="))
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
                    get("/zaas/api/v1/auth/oidc/webfinger?resource=foobar")
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
            mockMvc.perform(get("/zaas/api/v1/auth/oidc/webfinger?resource=foobar"))
                .andExpect(status().is(SC_NOT_FOUND));
        }

    }

}
