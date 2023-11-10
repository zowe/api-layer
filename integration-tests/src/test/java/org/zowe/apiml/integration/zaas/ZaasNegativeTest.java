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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.util.SecurityUtils;
import org.zowe.apiml.util.categories.GeneralAuthenticationTest;
import org.zowe.apiml.util.categories.ZaasTest;

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.core.Is.is;
import static org.zowe.apiml.integration.zaas.ZosmfTokensTest.WhenGeneratingZosmfTokens_returnValidZosmfToken.COOKIE;
import static org.zowe.apiml.integration.zaas.ZosmfTokensTest.ZAAS_ZOSMF_URI;
import static org.zowe.apiml.util.SecurityUtils.generateJwtWithRandomSignature;
import static org.zowe.apiml.util.SecurityUtils.getConfiguredSslConfig;

@ZaasTest
public class ZaasNegativeTest {

    private static Stream<Arguments> provideToken() {
        return Stream.of(
            Arguments.of(generateJwtWithRandomSignature(QueryResponse.Source.ZOSMF.value)),
            Arguments.of(generateJwtWithRandomSignature(QueryResponse.Source.ZOWE.value)),
            Arguments.of(generateJwtWithRandomSignature(QueryResponse.Source.ZOWE_PAT.value)),
            Arguments.of(generateJwtWithRandomSignature("https://localhost:10010"))
        );
    }


    @Nested
    @GeneralAuthenticationTest
    class ReturnUnauthorized {

        @BeforeEach
        void setUpCertificateAndToken() {
            RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
        }

        @Test
        void givenNoToken() {
            //@formatter:off
            when()
                .post(ZAAS_ZOSMF_URI)
            .then()
                .statusCode(is(SC_UNAUTHORIZED));
            //@formatter:on
        }

        @ParameterizedTest
        @MethodSource("org.zowe.apiml.integration.zaas.ZaasNegativeTest#provideToken")
        void givenInvalidOAuthToken(String token) {
            //@formatter:off
            given()
                .header("Authorization", "Bearer " + token)
            .when()
                .post(ZAAS_ZOSMF_URI)
            .then()
                .statusCode(is(SC_UNAUTHORIZED));
            //@formatter:on
        }

    }

    @Nested
    @GeneralAuthenticationTest
    class GivenNoCertificate {

        @BeforeEach
        void setUpCertificateAndToken() {
            RestAssured.useRelaxedHTTPSValidation();
        }

        @Test
        void thenReturnUnauthorized() {
            //@formatter:off
            given()
                .cookie(COOKIE, SecurityUtils.gatewayToken())
            .when()
                .post(ZAAS_ZOSMF_URI)
            .then()
                .statusCode(is(SC_UNAUTHORIZED));
            //@formatter:on
        }

    }
}
