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

import io.jsonwebtoken.Jwts;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.http.HttpHeaders;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.lang.JoseException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.zaas.cache.CachingServiceClientException;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.zowe.apiml.zaas.utils.JWTUtils.loadPrivateKey;

@ExtendWith(MockitoExtension.class)
class OIDCTokenProviderTest {

    private static final String OKTA_JWKS_RESOURCE = "test_samples/okta_jwks.json";
    private static final String EXPIRED_TOKEN = "eyJraWQiOiJMY3hja2tvcjk0cWtydW54SFA3VGtpYjU0N3J6bWtYdnNZVi1uYzZVLU40IiwiYWxnIjoiUlMyNTYifQ.eyJ2ZXIiOjEsImp0aSI6IkFULlExakp2UkZ0dUhFUFpGTXNmM3A0enQ5aHBRRHZrSU1CQ3RneU9IcTdlaEkiLCJpc3MiOiJodHRwczovL2Rldi05NTcyNzY4Ni5va3RhLmNvbS9vYXV0aDIvZGVmYXVsdCIsImF1ZCI6ImFwaTovL2RlZmF1bHQiLCJpYXQiOjE2OTcwNjA3NzMsImV4cCI6MTY5NzA2NDM3MywiY2lkIjoiMG9hNmE0OG1uaVhBcUVNcng1ZDciLCJ1aWQiOiIwMHU5OTExOGgxNmtQT1dBbTVkNyIsInNjcCI6WyJvcGVuaWQiXSwiYXV0aF90aW1lIjoxNjk3MDYwMDY0LCJzdWIiOiJzajg5NTA5MkBicm9hZGNvbS5uZXQiLCJncm91cHMiOlsiRXZlcnlvbmUiXX0.Cuf1JVq_NnfBxaCwiLsR5O6DBmVV1fj9utAfKWIF1hlek2hCJsDLQM4ii_ucQ0MM1V3nVE1ZatPB-W7ImWPlGz7NeNBv7jEV9DkX70hchCjPHyYpaUhAieTG75obdufiFpI55bz3qH5cPRvsKv0OKKI9T8D7GjEWsOhv6CevJJZZvgCFLGFfnacKLOY5fEBN82bdmCulNfPVrXF23rOregFjOBJ1cKWfjmB0UGWgI8VBGGemMNm3ACX3OYpTOek2PBfoCIZWOSGnLZumFTYA0F_3DsWYhIJNoFv16_EBBJcp_C0BYE_fiuXzeB0fieNUXASsKp591XJMflDQS_Zt1g";
    private static final String MALFORMED_TOKEN = "token";

    private static String VALID_TOKEN;
    private static JsonWebKeySet localJwkSet;

    private OIDCTokenProvider oidcTokenProvider;

    @Mock
    private CloseableHttpClient httpClient;
    @Mock
    private JWKResolver jwkResolver;

    static Stream<String> invalidTokens() {
        return Stream.of(
            EXPIRED_TOKEN, MALFORMED_TOKEN, "", null
        );
    }

    @BeforeAll
    static void init() throws Exception {
        var now = Instant.now();
        var jwkAndSet = loadPrivateKey("../keystore/localhost/localhost.keystore.p12", "localhost", "password");
        localJwkSet = jwkAndSet.jwkSet();
        VALID_TOKEN = Jwts.builder()
            .header().keyId("Lcxckkor94qkrunxHP7Tkib547rzmkXvsYV-nc6U-N4").and()
            .subject("user")
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(1200)))
            .issuer("API ML")
            .id(UUID.randomUUID().toString())
            .signWith(jwkAndSet.privateKey()).compact();
    }

    @BeforeEach
    void setup() throws CachingServiceClientException, IOException {
        oidcTokenProvider = new OIDCTokenProvider(Clock.systemUTC(), jwkResolver, httpClient);
        ReflectionTestUtils.setField(oidcTokenProvider, "jwkRefreshInterval", 1);
    }

    @Nested
    class GivenInitializationWithJwks {

        @Test
        void whenUriNotProvided_thenNotInitialized() throws Exception {
            ReflectionTestUtils.setField(oidcTokenProvider, "jwksUri", Collections.emptyList());
            oidcTokenProvider.afterPropertiesSet();
        }

        @Test
        void shouldNotModifyJwksUri() throws IOException {
            assertDoesNotThrow(() -> oidcTokenProvider.fetchJWKSet());
            assertTrue(oidcTokenProvider.getPublicKeys().isEmpty());
        }

    }

    @Nested
    class GivenCorrectConfiguration {

        @Nested
        class WhenJWKValidation {

            @BeforeEach
            void init() throws Exception {
                ReflectionTestUtils.setField(oidcTokenProvider, "jwksUri", Arrays.asList("https://localjwk", "https://jwksurl"));
            }

            @ParameterizedTest(name = "#{index} return invalid when given invalid token: {0}")
            @MethodSource("org.zowe.apiml.zaas.security.service.token.OIDCTokenProviderTest#invalidTokens")
            void whenInvalidToken_thenReturnInvalid(String token) throws JoseException, IOException {
                when(jwkResolver.resolve("https://localjwk")).thenReturn(localJwkSet);
                when(jwkResolver.resolve("https://jwksurl")).thenReturn(localJwkSet);
                assertFalse(oidcTokenProvider.isValid(token));
            }

            @Test
            void whenValidToken_thenReturnValid() throws JoseException, IOException {
                when(jwkResolver.resolve("https://localjwk")).thenReturn(localJwkSet);
                when(jwkResolver.resolve("https://jwksurl")).thenReturn(localJwkSet);
                assertTrue(oidcTokenProvider.isValid(VALID_TOKEN));
            }

        }

        @Nested
        class WhenEndpointValidation {


            @BeforeEach
            void init() throws Exception {
                ReflectionTestUtils.setField(oidcTokenProvider, "endpointUrl", "https://entra.com");
                var httpGet = new HttpGet("https://entra.com");
                httpGet.addHeader(HttpHeaders.AUTHORIZATION, ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + VALID_TOKEN);
                try (var mockResponse = mock(ClassicHttpResponse.class)) {

                    when(httpClient.execute(any(HttpGet.class), any(HttpClientResponseHandler.class)))
                        .thenAnswer(invocation -> {
                            HttpGet get = invocation.getArgument(0);
                            if (get.getHeader(HttpHeaders.AUTHORIZATION).getValue().equals(ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + VALID_TOKEN)) {
                                when(mockResponse.getCode()).thenReturn(200);
                            } else {
                                when(mockResponse.getCode()).thenReturn(401);
                            }
                            HttpClientResponseHandler<?> handler = invocation.getArgument(1);
                            return handler.handleResponse(mockResponse);
                        });
                }
            }

            @ParameterizedTest(name = "#{index} return invalid when given invalid token: {0}")
            @MethodSource("org.zowe.apiml.zaas.security.service.token.OIDCTokenProviderTest#invalidTokens")
            void whenInvalidToken_thenReturnInvalid(String token) {
                assertFalse(oidcTokenProvider.isValid(token));
            }

            @Test
            void whenValidToken_thenReturnValid() {
                assertTrue(oidcTokenProvider.isValid(VALID_TOKEN));
            }

        }
    }

}
