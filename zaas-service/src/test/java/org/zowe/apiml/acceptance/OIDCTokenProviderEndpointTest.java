/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.acceptance;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.response.ValidatableResponse;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.product.web.HttpConfig;
import org.zowe.apiml.security.common.error.ErrorType;
import org.zowe.apiml.security.common.util.JWTTestUtils;
import org.zowe.apiml.zaas.ZaasApplication;
import org.zowe.apiml.zaas.security.mapping.AuthenticationMapper;
import org.zowe.apiml.zaas.security.service.schema.source.OIDCAuthSource;
import org.zowe.apiml.zaas.security.service.token.JWKResolver;
import org.zowe.apiml.zaas.security.service.token.OIDCTokenProvider;

import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.security.SecurityUtils.COOKIE_AUTH_NAME;
import static org.zowe.apiml.zaas.zaas.SchemeController.CONTROLLER_PATH;

@SpringBootTest(
    properties = {
        "apiml.security.oidc.enabled=true",
        "apiml.security.oidc.userInfo.uri=" + OIDCTokenProviderEndpointTest.USER_INFO_URI,
        "apiml.security.filterChainConfiguration=new"
    },
    classes = ZaasApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
class OIDCTokenProviderEndpointTest {

    static final String REQUIRED_AUDIENCE = "APIML-audience";
    static final String USER_INFO_URI = "https://oidc.provider.com/user/info";
    static final String JWKS_URI = "https://oidc.provider.com/keys";
    private static final String SUBJECT = "oidc.username";
    private static final String UNMAPPED_SUBJECT = "unmapped.user";
    private static final JWTTestUtils.JwkAndSet TEST_KEYS;
    private static final String MALFORMED_TOKEN = "invalid-token";
    private static final String MATCHING_AUDIENCE_TOKEN;
    private static final String MATCHING_AUDIENCE_ARRAY_TOKEN;
    private static final String MISSING_AUDIENCE_TOKEN;
    private static final String MISMATCHED_AUDIENCE_TOKEN;
    private static final String MISMATCHED_AUDIENCE_ARRAY_TOKEN;
    private static final String UNKNOWN_KEY_MATCHING_AUDIENCE_TOKEN;
    private static final String UNKNOWN_KEY_MATCHING_AUDIENCE_ARRAY_TOKEN;
    private static final String UNKNOWN_KEY_MISSING_AUDIENCE_TOKEN;
    private static final String UNKNOWN_KEY_MISMATCHED_AUDIENCE_TOKEN;
    private static final String UNKNOWN_KEY_MISMATCHED_AUDIENCE_ARRAY_TOKEN;
    private static final String UNMAPPED_USER_TOKEN;
    private static final String EXPIRED_TOKEN;
    private static final String INVALID_SIGNATURE_TOKEN;

    static {
        try {
            TEST_KEYS = JWTTestUtils.loadPrivateKey("../keystore/service/service.keystore.p12", "localhost", "password");
            String keyId = TEST_KEYS.jwkSet().getJsonWebKeys().get(0).getKeyId();
            PrivateKey privateKey = TEST_KEYS.privateKey();
            Instant expiration = Instant.now().plusSeconds(1200);
            MATCHING_AUDIENCE_TOKEN = signToken(REQUIRED_AUDIENCE, SUBJECT, keyId, expiration, privateKey);
            MATCHING_AUDIENCE_ARRAY_TOKEN = signToken(List.of("other", REQUIRED_AUDIENCE), SUBJECT, keyId, expiration, privateKey);
            MISSING_AUDIENCE_TOKEN = signToken(null, SUBJECT, keyId, expiration, privateKey);
            MISMATCHED_AUDIENCE_TOKEN = signToken("other", SUBJECT, keyId, expiration, privateKey);
            MISMATCHED_AUDIENCE_ARRAY_TOKEN = signToken(List.of("other", "another"), SUBJECT, keyId, expiration, privateKey);

            // The signature is valid, but the advertised key ID is absent from the JWKS.
            UNKNOWN_KEY_MATCHING_AUDIENCE_TOKEN = signToken(REQUIRED_AUDIENCE, SUBJECT, "unknown-key", expiration, privateKey);
            UNKNOWN_KEY_MATCHING_AUDIENCE_ARRAY_TOKEN = signToken(List.of("other", REQUIRED_AUDIENCE), SUBJECT, "unknown-key", expiration, privateKey);
            UNKNOWN_KEY_MISSING_AUDIENCE_TOKEN = signToken(null, SUBJECT, "unknown-key", expiration, privateKey);
            UNKNOWN_KEY_MISMATCHED_AUDIENCE_TOKEN = signToken("other", SUBJECT, "unknown-key", expiration, privateKey);
            UNKNOWN_KEY_MISMATCHED_AUDIENCE_ARRAY_TOKEN = signToken(List.of("other", "another"), SUBJECT, "unknown-key", expiration, privateKey);

            UNMAPPED_USER_TOKEN = signToken(REQUIRED_AUDIENCE, UNMAPPED_SUBJECT, keyId, expiration, privateKey);
            EXPIRED_TOKEN = signToken(REQUIRED_AUDIENCE, SUBJECT, keyId, Instant.now().minusSeconds(30), privateKey);

            var keyGenerator = KeyPairGenerator.getInstance("RSA");
            keyGenerator.initialize(2048);
            // Advertise the known key ID but sign with a different private key.
            INVALID_SIGNATURE_TOKEN = signToken(REQUIRED_AUDIENCE, SUBJECT, keyId, expiration, keyGenerator.generateKeyPair().getPrivate());
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @LocalServerPort
    private int port;
    @Autowired
    private HttpConfig httpConfig;
    @Autowired
    private Environment environment;
    @MockitoBean(name = "secureHttpClientWithKeystore")
    private CloseableHttpClient httpClient;
    @MockitoBean(name = "oidcMapper")
    private AuthenticationMapper mapper;

    private RestAssuredConfig withClientCert;
    private final Map<String, Integer> userInfoResponses = new HashMap<>();

    @BeforeEach
    void setUp() throws Exception {
        withClientCert = RestAssuredConfig.newConfig().sslConfig(new SSLConfig().sslSocketFactory(
            new SSLSocketFactory(httpConfig.secureSslContext(), SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER))); // NOSONAR
        userInfoResponses.clear();
        when(mapper.mapToMainframeUserId(argThat(source -> source instanceof OIDCAuthSource oidc
            && List.of(SUBJECT).equals(oidc.getDistributedId())))).thenReturn("USER");
        when(httpClient.execute(any(HttpGet.class), any(HttpClientResponseHandler.class))).thenAnswer(invocation -> {
            HttpGet request = invocation.getArgument(0);
            String authorization = request.getFirstHeader(HttpHeaders.AUTHORIZATION).getValue();
            int status = userInfoResponses.getOrDefault(authorization, 401);
            HttpClientResponseHandler<?> handler = invocation.getArgument(1);
            try (var response = new BasicClassicHttpResponse(status)) {
                return handler.handleResponse(response);
            }
        });
    }

    @Nested
    @TestPropertySource(properties = "apiml.security.oidc.jwks.uri=")
    class WhenUserInfoValidation {

        @Nested
        class WithoutAudienceValidation {

            @BeforeEach
            void verifyAudienceConfiguration() {
                assertNull(environment.getProperty("apiml.security.oidc.aud"));
            }

            @ParameterizedTest(name = "{0}")
            @MethodSource("org.zowe.apiml.acceptance.OIDCTokenProviderEndpointTest#audienceTokens")
            void givenAnyAudience_whenExchangingToken_thenSuccess(String token) throws Exception {
                stubUserInfoResponse(token, 200);

                var response = exchangeToken(token);
                response.statusCode(200)
                    .body("cookieName", equalTo(COOKIE_AUTH_NAME))
                    .body("headerName", nullValue())
                    .body("token", not(equalTo(token)))
                    .body("token", notNullValue());
                verifyUserInfoCalled(token);
            }

            @Test
            void givenValidTokenWithoutMapping_thenSetOidcTokenHeader() throws Exception {
                stubUserInfoResponse(UNMAPPED_USER_TOKEN, 200);
                when(mapper.mapToMainframeUserId(any())).thenReturn(null);

                exchangeToken(UNMAPPED_USER_TOKEN).statusCode(401)
                    .header(ApimlConstants.HEADER_OIDC_TOKEN, UNMAPPED_USER_TOKEN)
                    .body("messages[0].messageKey", equalTo(ErrorType.IDENTITY_MAPPING_FAILED.getErrorMessageKey()));
                verify(mapper).mapToMainframeUserId(argThat(source -> source instanceof OIDCAuthSource oidc
                    && UNMAPPED_USER_TOKEN.equals(oidc.getRawSource())
                    && List.of(UNMAPPED_SUBJECT).equals(oidc.getDistributedId())));
                verifyUserInfoCalled(UNMAPPED_USER_TOKEN);
            }

            @Test
            void givenUnparseableToken_thenRejectWithoutOidcTokenHeader() {
                // Origin detection rejects malformed tokens before the OIDC provider runs.
                exchangeToken(MALFORMED_TOKEN).statusCode(401)
                    .header(ApimlConstants.HEADER_OIDC_TOKEN, nullValue())
                    .body("token", nullValue());
                verifyNoInteractions(httpClient, mapper);
            }

            @Test
            void givenMatchingAudienceButUserInfoRejects_thenRejectWithoutOidcTokenHeader() throws Exception {
                stubUserInfoResponse(MATCHING_AUDIENCE_TOKEN, 401);

                exchangeToken(MATCHING_AUDIENCE_TOKEN).statusCode(401)
                    .header(ApimlConstants.HEADER_OIDC_TOKEN, nullValue())
                    .body("token", nullValue());
                verifyUserInfoCalled(MATCHING_AUDIENCE_TOKEN);
                verifyNoInteractions(mapper);
            }
        }

        @Nested
        @TestPropertySource(properties = "apiml.security.oidc.aud=" + REQUIRED_AUDIENCE)
        class WithAudienceValidation {

            @BeforeEach
            void verifyAudienceConfiguration() {
                assertEquals(REQUIRED_AUDIENCE, environment.getProperty("apiml.security.oidc.aud"));
            }

            @ParameterizedTest(name = "{0}")
            @MethodSource("org.zowe.apiml.acceptance.OIDCTokenProviderEndpointTest#audienceTokensWithExpectations")
            void givenAudience_whenExchangingToken_thenSucceedOnValidAudience(String token, boolean matches) throws Exception {
                stubUserInfoResponse(token, 200);

                var response = exchangeToken(token);
                if (matches) {
                    response.statusCode(200)
                        .body("cookieName", equalTo(COOKIE_AUTH_NAME))
                        .body("headerName", nullValue())
                        .body("token", not(equalTo(token)))
                        .body("token", notNullValue());
                } else {
                    response.statusCode(401)
                        .header(ApimlConstants.HEADER_OIDC_TOKEN, nullValue())
                        .body("token", nullValue());
                    verifyNoInteractions(mapper);
                }
                verifyUserInfoCalled(token);
            }

            @Test
            void givenValidTokenWithoutMapping_thenSetOidcTokenHeader() throws Exception {
                stubUserInfoResponse(UNMAPPED_USER_TOKEN, 200);
                when(mapper.mapToMainframeUserId(any())).thenReturn(null);

                exchangeToken(UNMAPPED_USER_TOKEN).statusCode(401)
                    .header(ApimlConstants.HEADER_OIDC_TOKEN, UNMAPPED_USER_TOKEN)
                    .body("messages[0].messageKey", equalTo(ErrorType.IDENTITY_MAPPING_FAILED.getErrorMessageKey()));
                verify(mapper).mapToMainframeUserId(argThat(source -> source instanceof OIDCAuthSource oidc
                    && UNMAPPED_USER_TOKEN.equals(oidc.getRawSource())
                    && List.of(UNMAPPED_SUBJECT).equals(oidc.getDistributedId())));
                verifyUserInfoCalled(UNMAPPED_USER_TOKEN);
            }

            @Test
            void givenUnparseableToken_thenRejectWithoutOidcTokenHeader() {
                // Origin detection rejects malformed tokens before the OIDC provider runs.
                exchangeToken(MALFORMED_TOKEN).statusCode(401)
                    .header(ApimlConstants.HEADER_OIDC_TOKEN, nullValue())
                    .body("token", nullValue());
                verifyNoInteractions(httpClient, mapper);
            }

            @Test
            void givenMatchingAudienceButUserInfoRejects_thenRejectWithoutOidcTokenHeader() throws Exception {
                stubUserInfoResponse(MATCHING_AUDIENCE_TOKEN, 401);

                exchangeToken(MATCHING_AUDIENCE_TOKEN).statusCode(401)
                    .header(ApimlConstants.HEADER_OIDC_TOKEN, nullValue())
                    .body("token", nullValue());
                verifyUserInfoCalled(MATCHING_AUDIENCE_TOKEN);
                verifyNoInteractions(mapper);
            }
        }
    }

    @Nested
    @Import(JwksConfig.class)
    @TestPropertySource(properties = "apiml.security.oidc.jwks.uri=" + JWKS_URI)
    class WhenJwksValidation {

        @Autowired
        private OIDCTokenProvider provider;

        @BeforeEach
        void verifyJwksLoadedAtStartup() {
            var jwk = TEST_KEYS.jwkSet().getJsonWebKeys().get(0);
            assertEquals(jwk.getKey(), provider.getPublicKeys().get(jwk.getKeyId()).getKey());
        }

        @Nested
        class WithoutAudienceValidation {

            @BeforeEach
            void verifyAudienceConfiguration() {
                assertNull(environment.getProperty("apiml.security.oidc.aud"));
            }

            @ParameterizedTest(name = "{0}")
            @MethodSource("org.zowe.apiml.acceptance.OIDCTokenProviderEndpointTest#audienceTokens")
            void givenAnyAudience_whenExchangingToken_thenZoweJwtIssued(String token) {
                var response = exchangeToken(token);
                response.statusCode(200)
                    .body("cookieName", equalTo(COOKIE_AUTH_NAME))
                    .body("headerName", nullValue())
                    .body("token", not(equalTo(token)))
                    .body("token", notNullValue());
                verifyNoInteractions(httpClient);
            }

            @Test
            void givenValidTokenWithoutMapping_thenSetOidcTokenHeader() {
                when(mapper.mapToMainframeUserId(any())).thenReturn(null);

                exchangeToken(UNMAPPED_USER_TOKEN).statusCode(401)
                    .header(ApimlConstants.HEADER_OIDC_TOKEN, UNMAPPED_USER_TOKEN)
                    .body("messages[0].messageKey", equalTo(ErrorType.IDENTITY_MAPPING_FAILED.getErrorMessageKey()));
                verify(mapper).mapToMainframeUserId(argThat(source -> source instanceof OIDCAuthSource oidc
                    && UNMAPPED_USER_TOKEN.equals(oidc.getRawSource())
                    && List.of(UNMAPPED_SUBJECT).equals(oidc.getDistributedId())));
                verifyNoInteractions(httpClient);
            }

            @Test
            void givenUnparseableToken_thenRejectWithoutOidcTokenHeader() {
                // Origin detection rejects malformed tokens before the OIDC provider runs.
                exchangeToken(MALFORMED_TOKEN).statusCode(401)
                    .header(ApimlConstants.HEADER_OIDC_TOKEN, nullValue())
                    .body("token", nullValue());
                verifyNoInteractions(httpClient, mapper);
            }

            @Test
            void givenInvalidSignatureWithMatchingAudience_thenRejectWithoutFallback() {
                exchangeToken(INVALID_SIGNATURE_TOKEN).statusCode(401)
                    .header(ApimlConstants.HEADER_OIDC_TOKEN, nullValue())
                    .body("token", nullValue());
                verifyNoInteractions(httpClient, mapper);
            }

            @Test
            void givenExpiredTokenWithMatchingAudience_thenRejectWithoutOidcTokenHeader() {
                // Origin detection enforces expiry before the OIDC provider runs.
                exchangeToken(EXPIRED_TOKEN).statusCode(401)
                    .header(ApimlConstants.HEADER_OIDC_TOKEN, nullValue())
                    .body("token", nullValue());
                verifyNoInteractions(httpClient, mapper);
            }

            @ParameterizedTest(name = "{0}")
            @MethodSource("org.zowe.apiml.acceptance.OIDCTokenProviderEndpointTest#unknownKeyTokens")
            void givenUnknownKey_whenUserInfoAccepts_thenZoweJwtIssued(String token) throws Exception {
                stubUserInfoResponse(token, 200);

                var response = exchangeToken(token);
                response.statusCode(200)
                    .body("cookieName", equalTo(COOKIE_AUTH_NAME))
                    .body("headerName", nullValue())
                    .body("token", not(equalTo(token)))
                    .body("token", notNullValue());
                verifyUserInfoCalled(token);
            }
        }

        @Nested
        @TestPropertySource(properties = "apiml.security.oidc.aud=" + REQUIRED_AUDIENCE)
        class WithAudienceValidation {

            @BeforeEach
            void verifyAudienceConfiguration() {
                assertEquals(REQUIRED_AUDIENCE, environment.getProperty("apiml.security.oidc.aud"));
            }

            @ParameterizedTest(name = "{0}")
            @MethodSource("org.zowe.apiml.acceptance.OIDCTokenProviderEndpointTest#audienceTokensWithExpectations")
            void givenAudience_whenExchangingToken_thenSuccessIfAudienceMatches(String token, boolean matches) {
                var response = exchangeToken(token);
                if (matches) {
                    response.statusCode(200)
                        .body("cookieName", equalTo(COOKIE_AUTH_NAME))
                        .body("headerName", nullValue())
                        .body("token", not(equalTo(token)))
                        .body("token", notNullValue());
                } else {
                    response.statusCode(401)
                        .header(ApimlConstants.HEADER_OIDC_TOKEN, nullValue())
                        .body("token", nullValue());
                    verifyNoInteractions(mapper);
                }
                verifyNoInteractions(httpClient);
            }

            @Test
            void givenValidTokenWithoutMapping_thenSetOidcTokenHeader() {
                when(mapper.mapToMainframeUserId(any())).thenReturn(null);

                exchangeToken(UNMAPPED_USER_TOKEN).statusCode(401)
                    .header(ApimlConstants.HEADER_OIDC_TOKEN, UNMAPPED_USER_TOKEN)
                    .body("messages[0].messageKey", equalTo(ErrorType.IDENTITY_MAPPING_FAILED.getErrorMessageKey()));
                verify(mapper).mapToMainframeUserId(argThat(source -> source instanceof OIDCAuthSource oidc
                    && UNMAPPED_USER_TOKEN.equals(oidc.getRawSource())
                    && List.of(UNMAPPED_SUBJECT).equals(oidc.getDistributedId())));
                verifyNoInteractions(httpClient);
            }

            @Test
            void givenUnparseableToken_thenRejectWithoutOidcToken() {
                // Origin detection rejects malformed tokens before the OIDC provider runs.
                exchangeToken(MALFORMED_TOKEN).statusCode(401)
                    .header(ApimlConstants.HEADER_OIDC_TOKEN, nullValue())
                    .body("token", nullValue());
                verifyNoInteractions(httpClient, mapper);
            }

            @Test
            void givenInvalidSignatureWithMatchingAudience_thenRejectWithoutFallback() {
                exchangeToken(INVALID_SIGNATURE_TOKEN).statusCode(401)
                    .header(ApimlConstants.HEADER_OIDC_TOKEN, nullValue())
                    .body("token", nullValue());
                verifyNoInteractions(httpClient, mapper);
            }

            @Test
            void givenExpiredTokenWithMatchingAudience_thenRejectWithoutOidcToken() {
                // Origin detection enforces expiry before the OIDC provider runs.
                exchangeToken(EXPIRED_TOKEN).statusCode(401)
                    .header(ApimlConstants.HEADER_OIDC_TOKEN, nullValue())
                    .body("token", nullValue());
                verifyNoInteractions(httpClient, mapper);
            }

            @ParameterizedTest(name = "{0}")
            @MethodSource("org.zowe.apiml.acceptance.OIDCTokenProviderEndpointTest#unknownKeyTokensWithExpectations")
            void givenUnknownKey_whenUserInfoAccepts_thenApplyAudienceCheck(String token, boolean matches) throws Exception {
                stubUserInfoResponse(token, 200);

                var response = exchangeToken(token);
                if (matches) {
                    response.statusCode(200)
                        .body("cookieName", equalTo(COOKIE_AUTH_NAME))
                        .body("headerName", nullValue())
                        .body("token", not(equalTo(token)))
                        .body("token", notNullValue());
                } else {
                    response.statusCode(401)
                        .header(ApimlConstants.HEADER_OIDC_TOKEN, nullValue())
                        .body("token", nullValue());
                    verifyNoInteractions(mapper);
                }
                verifyUserInfoCalled(token);
            }
        }

        @Nested
        @TestPropertySource(properties = "apiml.security.oidc.userInfo.uri=")
        class WithoutUserInfoFallback {

            @BeforeEach
            void verifyUserInfoEndpointNotConfigured() {
                assertEquals("", environment.getProperty("apiml.security.oidc.userInfo.uri"));
            }

            @Nested
            class WithoutAudienceValidation {

                @BeforeEach
                void verifyAudienceConfiguration() {
                    assertNull(environment.getProperty("apiml.security.oidc.aud"));
                }

                @ParameterizedTest(name = "{0}")
                @MethodSource("org.zowe.apiml.acceptance.OIDCTokenProviderEndpointTest#audienceTokens")
                void givenKnownKey_whenExchangingToken_thenZoweJwtIssued(String token) {
                    exchangeToken(token).statusCode(200)
                        .body("cookieName", equalTo(COOKIE_AUTH_NAME))
                        .body("headerName", nullValue())
                        .body("token", not(equalTo(token)))
                        .body("token", notNullValue());
                    verifyNoInteractions(httpClient);
                }

                @ParameterizedTest(name = "{0}")
                @MethodSource("org.zowe.apiml.acceptance.OIDCTokenProviderEndpointTest#unknownKeyTokens")
                void givenUnknownKey_whenUserInfoNotConfigured_thenRejectWithoutHttpCall(String token) {
                    exchangeToken(token).statusCode(401)
                        .header(ApimlConstants.HEADER_OIDC_TOKEN, nullValue())
                        .body("token", nullValue());
                    verifyNoInteractions(httpClient, mapper);
                }
            }

            @Nested
            @TestPropertySource(properties = "apiml.security.oidc.aud=" + REQUIRED_AUDIENCE)
            class WithAudienceValidation {

                @BeforeEach
                void verifyAudienceConfiguration() {
                    assertEquals(REQUIRED_AUDIENCE, environment.getProperty("apiml.security.oidc.aud"));
                }

                @ParameterizedTest(name = "{0}")
                @MethodSource("org.zowe.apiml.acceptance.OIDCTokenProviderEndpointTest#audienceTokensWithExpectations")
                void givenKnownKey_whenExchangingToken_thenSuccessIfAudienceMatches(String token, boolean matches) {
                    var response = exchangeToken(token);
                    if (matches) {
                        response.statusCode(200)
                            .body("cookieName", equalTo(COOKIE_AUTH_NAME))
                            .body("headerName", nullValue())
                            .body("token", not(equalTo(token)))
                            .body("token", notNullValue());
                    } else {
                        response.statusCode(401)
                            .header(ApimlConstants.HEADER_OIDC_TOKEN, nullValue())
                            .body("token", nullValue());
                        verifyNoInteractions(mapper);
                    }
                    verifyNoInteractions(httpClient);
                }

                @ParameterizedTest(name = "{0}")
                @MethodSource("org.zowe.apiml.acceptance.OIDCTokenProviderEndpointTest#unknownKeyTokens")
                void givenUnknownKey_whenUserInfoNotConfigured_thenRejectWithoutHttpCall(String token) {
                    exchangeToken(token).statusCode(401)
                        .header(ApimlConstants.HEADER_OIDC_TOKEN, nullValue())
                        .body("token", nullValue());
                    verifyNoInteractions(httpClient, mapper);
                }
            }
        }
    }

    @TestConfiguration
    static class JwksConfig {

        @Bean
        @Primary
        JWKResolver testJwkResolver() throws Exception {
            // The provider fetches keys in @PostConstruct, before any @BeforeEach runs.
            var resolver = mock(JWKResolver.class);
            when(resolver.resolve(JWKS_URI)).thenReturn(TEST_KEYS.jwkSet());
            return resolver;
        }
    }

    static Stream<Arguments> audienceTokensWithExpectations() {
        return Stream.of(
            Arguments.of(Named.of("matching audience", MATCHING_AUDIENCE_TOKEN), true),
            Arguments.of(Named.of("matching audience in array", MATCHING_AUDIENCE_ARRAY_TOKEN), true),
            Arguments.of(Named.of("missing audience", MISSING_AUDIENCE_TOKEN), false),
            Arguments.of(Named.of("mismatched audience", MISMATCHED_AUDIENCE_TOKEN), false),
            Arguments.of(Named.of("mismatched audience array", MISMATCHED_AUDIENCE_ARRAY_TOKEN), false)
        );
    }

    static Stream<Arguments> audienceTokens() {
        return audienceTokensWithExpectations().map(arguments -> Arguments.of(arguments.get()[0]));
    }

    static Stream<Arguments> unknownKeyTokensWithExpectations() {
        return Stream.of(
            Arguments.of(Named.of("matching audience", UNKNOWN_KEY_MATCHING_AUDIENCE_TOKEN), true),
            Arguments.of(Named.of("matching audience in array", UNKNOWN_KEY_MATCHING_AUDIENCE_ARRAY_TOKEN), true),
            Arguments.of(Named.of("missing audience", UNKNOWN_KEY_MISSING_AUDIENCE_TOKEN), false),
            Arguments.of(Named.of("mismatched audience", UNKNOWN_KEY_MISMATCHED_AUDIENCE_TOKEN), false),
            Arguments.of(Named.of("mismatched audience array", UNKNOWN_KEY_MISMATCHED_AUDIENCE_ARRAY_TOKEN), false)
        );
    }

    static Stream<Arguments> unknownKeyTokens() {
        return unknownKeyTokensWithExpectations().map(arguments -> Arguments.of(arguments.get()[0]));
    }

    private static String signToken(Object audience, String subject, String keyId, Instant expiration, PrivateKey privateKey) throws Exception {
        var payload = new JWTClaimsSet.Builder()
            .issuer("https://oidc.provider.com")
            .subject(subject)
            .issueTime(Date.from(Instant.now().minusSeconds(60)))
            .expirationTime(Date.from(expiration))
            .build().toJSONObject();
        if (audience != null) {
            // Keep string and array claims distinct in the serialized token.
            payload.put("aud", audience);
        }
        var token = new JWSObject(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(keyId).build(), new Payload(payload));
        token.sign(new RSASSASigner(privateKey));
        return token.serialize();
    }

    private void stubUserInfoResponse(String token, int statusCode) {
        userInfoResponses.put(ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + token, statusCode);
    }

    private void verifyUserInfoCalled(String token) throws Exception {
        var requests = ArgumentCaptor.forClass(HttpGet.class);
        verify(httpClient, atLeastOnce()).execute(requests.capture(), any(HttpClientResponseHandler.class));
        for (HttpGet request : requests.getAllValues()) {
            assertEquals(USER_INFO_URI, request.getUri().toString());
            assertEquals(ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + token,
                request.getFirstHeader(HttpHeaders.AUTHORIZATION).getValue());
        }
    }

    private ValidatableResponse exchangeToken(String token) {
        return RestAssured.given().config(withClientCert)
            .header(HttpHeaders.AUTHORIZATION, ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + token)
            .post("https://localhost:" + port + CONTROLLER_PATH + "/zoweJwt")
            .then();
    }
}
