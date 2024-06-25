/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.integration.zos;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.GatewayTest;
import org.zowe.apiml.util.categories.MainframeDependentTests;
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.text.ParseException;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.zowe.apiml.util.SecurityUtils.getConfiguredSslConfig;

/**
 * Verify we can retrieve keys for JWT signature from Gateway, zOSMF and potentially other services.
 */
@TestsNotMeantForZowe
@GatewayTest
class PublicKeyIntegrationTest implements TestWithStartedInstances {

    private final static String ALL_PUBLIC_KEY_ENDPOINT = "/gateway/api/v1/auth/keys/public/all";
    private final static String CURRENT_PUBLIC_KEY_ENDPOINT = "/gateway/api/v1/auth/keys/public/current";

    @BeforeEach
    void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
    }

    @Nested
    class WhenRequestingAllTokenRelatedKeys {
        @Nested
        class ReturnZosmfAndInternalOnes {
            @Test
            @MainframeDependentTests
            void givenNoAuthentication() throws ParseException {
                String response = given()
                .when()
                    .get(HttpRequestUtils.getUriFromGateway(ALL_PUBLIC_KEY_ENDPOINT))
                .then()
                    .statusCode(is(SC_OK))
                    .extract().body().asString();

                JWKSet jwkSet = JWKSet.parse(response);

                verifyKeys(jwkSet, 2);
            }
        }
    }

    @Nested
    class WhenRequestingCurrentlyUsedKey {
        @Nested
        class ReturnActuallyUsedKey {
            @Test
            @MainframeDependentTests
            void givenNoAuthentication() throws ParseException {
                String response = given()
                    .accept(ContentType.JSON)
                .when()
                    .get(HttpRequestUtils.getUriFromGateway(CURRENT_PUBLIC_KEY_ENDPOINT))
                .then()
                    .statusCode(is(SC_OK))
                    .extract().body().asString();
                JWKSet jwkSet = JWKSet.parse(response);

                verifyKeys(jwkSet, 1);
            }
        }
    }

    private void verifyKeys(JWKSet jwkSet, int count) {
        assertEquals(count, jwkSet.getKeys().size());
        for (JWK jwk : jwkSet.getKeys()) {
            assertEquals("RSA", jwk.getKeyType().toString());
            assertTrue(jwk instanceof RSAKey);

            RSAKey rsaKey = (RSAKey) jwk;
            assertNotNull(rsaKey.getModulus());
            assertFalse(StringUtils.isEmpty(rsaKey.getModulus().toString()));
            assertNotNull(rsaKey.getPublicExponent());
            assertFalse(StringUtils.isEmpty(rsaKey.getPublicExponent().toString()));
        }
    }

}
