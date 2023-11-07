package org.zowe.apiml.integration.zaas;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.util.SecurityUtils;
import org.zowe.apiml.util.categories.GeneralAuthenticationTest;
import org.zowe.apiml.util.categories.zOSMFAuthTest;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.core.Is.is;
import static org.zowe.apiml.integration.zaas.ZosmfTokensTest.WhenGeneratingZosmfTokens_returnValidZosmfToken.COOKIE;
import static org.zowe.apiml.integration.zaas.ZosmfTokensTest.ZAAS_ZOSMF_URI;
import static org.zowe.apiml.util.SecurityUtils.generateJwtWithRandomSignature;
import static org.zowe.apiml.util.SecurityUtils.getConfiguredSslConfig;

@zOSMFAuthTest
public class ZaasNegativeTest {

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

        @Test
        void givenIncorrectlySignedZosmfToken() {
            String zoweToken = generateJwtWithRandomSignature(QueryResponse.Source.ZOSMF.value);

            //@formatter:off
            given()
                .header("Authorization", "Bearer " + zoweToken)
            .when()
                .post(ZAAS_ZOSMF_URI)
            .then()
                .statusCode(is(SC_UNAUTHORIZED));
            //@formatter:on
        }

        @Test
        void givenIncorrectlySignedZoweToken() {
            String zoweToken = generateJwtWithRandomSignature(QueryResponse.Source.ZOWE.value);

            //@formatter:off
            given()
                .header("Authorization", "Bearer " + zoweToken)
            .when()
                .post(ZAAS_ZOSMF_URI)
            .then()
                .statusCode(is(SC_UNAUTHORIZED));
            //@formatter:on
        }

        @Test
        void givenIncorrectlySignedAccessToken() {
            String zoweToken = generateJwtWithRandomSignature(QueryResponse.Source.ZOWE_PAT.value);

            //@formatter:off
            given()
                .header("Authorization", "Bearer " + zoweToken)
            .when()
                .post(ZAAS_ZOSMF_URI)
            .then()
                .statusCode(is(SC_UNAUTHORIZED));
            //@formatter:on
        }

        @Test
        @Tag("OktaOauth2Test")
        void givenInvalidOAuthToken() {
            String oAuthToken = generateJwtWithRandomSignature("https://localhost:10010");

            //@formatter:off
            given()
                .header("Authorization", "Bearer " + oAuthToken)
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
