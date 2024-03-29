/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.service.token;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.impl.DefaultClock;
import io.jsonwebtoken.impl.FixedClock;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.gateway.cache.CachingServiceClientException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OIDCTokenProviderTest {

    private static final String OKTA_JWKS_RESOURCE = "/test_samples/okta_jwks.json";
    private static final String JWKS_KEYS_BODY_INVALID = "\n"
        + "{\n"
        + "    \"keys\": [\n"
        + "        {\n"
        + "           \"kty\": \"RSA\",\n"
        + "           \"alg\": \"RS256\",\n"
        + "           \"kid\": \"Lcxckkor94qkrunxHP7Tkib547rzmkXvsYV-nc6U-N4\",\n"
        + "           \"use\": \"sig\",\n"
        + "           \"e\": \"AQAB\",\n"
        + "           \"n\": \"invalid\"\n"
        + "       }\n"
        + "    ]\n"
        + "}";

    private static final String EXPIRED_TOKEN = "eyJraWQiOiJMY3hja2tvcjk0cWtydW54SFA3VGtpYjU0N3J6bWtYdnNZVi1uYzZVLU40IiwiYWxnIjoiUlMyNTYifQ.eyJ2ZXIiOjEsImp0aSI6IkFULlExakp2UkZ0dUhFUFpGTXNmM3A0enQ5aHBRRHZrSU1CQ3RneU9IcTdlaEkiLCJpc3MiOiJodHRwczovL2Rldi05NTcyNzY4Ni5va3RhLmNvbS9vYXV0aDIvZGVmYXVsdCIsImF1ZCI6ImFwaTovL2RlZmF1bHQiLCJpYXQiOjE2OTcwNjA3NzMsImV4cCI6MTY5NzA2NDM3MywiY2lkIjoiMG9hNmE0OG1uaVhBcUVNcng1ZDciLCJ1aWQiOiIwMHU5OTExOGgxNmtQT1dBbTVkNyIsInNjcCI6WyJvcGVuaWQiXSwiYXV0aF90aW1lIjoxNjk3MDYwMDY0LCJzdWIiOiJzajg5NTA5MkBicm9hZGNvbS5uZXQiLCJncm91cHMiOlsiRXZlcnlvbmUiXX0.Cuf1JVq_NnfBxaCwiLsR5O6DBmVV1fj9utAfKWIF1hlek2hCJsDLQM4ii_ucQ0MM1V3nVE1ZatPB-W7ImWPlGz7NeNBv7jEV9DkX70hchCjPHyYpaUhAieTG75obdufiFpI55bz3qH5cPRvsKv0OKKI9T8D7GjEWsOhv6CevJJZZvgCFLGFfnacKLOY5fEBN82bdmCulNfPVrXF23rOregFjOBJ1cKWfjmB0UGWgI8VBGGemMNm3ACX3OYpTOek2PBfoCIZWOSGnLZumFTYA0F_3DsWYhIJNoFv16_EBBJcp_C0BYE_fiuXzeB0fieNUXASsKp591XJMflDQS_Zt1g";

    private static final String TOKEN = "token";

    private OIDCTokenProvider oidcTokenProvider;
    @Mock
    private CloseableHttpClient httpClient;
    @Mock
    private CloseableHttpResponse response;

    private StatusLine responseStatusLine;
    private BasicHttpEntity responseEntity;

    @BeforeEach
    void setup() throws CachingServiceClientException, IOException {
        responseStatusLine = mock(StatusLine.class);
        responseEntity = new BasicHttpEntity();
        responseEntity.setContent(IOUtils.toInputStream("", StandardCharsets.UTF_8));
        oidcTokenProvider = new OIDCTokenProvider(httpClient, new DefaultClock(), new ObjectMapper());
        ReflectionTestUtils.setField(oidcTokenProvider, "jwkRefreshInterval", 1);
        ReflectionTestUtils.setField(oidcTokenProvider, "jwksUri", "https://jwksurl");
        oidcTokenProvider.clientId = "client_id";
        oidcTokenProvider.clientSecret = "client_secret";
    }

    @Nested
    class GivenInitializationWithJwks {

        @BeforeEach
        void setup() {

            responseEntity.setContent(getClass().getResourceAsStream(OKTA_JWKS_RESOURCE));
        }

        @Test
        @SuppressWarnings("unchecked")
        void initialized_thenJwksFullfilled() throws IOException {
            Map<String, JwkKeys> jwks = (Map<String, JwkKeys>) ReflectionTestUtils.getField(oidcTokenProvider, "jwks");
            when(responseStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
            when(response.getStatusLine()).thenReturn(responseStatusLine);
            when(response.getEntity()).thenReturn(responseEntity);
            when(httpClient.execute(any())).thenReturn(response);
            oidcTokenProvider.afterPropertiesSet();
            assertFalse(jwks.isEmpty());
            assertTrue(jwks.containsKey("Lcxckkor94qkrunxHP7Tkib547rzmkXvsYV-nc6U-N4"));
            assertTrue(jwks.containsKey("-716sp3XBB_v30lGj2mu5MdXkdh8poa9zJQlAwC46n4"));
            assertNotNull(jwks.get("Lcxckkor94qkrunxHP7Tkib547rzmkXvsYV-nc6U-N4"));
            assertInstanceOf(Key.class, jwks.get("Lcxckkor94qkrunxHP7Tkib547rzmkXvsYV-nc6U-N4"));
        }

        @Test
        @SuppressWarnings("unchecked")
        void whenRequestFails_thenNotInitialized() throws IOException {
            Map<String, JwkKeys> jwks = (Map<String, JwkKeys>) ReflectionTestUtils.getField(oidcTokenProvider, "jwks");
            when(responseStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            when(response.getStatusLine()).thenReturn(responseStatusLine);
            when(response.getEntity()).thenReturn(responseEntity);
            when(httpClient.execute(any())).thenReturn(response);
            oidcTokenProvider.afterPropertiesSet();
            assertTrue(jwks.isEmpty());
        }

        @Test
        @SuppressWarnings("unchecked")
        void whenUriNotProvided_thenNotInitialized() {
            ReflectionTestUtils.setField(oidcTokenProvider, "jwksUri", "");
            Map<String, JwkKeys> jwks = (Map<String, JwkKeys>) ReflectionTestUtils.getField(oidcTokenProvider, "jwks");
            oidcTokenProvider.afterPropertiesSet();
            assertTrue(jwks.isEmpty());
        }

        @Test
        @SuppressWarnings("unchecked")
        void whenInvalidKey_thenNotInitialized() throws IOException {
            responseEntity.setContent(IOUtils.toInputStream(JWKS_KEYS_BODY_INVALID, StandardCharsets.UTF_8));
            Map<String, JwkKeys> jwks = (Map<String, JwkKeys>) ReflectionTestUtils.getField(oidcTokenProvider, "jwks");
            when(responseStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
            when(response.getStatusLine()).thenReturn(responseStatusLine);
            when(response.getEntity()).thenReturn(responseEntity);
            when(httpClient.execute(any())).thenReturn(response);
            oidcTokenProvider.afterPropertiesSet();
            assertTrue(jwks.isEmpty());
        }
    }

    @Nested
    class GivenTokenForValidation {

        @SuppressWarnings("unchecked")
        private void initJwks() throws IOException {
            Map<String, JwkKeys> jwks = (Map<String, JwkKeys>) ReflectionTestUtils.getField(oidcTokenProvider, "jwks");
            responseEntity.setContent(getClass().getResourceAsStream(OKTA_JWKS_RESOURCE));
            when(responseStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
            when(response.getStatusLine()).thenReturn(responseStatusLine);
            when(response.getEntity()).thenReturn(responseEntity);
            when(httpClient.execute(any())).thenReturn(response);
            oidcTokenProvider.afterPropertiesSet();
            assertFalse(jwks.isEmpty());
        }

        @Test
        void whenValidTokenExpired_thenReturnInvalid() throws IOException {
            initJwks();
            assertFalse(oidcTokenProvider.isValid(EXPIRED_TOKEN));
        }

        @Test
        void whenValidtoken_thenReturnValid() throws IOException {
            initJwks();
            ReflectionTestUtils.setField(oidcTokenProvider, "clock", new FixedClock(new Date(Instant.ofEpochSecond(1697060773 + 1000L).toEpochMilli())));
            assertTrue(oidcTokenProvider.isValid(EXPIRED_TOKEN));
        }

        @Test
        void whenInvalidToken_thenReturnInvalid() throws IOException {
            initJwks();
            assertFalse(oidcTokenProvider.isValid(TOKEN));
        }

        @Test
        @SuppressWarnings("unchecked")
        void whenNoJwks_thenReturnInvalid() {
            Map<String, Key> jwks = (Map<String, Key>) ReflectionTestUtils.getField(oidcTokenProvider, "jwks");
            assumeTrue(jwks.isEmpty());
            assertFalse(oidcTokenProvider.isValid(TOKEN));
        }

    }

    @Nested
    class GivenEmptyTokenProvided {
        @Test
        void whenTokenIsNull_thenReturnInvalid() {
            assertFalse(oidcTokenProvider.isValid(null));
        }

        @Test
        void whenTokenIsEmpty_thenReturnInvalid() {
            assertFalse(oidcTokenProvider.isValid(""));
        }
    }

    @Nested
    class GivenInvalidConfiguration {

        @ParameterizedTest
        @NullSource
        @EmptySource
        void whenInvalidClientId_thenReturnInvalid(String id) {
            oidcTokenProvider.clientId = id;
            assertFalse(oidcTokenProvider.isValid(TOKEN));
        }

        @ParameterizedTest
        @NullSource
        @EmptySource
        void whenInvalidClientSecret_thenReturnInvalid(String secret) {
            oidcTokenProvider.clientSecret = secret;
            assertFalse(oidcTokenProvider.isValid(TOKEN));
        }
    }

    @Nested
    class JwksUriLoad {
        @Mock
        private CloseableHttpClient httpClientMock;
        @Mock
        CloseableHttpResponse httpResponse;

        @BeforeEach
        public void setUp() {
            oidcTokenProvider = new OIDCTokenProvider(httpClientMock, new DefaultClock(), new ObjectMapper());
            ReflectionTestUtils.setField(oidcTokenProvider, "jwksUri", "https://jwksurl");
        }

        @Test
        void shouldNotModifyJwksUri() throws IOException {
            when(httpClientMock.execute(any())).thenReturn(httpResponse);

            oidcTokenProvider.fetchJwksUrls();

            verify(httpClientMock).execute(argThat((getJwksRequest) -> getJwksRequest.getURI().toString().equals("https://jwksurl")));
        }
    }
}
