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
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.operator.OperatorCreationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.X509Test;
import org.zowe.apiml.util.categories.zOSMFAuthTest;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.io.IOException;
import java.net.URI;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.zowe.apiml.util.SecurityUtils.*;
import static org.zowe.apiml.util.requests.Endpoints.ZAAS_ZOSMF_ENDPOINT;

@Slf4j
@zOSMFAuthTest
class ZosmfTokensTest implements TestWithStartedInstances {

    static final URI ZAAS_ZOSMF_URI = HttpRequestUtils.getUriFromGateway(ZAAS_ZOSMF_ENDPOINT);

    private static Stream<Arguments> provideClientCertificates() throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, NoSuchProviderException, OperatorCreationException {
        return Stream.of(
            Arguments.of(getClientCertificate()),
            Arguments.of(getDummyClientCertificate())
        );
    }

    @Nested
    class WhenGeneratingZosmfTokens_returnValidZosmfToken {

        final static String COOKIE = "apimlAuthenticationToken";
        private final static String LTPA_COOKIE = "LtpaToken2";
        private final static String JWT_COOKIE = "jwtToken";


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
                    .post(ZAAS_ZOSMF_URI)
                .then()
                    .statusCode(is(SC_OK))
                    .body("cookieName", is(JWT_COOKIE))
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
                    .post(ZAAS_ZOSMF_URI)
                .then()
                    .statusCode(is(SC_OK))
                    .body("cookieName", is(LTPA_COOKIE))
                    .body("token", is(ltpaToken));
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
                    .post(ZAAS_ZOSMF_URI)
                .then()
                    .statusCode(is(SC_OK))
                    .body("cookieName", is(JWT_COOKIE))
                    .body("token", not(""));
                //@formatter:on
        }

        @X509Test
        @ParameterizedTest
        @MethodSource("org.zowe.apiml.integration.zaas.ZosmfTokensTest#provideClientCertificates")
        void givenX509Certificate(String certificate) {
            //@formatter:off
                given()
                    .header("Client-Cert", certificate)
                .when()
                    .post(ZAAS_ZOSMF_URI)
                .then()
                    .statusCode(is(SC_OK))
                    .body("cookieName", is(JWT_COOKIE))
                    .body("token", not(""));
                //@formatter:on
        }

        @Test
        @Tag("OktaOauth2Test")
        void givenValidOAuthToken() {
            String oAuthToken = validOktaAccessToken(true);

            //@formatter:off
                given()
                    .cookie(COOKIE, oAuthToken)
                .when()
                    .log().all()
                    .post(ZAAS_ZOSMF_URI)
                .then()
                    .statusCode(is(SC_OK))
                    .body("cookieName", is(JWT_COOKIE))
                    .body("token", not(""));
                //@formatter:on
        }
    }

    // Negative tests are in ZaasNegativeTest since they are common for the whole component
}
