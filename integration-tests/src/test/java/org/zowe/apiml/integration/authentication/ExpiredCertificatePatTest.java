/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.integration.authentication;

import com.nimbusds.jose.jwk.RSAKey;
import io.jsonwebtoken.Jwts;
import io.restassured.RestAssured;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.SecurityUtils;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.GeneralAuthenticationTest;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@GeneralAuthenticationTest
class ExpiredCertificatePatTest implements TestWithStartedInstances {

    private static final String EXPIRED_KEYSTORE = System.getProperty("apiml.test.expiredKeystore");
    private static final char[] EXPIRED_KEYSTORE_PASSWORD = System.getProperty("apiml.test.expiredKeystorePassword", "").toCharArray();
    private static final String EXPIRED_ALIAS = System.getProperty("apiml.test.expiredAlias");

    private static final String QUERY_URL = SecurityUtils.getGatewayUrl("/zaas/api/v1/auth/query");
    private static final String JWKS_URL = SecurityUtils.getGatewayUrl("/zaas/api/v1/auth/keys/public/all");

    private static PrivateKey expiredKey;
    private static X509Certificate expiredCertificate;

    @BeforeAll
    static void loadExpiredKeyPair() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (var in = new FileInputStream(EXPIRED_KEYSTORE)) {
            keyStore.load(in, EXPIRED_KEYSTORE_PASSWORD);
        }
        expiredKey = (PrivateKey) keyStore.getKey(EXPIRED_ALIAS, EXPIRED_KEYSTORE_PASSWORD);
        expiredCertificate = (X509Certificate) keyStore.getCertificate(EXPIRED_ALIAS);
    }

    private static String thumbprint() throws Exception {
        return new RSAKey.Builder((RSAPublicKey) expiredCertificate.getPublicKey()).build().computeThumbprint().toString();
    }

    private static String patSignedWithExpiredKey(String kid, Instant expiration) {
        var builder = Jwts.builder()
            .setHeaderParam("typ", "JWT")
            .setIssuer("APIML_PAT")
            .setSubject(SecurityUtils.USERNAME)
            .setIssuedAt(new Date())
            .setExpiration(Date.from(expiration))
            .setId(UUID.randomUUID().toString())
            .claim("auth.prov", "zosmf")
            .claim("scopes", List.of("service"));
        if (kid != null) {
            builder.setHeaderParam("kid", kid);
        }
        return builder.signWith(expiredKey, SignatureAlgorithm.RS256).compact();
    }

    private static int queryStatus(String token) {
        return given()
            .config(RestAssured.config().sslConfig(SecurityUtils.getConfiguredSslConfig()))
            .cookie(SecurityUtils.PAT_COOKIE_AUTH_NAME, token)
        .when()
            .get(QUERY_URL)
        .then()
            .extract().statusCode();
    }

    @Nested
    class GivenPatSignedWithExpiredCertificateKey {

        @Test
        void whenKidMatchesKeyringEntry_thenAuthenticationSucceeds() throws Exception {
            String token = patSignedWithExpiredKey(thumbprint(), Instant.now().plusSeconds(300));
            assertEquals(SC_OK, queryStatus(token));
        }

        @Test
        void whenKidIsUnknown_thenAuthenticationFails() {
            String token = patSignedWithExpiredKey("unknownKid", Instant.now().plusSeconds(300));
            assertEquals(SC_UNAUTHORIZED, queryStatus(token));
        }

        @Test
        void whenKidIsMissing_thenAuthenticationFailsBecauseActiveKeyIsUsed() {
            String token = patSignedWithExpiredKey(null, Instant.now().plusSeconds(300));
            assertEquals(SC_UNAUTHORIZED, queryStatus(token));
        }

        @Test
        void whenTokenItselfIsExpired_thenAuthenticationFails() throws Exception {
            String token = patSignedWithExpiredKey(thumbprint(), Instant.now().minusSeconds(60));
            assertEquals(SC_UNAUTHORIZED, queryStatus(token));
        }

        @Test
        void whenSignatureIsTampered_thenAuthenticationFails() throws Exception {
            String token = patSignedWithExpiredKey(thumbprint(), Instant.now().plusSeconds(300));
            String tampered = token.substring(0, token.length() - 4) + "AAAA";
            assertEquals(SC_UNAUTHORIZED, queryStatus(tampered));
        }
    }

    @Nested
    class GivenPublicKeysEndpoint {

        @Test
        void whenCalled_thenExpiredCertificateKeyIsPublished() throws Exception {
            given()
                .config(RestAssured.config().sslConfig(SecurityUtils.getConfiguredSslConfig()))
            .when()
                .get(JWKS_URL)
            .then()
                .statusCode(is(SC_OK))
                .body("keys.kid", hasItem(thumbprint()));
        }
    }

    @Test
    void whenLoggingInWithCredentials_thenActiveKeyStillIssuesAcceptedToken() {
        String token = SecurityUtils.gatewayToken();
        given()
            .config(RestAssured.config().sslConfig(SecurityUtils.getConfiguredSslConfig()))
            .cookie(SecurityUtils.COOKIE_NAME, token)
        .when()
            .get(QUERY_URL)
        .then()
            .statusCode(is(SC_OK));
    }
}
