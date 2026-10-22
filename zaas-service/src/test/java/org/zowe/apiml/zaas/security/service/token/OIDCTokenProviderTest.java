/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service.token;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import lombok.SneakyThrows;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.lang.JoseException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.zaas.cache.CachingServiceClientException;

import java.io.IOException;
import java.security.PrivateKey;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.zowe.apiml.security.common.util.JWTTestUtils.loadPrivateKey;

@ExtendWith(MockitoExtension.class)
class OIDCTokenProviderTest {

    private static final String INVALID_SIGNATURE_TOKEN = "eyJraWQiOiJMY3hja2tvcjk0cWtydW54SFA3VGtpYjU0N3J6bWtYdnNZVi1uYzZVLU40IiwiYWxnIjoiUlMyNTYifQ.eyJ2ZXIiOjEsImp0aSI6IkFULlExakp2UkZ0dUhFUFpGTXNmM3A0enQ5aHBRRHZrSU1CQ3RneU9IcTdlaEkiLCJpc3MiOiJodHRwczovL2Rldi05NTcyNzY4Ni5va3RhLmNvbS9vYXV0aDIvZGVmYXVsdCIsImF1ZCI6ImFwaTovL2RlZmF1bHQiLCJpYXQiOjE2OTcwNjA3NzMsImV4cCI6MTY5NzA2NDM3MywiY2lkIjoiMG9hNmE0OG1uaVhBcUVNcng1ZDciLCJ1aWQiOiIwMHU5OTExOGgxNmtQT1dBbTVkNyIsInNjcCI6WyJvcGVuaWQiXSwiYXV0aF90aW1lIjoxNjk3MDYwMDY0LCJzdWIiOiJzajg5NTA5MkBicm9hZGNvbS5uZXQiLCJncm91cHMiOlsiRXZlcnlvbmUiXX0.Cuf1JVq_NnfBxaCwiLsR5O6DBmVV1fj9utAfKWIF1hlek2hCJsDLQM4ii_ucQ0MM1V3nVE1ZatPB-W7ImWPlGz7NeNBv7jEV9DkX70hchCjPHyYpaUhAieTG75obdufiFpI55bz3qH5cPRvsKv0OKKI9T8D7GjEWsOhv6CevJJZZvgCFLGFfnacKLOY5fEBN82bdmCulNfPVrXF23rOregFjOBJ1cKWfjmB0UGWgI8VBGGemMNm3ACX3OYpTOek2PBfoCIZWOSGnLZumFTYA0F_3DsWYhIJNoFv16_EBBJcp_C0BYE_fiuXzeB0fieNUXASsKp591XJMflDQS_Zt1g";
    private static final String MALFORMED_TOKEN = "token";

    private static final String KEY_ID = "Lcxckkor94qkrunxHP7Tkib547rzmkXvsYV-nc6U-N4";

    private static String VALID_TOKEN;
    private static final String REQUIRED_AUDIENCE = "APIML-audience";
    private static final String USER_INFO_URI = "https://entra.com/userinfo";

    private static PrivateKey privateKey;
    private static JsonWebKeySet localJwkSet;

    private OIDCTokenProvider oidcTokenProvider;

    @Mock
    private CloseableHttpClient httpClient;
    @Mock
    private JWKResolver jwkResolver;

    static Stream<String> invalidTokens() {
        return Stream.of(
            INVALID_SIGNATURE_TOKEN, MALFORMED_TOKEN, "", null
        );
    }

    static Stream<Arguments> audienceValidationParams() {
        return Stream.of(
            //required audience, provided audience, result
            Arguments.of("", null, true),
            Arguments.of(null, "other", true),
            Arguments.of("", List.of("other", "another"), true),
            Arguments.of(REQUIRED_AUDIENCE, REQUIRED_AUDIENCE, true),
            Arguments.of(REQUIRED_AUDIENCE, Set.of("other", REQUIRED_AUDIENCE), true),
            Arguments.of(REQUIRED_AUDIENCE, null, false),
            Arguments.of(REQUIRED_AUDIENCE, "", false),
            Arguments.of(REQUIRED_AUDIENCE, Set.of(), false),
            Arguments.of(REQUIRED_AUDIENCE, "other", false),
            Arguments.of(REQUIRED_AUDIENCE, Set.of("other", "another"), false),
            Arguments.of(REQUIRED_AUDIENCE, "apiml-audience", false),
            Arguments.of(REQUIRED_AUDIENCE, REQUIRED_AUDIENCE + "other", false)
        );
    }

