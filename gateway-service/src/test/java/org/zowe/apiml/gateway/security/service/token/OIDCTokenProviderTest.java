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
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OIDCTokenProviderTest {

    private static final String NOT_VALID_BODY = "{\n" +
    "    \"active\": false\n" +
    "}";

    private static final String JWKS_KEYS_BODY = "\n"
    + "{\n"
    + "    \"keys\": [\n"
    + "        {\n"
    + "           \"kty\": \"RSA\",\n"
    + "           \"alg\": \"RS256\",\n"
    + "           \"kid\": \"mLrvKBf4erBjkXcSCb2hjCBHLT1jM8MkYK-l-Z8MGe0\",\n"
    + "           \"use\": \"sig\",\n"
    + "           \"e\": \"AQAB\",\n"
    + "           \"n\": \"hU4h--6LTL7SfdjV7rbQThGCiO8gQOMzboxqVjExH5UCj-tvTceTtx7FdVM5NV_hNhPc3aOO2ItkzYCmk8f9VNGSH4UBNcdCSlni3d4ZEkL2lyLxDFf3l_8gUs8Ev-Jh48WJSBcfjTH5RXsVRrjqS3_yjj9ZfTLHEG-a7tKo4J6NNrH0kbwQQu0cJPA1shU_AX23Yny8MbtzcmZaIwYmYLC4JKKAGgtg49Kyk6JYIwvklqPTHXoHQuWJLS32tV_ZaXKATW0vtFzyZnKkQ09cYXU260jWxLfVCBJA_5Lj0sVga7p-NygwzfQXlrHPx4ZsHrmkjkibzMH-18RQrMs38w\"\n"
    + "       }\n"
    + "    ]\n"
    + "}";

    private static final String TOKEN = "token";

    private OIDCTokenProvider oidcTokenProvider;

    @Mock
    private OIDCTokenProvider underTest;
    @Mock
    private CloseableHttpClient httpClient;
    @Mock
    private CloseableHttpResponse response;

    private StatusLine responseStatusLine;
    private BasicHttpEntity responseEntity;

    private ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setup() throws CachingServiceClientException, IOException {
        responseStatusLine = mock(StatusLine.class);
        responseEntity = new BasicHttpEntity();
        responseEntity.setContent(IOUtils.toInputStream("", StandardCharsets.UTF_8));
        oidcTokenProvider = new OIDCTokenProvider(httpClient, mapper, "https://jwksurl", 1L);
        oidcTokenProvider.clientId = "client_id";
        oidcTokenProvider.clientSecret = "client_secret";
    }

    @Nested
    class GivenInitializationWithJwks {

        @BeforeEach
        void setup() throws IOException {
            responseEntity.setContent(IOUtils.toInputStream(JWKS_KEYS_BODY, StandardCharsets.UTF_8));
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
            assertTrue(jwks.containsKey("mLrvKBf4erBjkXcSCb2hjCBHLT1jM8MkYK-l-Z8MGe0"));
            assertNotNull(jwks.get("mLrvKBf4erBjkXcSCb2hjCBHLT1jM8MkYK-l-Z8MGe0"));
            assertInstanceOf(Key.class, jwks.get("mLrvKBf4erBjkXcSCb2hjCBHLT1jM8MkYK-l-Z8MGe0"));
        }

        @Test
        void whenRequestFails_thenRetry() {

        }

        @Test
        void whenRequestFails_thenNotInitialized() {

        }

    }

    @Nested
    class GivenTokenForValidation {

        @SuppressWarnings("unchecked")
        private void initJwks() throws ClientProtocolException, IOException {
            Map<String, JwkKeys> jwks = (Map<String, JwkKeys>) ReflectionTestUtils.getField(oidcTokenProvider, "jwks");
            responseEntity.setContent(IOUtils.toInputStream(JWKS_KEYS_BODY, StandardCharsets.UTF_8));
            when(responseStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
            when(response.getStatusLine()).thenReturn(responseStatusLine);
            when(response.getEntity()).thenReturn(responseEntity);
            when(httpClient.execute(any())).thenReturn(response);
            oidcTokenProvider.afterPropertiesSet();
            assertFalse(jwks.isEmpty());
        }

        @Test
        void whenValidToken_thenReturnValid() throws ClientProtocolException, IOException {
            initJwks();
            assertTrue(oidcTokenProvider.isValid(TOKEN));
        }

        @Test
        void whenInvalidToken_thenReturnInvalid() throws ClientProtocolException, IOException {
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

}
