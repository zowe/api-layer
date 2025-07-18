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
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jose.util.Resource;
import io.jsonwebtoken.impl.DefaultClock;
import io.jsonwebtoken.impl.FixedClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.zaas.cache.CachingServiceClientException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OIDCTokenProviderJWKTest {

    private static final String OKTA_JWKS_RESOURCE = "test_samples/okta_jwks.json";

    private static final String EXPIRED_TOKEN = "eyJraWQiOiJMY3hja2tvcjk0cWtydW54SFA3VGtpYjU0N3J6bWtYdnNZVi1uYzZVLU40IiwiYWxnIjoiUlMyNTYifQ.eyJ2ZXIiOjEsImp0aSI6IkFULlExakp2UkZ0dUhFUFpGTXNmM3A0enQ5aHBRRHZrSU1CQ3RneU9IcTdlaEkiLCJpc3MiOiJodHRwczovL2Rldi05NTcyNzY4Ni5va3RhLmNvbS9vYXV0aDIvZGVmYXVsdCIsImF1ZCI6ImFwaTovL2RlZmF1bHQiLCJpYXQiOjE2OTcwNjA3NzMsImV4cCI6MTY5NzA2NDM3MywiY2lkIjoiMG9hNmE0OG1uaVhBcUVNcng1ZDciLCJ1aWQiOiIwMHU5OTExOGgxNmtQT1dBbTVkNyIsInNjcCI6WyJvcGVuaWQiXSwiYXV0aF90aW1lIjoxNjk3MDYwMDY0LCJzdWIiOiJzajg5NTA5MkBicm9hZGNvbS5uZXQiLCJncm91cHMiOlsiRXZlcnlvbmUiXX0.Cuf1JVq_NnfBxaCwiLsR5O6DBmVV1fj9utAfKWIF1hlek2hCJsDLQM4ii_ucQ0MM1V3nVE1ZatPB-W7ImWPlGz7NeNBv7jEV9DkX70hchCjPHyYpaUhAieTG75obdufiFpI55bz3qH5cPRvsKv0OKKI9T8D7GjEWsOhv6CevJJZZvgCFLGFfnacKLOY5fEBN82bdmCulNfPVrXF23rOregFjOBJ1cKWfjmB0UGWgI8VBGGemMNm3ACX3OYpTOek2PBfoCIZWOSGnLZumFTYA0F_3DsWYhIJNoFv16_EBBJcp_C0BYE_fiuXzeB0fieNUXASsKp591XJMflDQS_Zt1g";

    private static final String TOKEN = "token";

    private OIDCTokenProviderJWK oidcTokenProviderJwk;

    @Mock private DefaultResourceRetriever resourceRetriever;

    @BeforeEach
    void setup() throws CachingServiceClientException, IOException {
        oidcTokenProviderJwk = new OIDCTokenProviderJWK(new DefaultClock(), resourceRetriever);
        ReflectionTestUtils.setField(oidcTokenProviderJwk, "jwkRefreshInterval", 1);
        ReflectionTestUtils.setField(oidcTokenProviderJwk, "jwksUri", "https://jwksurl");

        String oktaJwks = Resources.toString(Resources.getResource(OKTA_JWKS_RESOURCE), StandardCharsets.UTF_8);

        lenient().when(resourceRetriever.retrieveResource(any())).thenReturn(new Resource(oktaJwks, null));
    }

    @Nested
    class GivenInitializationWithJwks {

        @Test
        void initialized_thenJwksFullfilled() {
            oidcTokenProviderJwk.afterPropertiesSet();
            Map<String, PublicKey> publicKeys = oidcTokenProviderJwk.getPublicKeys();

            assertFalse(publicKeys.isEmpty());
            assertTrue(publicKeys.containsKey("Lcxckkor94qkrunxHP7Tkib547rzmkXvsYV-nc6U-N4"));
            assertTrue(publicKeys.containsKey("-716sp3XBB_v30lGj2mu5MdXkdh8poa9zJQlAwC46n4"));
            assertNotNull(publicKeys.get("Lcxckkor94qkrunxHP7Tkib547rzmkXvsYV-nc6U-N4"));
            assertInstanceOf(Key.class, publicKeys.get("Lcxckkor94qkrunxHP7Tkib547rzmkXvsYV-nc6U-N4"));
        }

        @Test
        void whenRequestFails_thenNotInitialized() throws IOException {
            doThrow(new IOException("failed request")).when(resourceRetriever).retrieveResource(any());
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
        void whenInvalidKeyResponse_thenNotInitialized() throws IOException {
            when(resourceRetriever.retrieveResource(any())).thenReturn(new Resource("invalid_json", null));
            oidcTokenProviderJwk.afterPropertiesSet();
            assertTrue(oidcTokenProviderJwk.getPublicKeys().isEmpty());
        }
    }

    @Nested
    class GivenTokenForValidation {

        @Test
        void whenValidTokenExpired_thenReturnInvalid() {
            assertFalse(oidcTokenProviderJwk.isValid(EXPIRED_TOKEN));
        }

        @Test
        void whenValidToken_thenReturnValid() {
            ReflectionTestUtils.setField(oidcTokenProviderJwk, "clock", new FixedClock(new Date(Instant.ofEpochSecond(1697060773 + 1000L).toEpochMilli())));
            assertTrue(oidcTokenProviderJwk.isValid(EXPIRED_TOKEN));
        }

        @Test
        void whenInvalidToken_thenReturnInvalid() {
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
            oidcTokenProviderJwk = new OIDCTokenProviderJWK(new DefaultClock(), resourceRetriever);
            ReflectionTestUtils.setField(oidcTokenProviderJwk, "jwksUri", "https://jwksurl");
            ReflectionTestUtils.setField(oidcTokenProviderJwk, "resourceRetriever", resourceRetriever);
        }

        @Test
        void shouldNotModifyJwksUri() throws IOException {
            var json = "{}";

            when(resourceRetriever.retrieveResource(any())).thenReturn(new Resource(json, null));

            assertDoesNotThrow(() -> oidcTokenProviderJwk.fetchJWKSet());
        }

        @Test
        void shouldHandleNullPointer_whenJWKKeyNull() throws IOException {
            var json = """
        {
            "keys": [
                {
                    "kty": RSA,
                    "alg": "RS256",
                    "kid": "Lcxckkor94qkrunxHP7Tkib547rzmkXvsYV-nc6U-N4",
                    "use": null,
                    "e": "AQAB",
                    "n": "v6wT5k7uLto_VPTV8fW9_wRqWHuqnZbyEYAwNYRdffe9WowwnzUAr0Z93-4xDvCRuVfTfvCe9orEWdjZMaYlDq_Dj5BhLAqmBAF299Kv1GymOioLRDvoVWy0aVHYXXNaqJCPsaWIDiCly-_kJBbnda_rmB28a_878TNxom0mDQ20TI5SgdebqqMBOdHEqIYH1ER9euybekeqJX24EqE9YW4Yug5BOkZ9KcUkiEsH_NPyRlozihj18Qab181PRyKHE6M40W7w67XcRq2llTy-z9RrQupcyvLD7L62KN0ey8luKWnVg4uIOldpyBYyiRX2WPM-2K00RVC0e4jQKs34Gw"
                }
            ]
        }
        """;

            when(resourceRetriever.retrieveResource(any())).thenReturn(new Resource(json, null));
            assertDoesNotThrow(() -> oidcTokenProviderJwk.fetchJWKSet());
        }


        @Test
        void shouldHandleNullPointer_whenJWKTypeNull() throws IOException {
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

            assertDoesNotThrow(() -> oidcTokenProviderJwk.fetchJWKSet());
        }

        @Test
        void throwsCorrectException() throws IOException {
            var json = """
        {
            "keys": [
                {
                    "kty": RSA,
                    "kid": "123",
                    "use": "sig",
                    "e": "AQAB",
                    "n": "v6wT5k7uLto_VPTV8fW9_wRqWHuqnZbyEYAwNYRdffe9WowwnzUAr0Z93-4xDvCRuVfTfvCe9orEWdjZMaYlDq_Dj5BhLAqmBAF299Kv1GymOioLRDvoVWy0aVHYXXNaqJCPsaWIDiCly-_kJBbnda_rmB28a_878TNxom0mDQ20TI5SgdebqqMBOdHEqIYH1ER9euybekeqJX24EqE9YW4Yug5BOkZ9KcUkiEsH_NPyRlozihj18Qab181PRyKHE6M40W7w67XcRq2llTy-z9RrQupcyvLD7L62KN0ey8luKWnVg4uIOldpyBYyiRX2WPM-2K00RVC0e4jQKs34Gw"
                }
            ]
        }
        """;

            when(resourceRetriever.retrieveResource(any())).thenReturn(new Resource(json, null));

            assertDoesNotThrow(() -> oidcTokenProviderJwk.fetchJWKSet());
        }

    }
}
