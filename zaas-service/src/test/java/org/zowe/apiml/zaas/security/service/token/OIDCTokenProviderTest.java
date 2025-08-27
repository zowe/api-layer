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

import com.google.common.io.Resources;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jose.util.Resource;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.impl.DefaultClock;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.http.HttpHeaders;
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

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OIDCTokenProviderTest {

    private static final String OKTA_JWKS_RESOURCE = "test_samples/okta_jwks.json";

    private static final String EXPIRED_TOKEN = "eyJraWQiOiJMY3hja2tvcjk0cWtydW54SFA3VGtpYjU0N3J6bWtYdnNZVi1uYzZVLU40IiwiYWxnIjoiUlMyNTYifQ.eyJ2ZXIiOjEsImp0aSI6IkFULlExakp2UkZ0dUhFUFpGTXNmM3A0enQ5aHBRRHZrSU1CQ3RneU9IcTdlaEkiLCJpc3MiOiJodHRwczovL2Rldi05NTcyNzY4Ni5va3RhLmNvbS9vYXV0aDIvZGVmYXVsdCIsImF1ZCI6ImFwaTovL2RlZmF1bHQiLCJpYXQiOjE2OTcwNjA3NzMsImV4cCI6MTY5NzA2NDM3MywiY2lkIjoiMG9hNmE0OG1uaVhBcUVNcng1ZDciLCJ1aWQiOiIwMHU5OTExOGgxNmtQT1dBbTVkNyIsInNjcCI6WyJvcGVuaWQiXSwiYXV0aF90aW1lIjoxNjk3MDYwMDY0LCJzdWIiOiJzajg5NTA5MkBicm9hZGNvbS5uZXQiLCJncm91cHMiOlsiRXZlcnlvbmUiXX0.Cuf1JVq_NnfBxaCwiLsR5O6DBmVV1fj9utAfKWIF1hlek2hCJsDLQM4ii_ucQ0MM1V3nVE1ZatPB-W7ImWPlGz7NeNBv7jEV9DkX70hchCjPHyYpaUhAieTG75obdufiFpI55bz3qH5cPRvsKv0OKKI9T8D7GjEWsOhv6CevJJZZvgCFLGFfnacKLOY5fEBN82bdmCulNfPVrXF23rOregFjOBJ1cKWfjmB0UGWgI8VBGGemMNm3ACX3OYpTOek2PBfoCIZWOSGnLZumFTYA0F_3DsWYhIJNoFv16_EBBJcp_C0BYE_fiuXzeB0fieNUXASsKp591XJMflDQS_Zt1g";

    private static String VALID_TOKEN;
    private static final String MALFORMED_TOKEN = "token";
    private static JWKSet localJwkSet;
    private static String oktaJwks;

    private OIDCTokenProvider oidcTokenProvider;

    @Mock
    private DefaultResourceRetriever resourceRetriever;
    @Mock
    private CloseableHttpClient httpClient;

    static Stream<String> invalidTokens() {
        return Stream.of(
            EXPIRED_TOKEN, MALFORMED_TOKEN, "", null
        );
    }

    @BeforeAll
    static void init() throws Exception {
        var now = Instant.now();
        var pKey = loadPrivateKey("../keystore/localhost/localhost.keystore.p12", "localhost", "password");
        VALID_TOKEN = Jwts.builder()
            .header().keyId("0987").and()
            .subject("user")
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(1200)))
            .issuer("API ML")
            .id(UUID.randomUUID().toString())
            .signWith(pKey, Jwts.SIG.RS256).compact();
    }

    static PrivateKey loadPrivateKey(String path, String alias, String password) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(path)) {
            ks.load(fis, password.toCharArray());
        }
        Key key = ks.getKey(alias, password.toCharArray());
        var cert = ks.getCertificate(alias);
        var pubKey = cert.getPublicKey();
        if (pubKey instanceof RSAPublicKey rsaPublicKey) {
            var k = new RSAKey.Builder(rsaPublicKey).keyID("0987").build().toPublicJWK();
            localJwkSet = new JWKSet(k);
        }

        return (PrivateKey) key;
    }

    @BeforeEach
    void setup() throws CachingServiceClientException, IOException {
        oidcTokenProvider = new OIDCTokenProvider(new DefaultClock(), resourceRetriever, httpClient);
        ReflectionTestUtils.setField(oidcTokenProvider, "jwkRefreshInterval", 1);
        oktaJwks = Resources.toString(Resources.getResource(OKTA_JWKS_RESOURCE), StandardCharsets.UTF_8);
    }

    @Nested
    class GivenInitializationWithJwks {

        @Test
        void initialized_thenJwksFullfilled() throws Exception {
            ReflectionTestUtils.setField(oidcTokenProvider, "jwksUri", Arrays.asList("https://jwksurl"));
            when(resourceRetriever.retrieveResource(eq(new URL("https://jwksurl")))).thenReturn(new Resource(oktaJwks, null));
            oidcTokenProvider.afterPropertiesSet();
            Map<String, PublicKey> publicKeys = oidcTokenProvider.getPublicKeys();

            assertFalse(publicKeys.isEmpty());
            assertTrue(publicKeys.containsKey("Lcxckkor94qkrunxHP7Tkib547rzmkXvsYV-nc6U-N4"));
            assertTrue(publicKeys.containsKey("-716sp3XBB_v30lGj2mu5MdXkdh8poa9zJQlAwC46n4"));
            assertNotNull(publicKeys.get("Lcxckkor94qkrunxHP7Tkib547rzmkXvsYV-nc6U-N4"));
            assertInstanceOf(Key.class, publicKeys.get("Lcxckkor94qkrunxHP7Tkib547rzmkXvsYV-nc6U-N4"));
        }

        @Test
        void whenUriNotProvided_thenNotInitialized() throws Exception {
            ReflectionTestUtils.setField(oidcTokenProvider, "jwksUri", Collections.emptyList());
            oidcTokenProvider.afterPropertiesSet();
            verify(resourceRetriever, times(0)).retrieveResource(any());
        }
    }

    @Nested
    class GivenCorrectConfiguration {


        @Nested
        class WhenJWKValidation {

            @BeforeEach
            void init() throws Exception {
                ReflectionTestUtils.setField(oidcTokenProvider, "jwksUri", Arrays.asList("https://jwksurl", "https://localjwk"));
                when(resourceRetriever.retrieveResource(eq(new URL("https://jwksurl")))).thenReturn(new Resource(oktaJwks, null));
                when(resourceRetriever.retrieveResource(eq(new URL("https://localjwk")))).thenReturn(new Resource(localJwkSet.toString(), null));
            }

            @ParameterizedTest(name = "#{index} return invalid when given invalid token: {0}")
            @MethodSource("org.zowe.apiml.zaas.security.service.token.OIDCTokenProviderTest#invalidTokens")
            void whenInvalidToken_thenReturnInvalid(String token) {
                assertFalse(oidcTokenProvider.isValid(token));
            }

            @Test
            void whenValidToken_thenReturnValid() {
                assumeTrue(oidcTokenProvider.isValid(VALID_TOKEN));
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
                assumeTrue(oidcTokenProvider.isValid(VALID_TOKEN));
            }

        }
    }


    @Nested
    class JwksUriLoad {

        @BeforeEach
        public void setUp() {
            oidcTokenProvider = new OIDCTokenProvider(new DefaultClock(), resourceRetriever, httpClient);
            ReflectionTestUtils.setField(oidcTokenProvider, "jwksUri", Arrays.asList("https://jwksurl"));
            ReflectionTestUtils.setField(oidcTokenProvider, "resourceRetriever", resourceRetriever);
        }

        @Test
        void shouldNotModifyJwksUri() throws IOException {
            var json = "{}";

            when(resourceRetriever.retrieveResource(any())).thenReturn(new Resource(json, null));

            assertDoesNotThrow(() -> oidcTokenProvider.fetchJWKSet());
            assertTrue(oidcTokenProvider.getPublicKeys().isEmpty());
        }


        @Test
        void givenMissingParameterInJWK_doNotThrowException() throws IOException {
            var json = """
                {
                    "keys": [
                        {
                            "kty": null,
                            "alg": "RS256",
                            "kid": "Lcxckkor94qkrunxHP7Tkib547rzmkXvsYV-nc6U-N4",
                            "use": "sig",
                            "e": "AQAB",
                            "n": "v6wT5k7uLto_VPTV8fW9_wRqWHuqnZbyEYAwNYRdffe9WowwnzUAr0Z93-4xDvCRuVfTfvCe9orEWdjZMaYlDq_Dj5BhLAqmBAF299Kv1GymOioLRDvoVWy0aVHYXXNaqJCPsaWIDiCly-_kJBbnda_rmB28a_878TNxom0mDQ20TI5SgdebqqMBOdHEqIYH1ER9euybekeqJX24EqE9YW4Yug5BOkZ9KcUkiEsH_NPyRlozihj18Qab181PRyKHE6M40W7w67XcRq2llTy-z9RrQupcyvLD7L62KN0ey8luKWnVg4uIOldpyBYyiRX2WPM-2K00RVC0e4jQKs34Gw"
                        }
                    ]
                }
                """;

            when(resourceRetriever.retrieveResource(any())).thenReturn(new Resource(json, null));

            assertDoesNotThrow(() -> oidcTokenProvider.fetchJWKSet());
            assertTrue(oidcTokenProvider.getPublicKeys().isEmpty());
        }

        @Test
        void giveValidJWK_setPublicKey() throws IOException {
            var json = """
                {
                    "keys": [
                        {
                            "kty": "RSA",
                            "alg": "RS256",
                            "kid": "-716sp3XBB_v30lGj2mu5MdXkdh8poa9zJQlAwC46n4",
                            "use": "sig",
                            "e": "AQAB",
                            "n": "5rYyqFsxel0Pv-xRDHPbg3IfumE4ks9ffLvJrfZVgrTQyiFmFfBnyD3r7y6626Yr5-68Pj0I5SHlCBPkkgTU_e9Z3tCYiegtIOeJdSdumWR2JDVAsbpwFJDG_kxP9czgX7HL0T2BPSapx7ba0ZBXd2-SfSDDL-c1Q0rJ1uQEJwDXAGZV4qy_oXuQf5DuV65Xj8y2Qn1DtVEBThxita-kis_H35CTWgW2zyyaS_08wa00R98mnQ2SHfmO5fZABITmH0DO0coDHqKZ429VNNpELLX9e95dirQ1jfngDbBCmy-XsT8yc6NpAaXmd8P2NHdsO2oK46EQEaFRyMcoDTs3-w"
                        }
                    ]
                }
                """;

            when(resourceRetriever.retrieveResource(any())).thenReturn(new Resource(json, null));

            oidcTokenProvider.fetchJWKSet();
            assertFalse(oidcTokenProvider.getPublicKeys().isEmpty());
        }

    }
}