    private static JwtBuilder tokenWithAudience(Object audience) {
        var now = Instant.now();
        var builder = Jwts.builder()
            .header().keyId(KEY_ID).and()
            .subject("user")
            .issuedAt(Date.from(now.minusSeconds(300)))
            .expiration(Date.from(now.plusSeconds(1200)))
            .issuer("API ML")
            .id(UUID.randomUUID().toString());

        if (audience instanceof String aud) {
            builder.audience().add(aud).and();
        } else if (audience instanceof Collection<?> aud) {
            builder.audience().add((Collection<String>) aud).and(); // NOSONAR
        }
        return builder;
    }

    private static String createTokenWithAudience(Object audience) {
        return tokenWithAudience(audience).signWith(privateKey).compact();
    }

    private static String createExpiredToken(Object audience) {
        // A valid signature and known kid ensure expiration triggers the fallback.
        return tokenWithAudience(audience)
            .expiration(Date.from(Instant.now().minusSeconds(60)))
            .signWith(privateKey).compact();
    }

    private static String createUnsignedToken(Object audience) {
        return tokenWithAudience(audience).header().keyId(null).and().compact();
    }

    private static String createTokenWithoutKeyId(Object audience) {
        return tokenWithAudience(audience).header().keyId(null).and()
            .signWith(privateKey).compact();
    }

    @SneakyThrows
    private void mockOIDCEndpointResponse(int status) {
        var mockResponse = mock(ClassicHttpResponse.class);
        when(mockResponse.getCode()).thenReturn(status);
        when(httpClient.execute(any(HttpGet.class), any(HttpClientResponseHandler.class)))
            .thenAnswer(invocation -> {
                HttpClientResponseHandler<?> handler = invocation.getArgument(1);
                return handler.handleResponse(mockResponse);
            });
    }

    private void verifyUserInfoCalled(String token) throws Exception {
        var request = ArgumentCaptor.forClass(HttpGet.class);
        verify(httpClient).execute(request.capture(), any(HttpClientResponseHandler.class));
        assertEquals(USER_INFO_URI, request.getValue().getUri().toString());
        assertEquals(ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + token,
            request.getValue().getFirstHeader(HttpHeaders.AUTHORIZATION).getValue());
    }

    private void assertUserInfoIOExceptionRejected(String token) throws Exception {
        when(httpClient.execute(any(HttpGet.class), any(HttpClientResponseHandler.class)))
            .thenThrow(new IOException("UserInfo unavailable"));
        assertFalse(assertDoesNotThrow(() -> oidcTokenProvider.isValid(token)));
        verifyUserInfoCalled(token);
    }

