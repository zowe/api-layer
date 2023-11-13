/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.integration.zaas;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.ZaasTest;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Collections;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.zowe.apiml.integration.zaas.ZaasTestUtil.*;
import static org.zowe.apiml.util.SecurityUtils.*;

@ZaasTest
class ZoweTokensTest implements TestWithStartedInstances {

    @Nested
    class WhenGeneratingZosmfTokens_returnValidZosmfToken {

        @BeforeEach
        void setUpCertificate() {
            RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
        }

        @Test
        void givenValidZosmfToken() {
            String zosmfToken = getZosmfJwtToken();

            //@formatter:off
            given()
                .cookie(COOKIE, zosmfToken)
            .when()
                .post(ZAAS_ZOWE_URI)
            .then()
                .statusCode(is(SC_OK))
                .body("cookieName", is(COOKIE))
                .body("token", is(zosmfToken));
            //@formatter:on
        }

        @Test
        void givenValidZoweTokenWithLtpa() throws UnrecoverableKeyException, CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
            String ltpaToken = getZosmfToken(LTPA_COOKIE);
            String zoweToken = generateZoweJwtWithLtpa(ltpaToken);

            //@formatter:off
                given()
                    .header("Authorization", "Bearer " + zoweToken)
                .when()
                    .post(ZAAS_ZOWE_URI)
                .then()
                    .statusCode(is(SC_OK))
                    .body("cookieName", is(COOKIE))
                    .body("token", is(zoweToken));
                //@formatter:on
        }

        @Test
        void givenValidAccessToken() {
            String serviceId = "gateway";
            String pat = personalAccessToken(Collections.singleton(serviceId));

            //@formatter:off
            given()
                .header("Authorization", "Bearer " + pat)
                .header("X-Service-Id", serviceId)
            .when()
                .post(ZAAS_ZOWE_URI)
            .then()
                .statusCode(is(SC_OK))
                .body("cookieName", is(COOKIE))
                .body("token", not(""));
            //@formatter:on
        }

        @ParameterizedTest
        @MethodSource("org.zowe.apiml.integration.zaas.ZaasTestUtil#provideClientCertificates")
        void givenX509Certificate(String certificate) {
            //@formatter:off
            given()
                .header("Client-Cert", certificate)
            .when()
                .post(ZAAS_ZOWE_URI)
            .then()
                .statusCode(is(SC_OK))
                .body("cookieName", is(COOKIE))
                .body("token", not(""));
            //@formatter:on
        }

        @Test
        void givenValidOAuthToken() {
            String oAuthToken = validOktaAccessToken(true);

            //@formatter:off
            given()
                .cookie(COOKIE, oAuthToken)
            .when()
                .post(ZAAS_ZOWE_URI)
            .then()
                .statusCode(is(SC_OK))
                .body("cookieName", is(COOKIE))
                .body("token", not(""));
            //@formatter:on
        }
    }

    // Negative tests are in ZaasNegativeTest since they are common for the whole service
}
