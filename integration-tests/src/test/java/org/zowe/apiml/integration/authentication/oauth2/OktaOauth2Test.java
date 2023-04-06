/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.integration.authentication.oauth2;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.integration.authentication.pat.ValidateRequestModel;
import org.zowe.apiml.util.SecurityUtils;
import org.zowe.apiml.util.http.HttpRequestUtils;
import org.zowe.apiml.util.requests.Endpoints;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.zowe.apiml.util.requests.Endpoints.*;

@Tag("OktaOauth2Test")
public class OktaOauth2Test {

    public static final URI VALIDATE_ENDPOINT = HttpRequestUtils.getUriFromGateway(Endpoints.VALIDATE_OIDC_TOKEN);
    public static final URI PASS_TICKET_REQUEST_ENDPOINT = HttpRequestUtils.getUriFromGateway(REQUEST_INFO_ENDPOINT);
    public static final URI ZOWE_JWT_REQUEST_ENDPOINT = HttpRequestUtils.getUriFromGateway(ZOWE_JWT_REQUEST);
    public static final URI SAF_IDT_REQUEST_ENDPOINT = HttpRequestUtils.getUriFromGateway(SAF_IDT_REQUEST);
    public static final URI ZOSMF_REQUEST_ENDPOINT = HttpRequestUtils.getUriFromGateway("/discoverableclient/api/v1/request");

    @Nested
    class GivenValidOktaToken {
        private final String token = SecurityUtils.validOktaAccessToken();

        @Test
        void thenValidateReturns200() {
            ValidateRequestModel requestBody = new ValidateRequestModel();
            requestBody.setToken(token);
            RestAssured.useRelaxedHTTPSValidation();
            given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post(VALIDATE_ENDPOINT)
                .then().statusCode(HttpStatus.SC_OK);
        }
    }

    @Nested
    class GivenExpiredOktaToken {
        private final String token = SecurityUtils.expiredOktaAccessToken();

        @Test
        void thenValidateReturns401() {
            ValidateRequestModel requestBody = new ValidateRequestModel();
            requestBody.setToken(token);
            RestAssured.useRelaxedHTTPSValidation();
            given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post(VALIDATE_ENDPOINT)
                .then().statusCode(HttpStatus.SC_UNAUTHORIZED);
        }

        @Test
        void whenPassingToHeader_thenAddFailureHeader() {
            RestAssured.useRelaxedHTTPSValidation();
            given()
                .contentType(ContentType.JSON)
                .header(new Header(ApimlConstants.OIDC_HEADER_NAME, token))
                .when()
                .get(ZOWE_JWT_REQUEST_ENDPOINT)
                .then()
                .body("headers.x-zowe-auth-failure", is("ZWEAG103E The token has expired"))
                .header(ApimlConstants.AUTH_FAIL_HEADER, startsWith("ZWEAG103E The token has expired"))
                .statusCode(200);
        }
    }

    @Nested
    class WhenTestingZoweJwtScheme {
        private final String token = SecurityUtils.validOktaAccessToken();

        @Test
        void givenTokenInHeader_thenReturnInfo() {
            RestAssured.useRelaxedHTTPSValidation();

            given()
                .contentType(ContentType.JSON)
                .header(new Header(ApimlConstants.OIDC_HEADER_NAME, token))
                .when()
                .get(ZOWE_JWT_REQUEST_ENDPOINT)
                .then()
                .body("headers.cookie", isNotNull())
                .body("cookies.apimlAuthenticationToken", isNotNull())
                .statusCode(200);
        }
    }

    @Nested
    class WhenTestingZosmfScheme {
        private final String token = SecurityUtils.validOktaAccessToken();

        @Test
        void givenTokenInHeader_thenReturnInfo() {
            RestAssured.useRelaxedHTTPSValidation();

            given()
                .contentType(ContentType.JSON)
                .header(new Header(ApimlConstants.OIDC_HEADER_NAME, token))
                .when()
                .get(ZOSMF_REQUEST_ENDPOINT)
                .then()
                .body("headers.cookie", isNotNull())
                .body("cookies.apimlAuthenticationToken", isNotNull())
                .statusCode(200);
        }
    }

    @Nested
    class WhenTestingSafIdtScheme {
        private final String token = SecurityUtils.validOktaAccessToken();

        @Test
        void givenTokenInHeader_thenReturnInfo() {
            RestAssured.useRelaxedHTTPSValidation();

            given()
                .contentType(ContentType.JSON)
                .header(new Header(ApimlConstants.OIDC_HEADER_NAME, token))
                .when()
                .get(SAF_IDT_REQUEST_ENDPOINT)
                .then()
                .body("headers.cookie", isNotNull())
                .body("cookies.apimlAuthenticationToken", isNotNull())
                .statusCode(200);
        }
    }

    @Nested
    class WhenTestingPassticketScheme {
        private final String token = SecurityUtils.validOktaAccessToken();

        @Test
        void givenTokenInHeader_thenReturnInfo() {
            RestAssured.useRelaxedHTTPSValidation();

            given()
                .contentType(ContentType.JSON)
                .header(new Header(ApimlConstants.OIDC_HEADER_NAME, token))
                .when()
                .get(PASS_TICKET_REQUEST_ENDPOINT)
                .then()
                .body("headers.cookie", isNotNull())
                .body("cookies.apimlAuthenticationToken", isNotNull())
                .statusCode(200);
        }
    }

    @Nested
    class WhenProvidingInvalidToken {
        @Test
        void givenTokenInHeader_thenAddFailureHeader() {
            RestAssured.useRelaxedHTTPSValidation();

            given()
                .contentType(ContentType.JSON)
                .header(new Header(ApimlConstants.OIDC_HEADER_NAME, "invalidToken"))
                .when()
                .get(ZOWE_JWT_REQUEST_ENDPOINT)
                .then()
                .body("headers.x-zowe-auth-failure", is("ZWEAG102E Token is not valid"))
                .header(ApimlConstants.AUTH_FAIL_HEADER, startsWith("ZWEAG102E Token is not valid"))
                .statusCode(200);
        }
    }

}