    @BeforeAll
    static void init() throws Exception {
        var now = Instant.now();
        var jwkAndSet = loadPrivateKey("../keystore/service/service.keystore.p12", "localhost", "password");
        localJwkSet = jwkAndSet.jwkSet();
        privateKey = jwkAndSet.privateKey();
        VALID_TOKEN = Jwts.builder()
            .header().keyId(KEY_ID).and()
            .subject("user")
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(1200)))
            .issuer("API ML")
            .id(UUID.randomUUID().toString())
            .signWith(jwkAndSet.privateKey()).compact();
    }

    @BeforeEach
    void setup() throws CachingServiceClientException {
        oidcTokenProvider = new OIDCTokenProvider(Clock.systemUTC(), jwkResolver, httpClient);
        ReflectionTestUtils.setField(oidcTokenProvider, "jwkRefreshInterval", 1);
    }

    @AfterEach
    void cleanup() {
        ReflectionTestUtils.setField(oidcTokenProvider, "requiredAudience", null);
    }

    @Nested
    class GivenInitializationWithJwks {

        @Test
        void whenUriNotProvided_thenNotInitialized() throws Exception {
            ReflectionTestUtils.setField(oidcTokenProvider, "jwksUri", Collections.emptyList());
            oidcTokenProvider.afterPropertiesSet();
            verify(jwkResolver, times(0)).resolve(any());
        }

        @Test
        void shouldNotModifyJwksUri() {
            assertDoesNotThrow(() -> oidcTokenProvider.fetchJWKSet());
            assertTrue(oidcTokenProvider.getPublicKeys().isEmpty());
        }

    }

    @Nested
    class GivenCorrectConfiguration {

        @Nested
        class WhenJWKValidation {

            @BeforeEach
            void init() {
                ReflectionTestUtils.setField(oidcTokenProvider, "jwksUri", Arrays.asList("https://localjwk", "https://jwksurl"));
                // A usable endpoint makes an accidental fallback observable, besides it is needed for fallback tests.
                ReflectionTestUtils.setField(oidcTokenProvider, "endpointUrl", USER_INFO_URI);
            }

            @ParameterizedTest(name = "#{index} return invalid when given invalid token: {0}")
            @MethodSource("org.zowe.apiml.zaas.security.service.token.OIDCTokenProviderTest#invalidTokens")
            void whenInvalidToken_thenReturnInvalid(String token) throws JoseException, IOException {
                when(jwkResolver.resolve("https://localjwk")).thenReturn(localJwkSet);
                when(jwkResolver.resolve("https://jwksurl")).thenReturn(localJwkSet);
                oidcTokenProvider.fetchJWKSet();
                assertFalse(oidcTokenProvider.isValid(token));
                verifyNoInteractions(httpClient);
            }

            @ParameterizedTest(name = "Audience required: {0}, provided: {1}, validation result: {2}")
            @MethodSource("org.zowe.apiml.zaas.security.service.token.OIDCTokenProviderTest#audienceValidationParams")
            void whenValidTokenWithAudience(String required, Object provided, boolean expectedResult) throws JoseException, IOException {
                ReflectionTestUtils.setField(oidcTokenProvider, "requiredAudience", required);
                when(jwkResolver.resolve("https://localjwk")).thenReturn(localJwkSet);
                when(jwkResolver.resolve("https://jwksurl")).thenReturn(localJwkSet);
                assertEquals(expectedResult, oidcTokenProvider.isValid(createTokenWithAudience(provided)));
                verifyNoInteractions(httpClient);
            }

            @Nested
            class WithoutUserInfoEndpoint {

                @BeforeEach
                void configureJwksOnly() throws Exception {
                    ReflectionTestUtils.setField(oidcTokenProvider, "endpointUrl", "");
                    when(jwkResolver.resolve("https://localjwk")).thenReturn(localJwkSet);
                    when(jwkResolver.resolve("https://jwksurl")).thenReturn(localJwkSet);
                    oidcTokenProvider.fetchJWKSet();
                }

                @ParameterizedTest(name = "Required audience: {0}")
                @ValueSource(strings = {"", REQUIRED_AUDIENCE})
                void whenUnsignedToken_thenRejectWithoutHttpCall(String requiredAudience) {
                    ReflectionTestUtils.setField(oidcTokenProvider, "requiredAudience", requiredAudience);
                    String token = createUnsignedToken(REQUIRED_AUDIENCE);

                    assertFalse(oidcTokenProvider.isValid(token));
                    verifyNoInteractions(httpClient);
                }

                @ParameterizedTest(name = "Required audience: {0}")
                @ValueSource(strings = {"", REQUIRED_AUDIENCE})
                void whenTokenWithoutKeyId_thenRejectWithoutHttpCall(String requiredAudience) {
                    ReflectionTestUtils.setField(oidcTokenProvider, "requiredAudience", requiredAudience);
                    String token = createTokenWithoutKeyId(REQUIRED_AUDIENCE);

                    assertFalse(oidcTokenProvider.isValid(token));
                    verifyNoInteractions(httpClient);
                }
            }

            @Nested
            class WhenUserInfoEndpointFallback {

                @BeforeEach
                void loadKeys() throws Exception {
                    when(jwkResolver.resolve("https://localjwk")).thenReturn(localJwkSet);
                    when(jwkResolver.resolve("https://jwksurl")).thenReturn(localJwkSet);
                    oidcTokenProvider.fetchJWKSet();
                }

                @ParameterizedTest(name = "Audience required: {0}, provided: {1}, validation result: {2}")
                @MethodSource("org.zowe.apiml.zaas.security.service.token.OIDCTokenProviderTest#audienceValidationParams")
                void whenExpiredToken_thenFallbackAndCheckAudience(String required, Object provided, boolean expectedResult) throws Exception {
                    assertFallbackAudience(createExpiredToken(provided), required, expectedResult);
                }

                @Test
                void whenExpiredTokenAndUserInfoRejects_thenReturnInvalid() throws Exception {
                    ReflectionTestUtils.setField(oidcTokenProvider, "requiredAudience", REQUIRED_AUDIENCE);
                    String token = createExpiredToken(REQUIRED_AUDIENCE);
                    mockOIDCEndpointResponse(HttpStatus.SC_UNAUTHORIZED);

                    assertFalse(oidcTokenProvider.isValid(token));
                    verifyUserInfoCalled(token);
                }

                @ParameterizedTest(name = "Audience required: {0}, provided: {1}, validation result: {2}")
                @MethodSource("org.zowe.apiml.zaas.security.service.token.OIDCTokenProviderTest#audienceValidationParams")
                void whenUnsignedToken_thenFallbackAndCheckAudience(String required, Object provided, boolean expectedResult) throws Exception {
                    assertFallbackAudience(createUnsignedToken(provided), required, expectedResult);
                }

                @ParameterizedTest(name = "Audience required: {0}, provided: {1}, validation result: {2}")
                @MethodSource("org.zowe.apiml.zaas.security.service.token.OIDCTokenProviderTest#audienceValidationParams")
                void whenTokenWithoutKeyId_thenFallbackAndCheckAudience(String required, Object provided, boolean expectedResult) throws Exception {
                    assertFallbackAudience(createTokenWithoutKeyId(provided), required, expectedResult);
                }

                @Test
                void whenUserInfoThrowsIOException_thenRejectWithoutThrowing() throws Exception {
                    ReflectionTestUtils.setField(oidcTokenProvider, "requiredAudience", REQUIRED_AUDIENCE);
                    assertUserInfoIOExceptionRejected(createTokenWithoutKeyId(REQUIRED_AUDIENCE));
                }

                private void assertFallbackAudience(String token, String required, boolean expectedResult) throws Exception {
                    ReflectionTestUtils.setField(oidcTokenProvider, "requiredAudience", required);
                    mockOIDCEndpointResponse(HttpStatus.SC_OK);
                    assertEquals(expectedResult, oidcTokenProvider.isValid(token));
                    verifyUserInfoCalled(token);
                }
            }
        }

        @Nested
        class WhenEndpointValidation {

            @BeforeEach
            void init() {
                ReflectionTestUtils.setField(oidcTokenProvider, "endpointUrl", USER_INFO_URI);
            }

            @Test
            void whenUserInfoThrowsIOException_thenRejectWithoutThrowing() throws Exception {
                ReflectionTestUtils.setField(oidcTokenProvider, "requiredAudience", REQUIRED_AUDIENCE);
                assertUserInfoIOExceptionRejected(createTokenWithAudience(REQUIRED_AUDIENCE));
            }

            @ParameterizedTest(name = "#{index} return invalid when given invalid token: {0}")
            @MethodSource("org.zowe.apiml.zaas.security.service.token.OIDCTokenProviderTest#invalidTokens")
            void whenInvalidToken_thenReturnInvalid(String token) {
                mockOIDCEndpointResponse(HttpStatus.SC_UNAUTHORIZED);
                assertFalse(oidcTokenProvider.isValid(token));
            }

            @Test
            void whenValidToken_thenReturnValid() {
                mockOIDCEndpointResponse(HttpStatus.SC_OK);
                assertTrue(oidcTokenProvider.isValid(VALID_TOKEN));
            }

            @ParameterizedTest(name = "Audience required: {0}, provided: {1}, validation result: {2}")
            @MethodSource("org.zowe.apiml.zaas.security.service.token.OIDCTokenProviderTest#audienceValidationParams")
            void whenValidTokenWithAudience(String required, Object provided, boolean expectedResult) {
                ReflectionTestUtils.setField(oidcTokenProvider, "requiredAudience", required);
                mockOIDCEndpointResponse(HttpStatus.SC_OK);
                assertEquals(expectedResult, oidcTokenProvider.isValid(createTokenWithAudience(provided)));
            }
        }
    }
}
