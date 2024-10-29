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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.Requirement;
import com.nimbusds.jose.jwk.*;
import io.jsonwebtoken.impl.DefaultClock;
import io.jsonwebtoken.impl.FixedClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.zaas.cache.CachingServiceClientException;

import java.io.IOException;
import java.net.URL;
import java.security.Key;
import java.security.PublicKey;
import java.text.ParseException;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OIDCTokenProviderJWKTest {

    private static final String OKTA_JWKS_RESOURCE = "/test_samples/okta_jwks.json";

    private static final String EXPIRED_TOKEN = "eyJraWQiOiJMY3hja2tvcjk0cWtydW54SFA3VGtpYjU0N3J6bWtYdnNZVi1uYzZVLU40IiwiYWxnIjoiUlMyNTYifQ.eyJ2ZXIiOjEsImp0aSI6IkFULlExakp2UkZ0dUhFUFpGTXNmM3A0enQ5aHBRRHZrSU1CQ3RneU9IcTdlaEkiLCJpc3MiOiJodHRwczovL2Rldi05NTcyNzY4Ni5va3RhLmNvbS9vYXV0aDIvZGVmYXVsdCIsImF1ZCI6ImFwaTovL2RlZmF1bHQiLCJpYXQiOjE2OTcwNjA3NzMsImV4cCI6MTY5NzA2NDM3MywiY2lkIjoiMG9hNmE0OG1uaVhBcUVNcng1ZDciLCJ1aWQiOiIwMHU5OTExOGgxNmtQT1dBbTVkNyIsInNjcCI6WyJvcGVuaWQiXSwiYXV0aF90aW1lIjoxNjk3MDYwMDY0LCJzdWIiOiJzajg5NTA5MkBicm9hZGNvbS5uZXQiLCJncm91cHMiOlsiRXZlcnlvbmUiXX0.Cuf1JVq_NnfBxaCwiLsR5O6DBmVV1fj9utAfKWIF1hlek2hCJsDLQM4ii_ucQ0MM1V3nVE1ZatPB-W7ImWPlGz7NeNBv7jEV9DkX70hchCjPHyYpaUhAieTG75obdufiFpI55bz3qH5cPRvsKv0OKKI9T8D7GjEWsOhv6CevJJZZvgCFLGFfnacKLOY5fEBN82bdmCulNfPVrXF23rOregFjOBJ1cKWfjmB0UGWgI8VBGGemMNm3ACX3OYpTOek2PBfoCIZWOSGnLZumFTYA0F_3DsWYhIJNoFv16_EBBJcp_C0BYE_fiuXzeB0fieNUXASsKp591XJMflDQS_Zt1g";

    private static final String TOKEN = "token";

    private OIDCTokenProviderJWK oidcTokenProviderJwk;

    private JWKSet jwkSet;

    @BeforeEach
    void setup() throws CachingServiceClientException {
        oidcTokenProviderJwk = new OIDCTokenProviderJWK(new DefaultClock());
        ReflectionTestUtils.setField(oidcTokenProviderJwk, "jwkRefreshInterval", 1);
        ReflectionTestUtils.setField(oidcTokenProviderJwk, "jwksUri", "https://jwksurl");
    }

    @Nested
    class GivenInitializationWithJwks {

        @Test
        @SuppressWarnings("unchecked")
        void initialized_thenJwksFullfilled() throws ParseException, IOException {
            jwkSet = JWKSet.load(getClass().getResourceAsStream(OKTA_JWKS_RESOURCE));
            try (MockedStatic<JWKSet> mockedStatic = Mockito.mockStatic(JWKSet.class)) {
                mockedStatic.when(() -> JWKSet.load(any(URL.class))).thenReturn(jwkSet);
                oidcTokenProviderJwk.afterPropertiesSet();
            }
            Map<String, PublicKey> publicKeys = oidcTokenProviderJwk.getPublicKeys();

            assertFalse(publicKeys.isEmpty());
            assertTrue(publicKeys.containsKey("Lcxckkor94qkrunxHP7Tkib547rzmkXvsYV-nc6U-N4"));
            assertTrue(publicKeys.containsKey("-716sp3XBB_v30lGj2mu5MdXkdh8poa9zJQlAwC46n4"));
            assertNotNull(publicKeys.get("Lcxckkor94qkrunxHP7Tkib547rzmkXvsYV-nc6U-N4"));
            assertInstanceOf(Key.class, publicKeys.get("Lcxckkor94qkrunxHP7Tkib547rzmkXvsYV-nc6U-N4"));
        }

        @Test
        void whenRequestFails_thenNotInitialized() {
            try (MockedStatic<JWKSet> mockedStatic = Mockito.mockStatic(JWKSet.class)) {
                mockedStatic.when(() -> JWKSet.load(any(URL.class))).thenThrow(IOException.class);
                oidcTokenProviderJwk.afterPropertiesSet();
            }
            oidcTokenProviderJwk.afterPropertiesSet();
            assertTrue(oidcTokenProviderJwk.getPublicKeys().isEmpty());
        }

        @Test
        void whenUriNotProvided_thenNotInitialized() {
            ReflectionTestUtils.setField(oidcTokenProviderJwk, "jwksUri", "");
            oidcTokenProviderJwk.afterPropertiesSet();
            assertTrue(oidcTokenProviderJwk.getPublicKeys().isEmpty());
        }

        @Test
        void whenInvalidKeyResponse_thenNotInitialized() {
            try (MockedStatic<JWKSet> mockedStatic = Mockito.mockStatic(JWKSet.class)) {
                mockedStatic.when(() -> JWKSet.load(any(URL.class))).thenThrow(ParseException.class);
                oidcTokenProviderJwk.afterPropertiesSet();
            }
            oidcTokenProviderJwk.afterPropertiesSet();
            assertTrue(oidcTokenProviderJwk.getPublicKeys().isEmpty());
        }
    }

    @Nested
    class GivenTokenForValidation {

        @SuppressWarnings("unchecked")
        private void initPublicKeys() throws IOException, ParseException {
            jwkSet = JWKSet.load(getClass().getResourceAsStream(OKTA_JWKS_RESOURCE));
            try (MockedStatic<JWKSet> mockedStatic = Mockito.mockStatic(JWKSet.class)) {
                mockedStatic.when(() -> JWKSet.load(any(URL.class))).thenReturn(jwkSet);
                oidcTokenProviderJwk.afterPropertiesSet();
            }
            assertFalse(oidcTokenProviderJwk.getPublicKeys().isEmpty());
        }

        @Test
        void whenValidTokenExpired_thenReturnInvalid() throws IOException, ParseException {
            initPublicKeys();
            assertFalse(oidcTokenProviderJwk.isValid(EXPIRED_TOKEN));
        }

        @Test
        void whenValidToken_thenReturnValid() throws IOException, ParseException {
            initPublicKeys();
            ReflectionTestUtils.setField(oidcTokenProviderJwk, "clock", new FixedClock(new Date(Instant.ofEpochSecond(1697060773 + 1000L).toEpochMilli())));
            assertTrue(oidcTokenProviderJwk.isValid(EXPIRED_TOKEN));
        }

        @Test
        void whenInvalidToken_thenReturnInvalid() throws IOException, ParseException {
            initPublicKeys();
            assertFalse(oidcTokenProviderJwk.isValid(TOKEN));
        }

        @Test
        void whenNoJwk_thenReturnInvalid() {
            assumeTrue(oidcTokenProviderJwk.getPublicKeys().isEmpty());
            assertFalse(oidcTokenProviderJwk.isValid(TOKEN));
        }

    }

    @Nested
    class GivenEmptyTokenProvided {
        @Test
        void whenTokenIsNull_thenReturnInvalid() {
            assertFalse(oidcTokenProviderJwk.isValid(null));
        }

        @Test
        void whenTokenIsEmpty_thenReturnInvalid() {
            assertFalse(oidcTokenProviderJwk.isValid(""));
        }
    }

    @Nested
    class JwksUriLoad {


        @BeforeEach
        public void setUp() {
            oidcTokenProviderJwk = new OIDCTokenProviderJWK(new DefaultClock());
            ReflectionTestUtils.setField(oidcTokenProviderJwk, "jwksUri", "https://jwksurl");
        }

        @Test
        void shouldNotModifyJwksUri() {

            try (MockedStatic<JWKSet> mockedStatic = Mockito.mockStatic(JWKSet.class)) {
                mockedStatic.when(() -> JWKSet.load(any(URL.class))).thenReturn(new JWKSet());

                oidcTokenProviderJwk.fetchJWKSet();
                mockedStatic.verify(() -> JWKSet.load(new URL("https://jwksurl")), times(1));
            }
        }

        @Test
        void shouldHandleNullPointer_whenJWKKeyNull() {

            JWKSet mockedJwtSet = mock(JWKSet.class);
            List<JWK> mockedKeys = new ArrayList<>();
            JWK mockedJwk = mock(JWK.class);
            when(mockedJwk.getKeyUse()).thenReturn(null);
            RSAKey rsaKey = mock(RSAKey.class);
            mockedKeys.add(mockedJwk);
            when(mockedJwtSet.getKeys()).thenReturn(mockedKeys);

            try (MockedStatic<JWKSet> mockedStatic = Mockito.mockStatic(JWKSet.class)) {
                mockedStatic.when(() -> JWKSet.load(any(URL.class))).thenReturn(mockedJwtSet);

                oidcTokenProviderJwk.fetchJWKSet();

                verify(rsaKey, never()).toRSAPublicKey();
            } catch (JOSEException e) {
                fail("Exception thrown: " + e.getMessage());
            }
        }

        @Test
        void shouldHandleNullPointer_whenJWKTypeNull() {

            JWKSet mockedJwtSet = mock(JWKSet.class);
            List<JWK> mockedKeys = new ArrayList<>();
            JWK mockedJwk = mock(JWK.class);
            when(mockedJwk.getKeyType()).thenReturn(null);
            RSAKey rsaKey = mock(RSAKey.class);
            mockedKeys.add(mockedJwk);
            when(mockedJwtSet.getKeys()).thenReturn(mockedKeys);

            try (MockedStatic<JWKSet> mockedStatic = Mockito.mockStatic(JWKSet.class)) {
                mockedStatic.when(() -> JWKSet.load(any(URL.class))).thenReturn(mockedJwtSet);

                oidcTokenProviderJwk.fetchJWKSet();

                verify(rsaKey, never()).toRSAPublicKey();
            } catch (JOSEException e) {
                fail("Exception thrown: " + e.getMessage());
            }
        }

        @Test
        void throwsCorrectException() throws JOSEException {

            try (MockedStatic<JWKSet> mockedStatic = Mockito.mockStatic(JWKSet.class)) {
                JWKSet jwkSet1 = mock(JWKSet.class);
                JWK invalidJwk = mock(JWK.class);
                when(invalidJwk.getKeyUse()).thenReturn(new KeyUse("sig"));
                when(invalidJwk.getKeyType()).thenReturn(new KeyType("RSA", Requirement.REQUIRED));
                when(invalidJwk.getKeyID()).thenReturn("123");
                RSAKey rsaKey = mock(RSAKey.class);
                when(invalidJwk.toRSAKey()).thenReturn(rsaKey);
                when(rsaKey.toRSAPublicKey()).thenThrow(new JOSEException());
                List<JWK> keys = Collections.singletonList(invalidJwk);
                when(jwkSet1.getKeys()).thenReturn(keys);
                mockedStatic.when(() -> JWKSet.load(any(URL.class))).thenReturn(jwkSet1);

                assertDoesNotThrow(() -> oidcTokenProviderJwk.fetchJWKSet());
            }
        }
    }
}
