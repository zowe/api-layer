/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gatewayservice;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import io.restassured.RestAssured;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.categories.MainframeDependentTests;
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.GatewayServiceConfiguration;

import java.text.ParseException;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.zowe.apiml.gatewayservice.SecurityUtils.getConfiguredSslConfig;

@MainframeDependentTests
public class PublicKeyIntegrationTest {

    private final static String ALL_PUBLIC_KEY_ENDPOINT = "/api/v1/gateway/auth/keys/public/all";
    private final static String CURRENT_PUBLIC_KEY_ENDPOINT = "/api/v1/gateway/auth/keys/public/current";

    private final static GatewayServiceConfiguration SERVICE_CONFIGURATION = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
    private final static String GATEWAY_SCHEME = SERVICE_CONFIGURATION.getScheme();
    private final static String GATEWAY_HOST = SERVICE_CONFIGURATION.getHost();
    private final static int GATEWAY_PORT = SERVICE_CONFIGURATION.getPort();
    private final static String GATEWAY_URL = String.format("%s://%s:%s", GATEWAY_SCHEME, GATEWAY_HOST, GATEWAY_PORT);

    @BeforeEach
    public void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
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

    @Test
    @TestsNotMeantForZowe
    public void testAllPublicKeys() throws ParseException {
        String response = given()
            .when()
            .get(GATEWAY_URL + ALL_PUBLIC_KEY_ENDPOINT)
        .then()
            .statusCode(is(SC_OK))
            .extract().body().asString();
        JWKSet jwkSet = JWKSet.parse(response);

        verifyKeys(jwkSet, 2);
    }

    @Test
    @TestsNotMeantForZowe
    public void testCurrentPublicKeys() throws ParseException {
        String response = given()
            .when()
            .get(GATEWAY_URL + CURRENT_PUBLIC_KEY_ENDPOINT)
            .then()
            .statusCode(is(SC_OK))
            .extract().body().asString();
        JWKSet jwkSet = JWKSet.parse(response);

        verifyKeys(jwkSet, 1);
    }

}
