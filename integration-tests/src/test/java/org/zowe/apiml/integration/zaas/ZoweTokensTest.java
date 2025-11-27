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
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.zowe.apiml.integration.zaas.ZaasTestUtil.COOKIE;
import static org.zowe.apiml.integration.zaas.ZaasTestUtil.ZAAS_ZOWE_URI;
import static org.zowe.apiml.integration.zaas.ZaasTestUtil.isTestForICSF;
import static org.zowe.apiml.util.SecurityUtils.generateZoweJwtWithLtpa;
import static org.zowe.apiml.util.SecurityUtils.getConfiguredSslConfig;
import static org.zowe.apiml.util.SecurityUtils.getZosmfJwtToken;
import static org.zowe.apiml.util.SecurityUtils.getZosmfLtpaToken;
import static org.zowe.apiml.util.SecurityUtils.personalAccessToken;
import static org.zowe.apiml.util.SecurityUtils.validOidcAccessToken;

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
            assumeTrue(ZAAS_ZOWE_URI.isPresent());
            var zosmfToken = getZosmfJwtToken();

            //@formatter:off
            given()
                .cookie(COOKIE, zosmfToken)
            .when()
                .post(ZAAS_ZOWE_URI.get())
            .then()
                .statusCode(SC_OK)
                .body("cookieName", is(COOKIE))
                .body("token", is(zosmfToken));
            //@formatter:on
        }

        @Test
        void givenValidZoweTokenWithLtpa() throws UnrecoverableKeyException, CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
            assumeFalse(isTestForICSF());
            assumeTrue(ZAAS_ZOWE_URI.isPresent());
            var ltpaToken = getZosmfLtpaToken();
            var zoweToken = generateZoweJwtWithLtpa(ltpaToken);

            //@formatter:off
                given()
                    .header("Authorization", "Bearer " + zoweToken)
                .when()
                    .post(ZAAS_ZOWE_URI.get())
                .then()
                    .statusCode(SC_OK)
                    .body("cookieName", is(COOKIE))
                    .body("token", is(zoweToken));
                //@formatter:on
        }

        @Test
        void givenValidAccessToken() {
            assumeTrue(ZAAS_ZOWE_URI.isPresent());
            var serviceId = "gateway";
            var pat = personalAccessToken(Collections.singleton(serviceId));

            //@formatter:off
            given()
                .header("Authorization", "Bearer " + pat)
                .header("X-Service-Id", serviceId)
            .when()
                .post(ZAAS_ZOWE_URI.get())
            .then()
                .statusCode(SC_OK)
                .body("cookieName", is(COOKIE))
                .body("token", not(emptyOrNullString()));
            //@formatter:on
        }

        @ParameterizedTest(name = "ZoweTokensTest.givenX509Certificate {1}")
        @MethodSource("org.zowe.apiml.integration.zaas.ZaasTestUtil#provideClientCertificates")
        void givenX509Certificate(String certificate, String description) {
            assumeTrue(ZAAS_ZOWE_URI.isPresent());
            assumeFalse(isTestForICSF());
            //@formatter:off
            given()
                .header("Client-Cert", certificate)
            .when()
                .post(ZAAS_ZOWE_URI.get())
            .then()
                .statusCode(SC_OK)
                .body("cookieName", is(COOKIE))
                .body("token", not(emptyOrNullString()));
            //@formatter:on
        }

        @Test
        void givenValidOAuthToken() {
            assumeTrue(ZAAS_ZOWE_URI.isPresent());
            var oAuthToken = validOidcAccessToken(true);

            //@formatter:off
            given()
                .log().all()
                .cookie(COOKIE, oAuthToken)
            .when()
                .post(ZAAS_ZOWE_URI.get())
            .then()
                .log().all()
                .statusCode(SC_OK)
                .body("cookieName", is(COOKIE))
                .body("token", not(emptyOrNullString()));
            //@formatter:on
        }
    }

    // Negative tests are in ZaasNegativeTest since they are common for the whole service
}
