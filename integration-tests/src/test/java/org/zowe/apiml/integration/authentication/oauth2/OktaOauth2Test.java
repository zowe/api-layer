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
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.hasKey;
import static org.zowe.apiml.util.requests.Endpoints.*;

@Tag("OktaOauth2Test")
public class OktaOauth2Test {

    public static final URI VALIDATE_ENDPOINT = HttpRequestUtils.getUriFromGateway(Endpoints.VALIDATE_OIDC_TOKEN);
    public static final URI PASS_TICKET_REQUEST_ENDPOINT = HttpRequestUtils.getUriFromGateway(REQUEST_INFO_ENDPOINT);
    public static final URI ZOWE_JWT_REQUEST_ENDPOINT = HttpRequestUtils.getUriFromGateway(ZOWE_JWT_REQUEST);
    public static final URI SAF_IDT_REQUEST_ENDPOINT = HttpRequestUtils.getUriFromGateway(SAF_IDT_REQUEST);
    public static final URI ZOSMF_REQUEST_ENDPOINT = HttpRequestUtils.getUriFromGateway(ZOSMF_REQUEST);

    private final String validToken = SecurityUtils.validOktaAccessToken();
    private final String expiredToken = SecurityUtils.expiredOktaAccessToken();

    @Nested
    class GivenValidOktaToken {

        @Test
        void thenValidateReturns200() {
            ValidateRequestModel requestBody = new ValidateRequestModel();
            requestBody.setToken(validToken);
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

        @Test
        void thenValidateReturns401() {
            ValidateRequestModel requestBody = new ValidateRequestModel();
            requestBody.setToken(expiredToken);
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
                .header(new Header(ApimlConstants.OIDC_HEADER_NAME, expiredToken))
                .when()
                .get(ZOWE_JWT_REQUEST_ENDPOINT)
                .then().statusCode(200)
                .body("headers", hasKey("x-zowe-auth-failure"))
                .body("headers.x-zowe-auth-failure", is("ZWEAG103E The token has expired"))
                .header(ApimlConstants.AUTH_FAIL_HEADER, startsWith("ZWEAG103E The token has expired"));
        }
    }

    @Nested
    class WhenTestingZoweJwtScheme {

        @Test
        void givenTokenInHeader_thenReturnInfo() {
            RestAssured.useRelaxedHTTPSValidation();

            given()
                .contentType(ContentType.JSON)
                .header(new Header(ApimlConstants.OIDC_HEADER_NAME, validToken))
                .when()
                .get(ZOWE_JWT_REQUEST_ENDPOINT)
                .then().statusCode(200)
                .body("headers", not(hasKey("x-zowe-auth-failure")))
                .body("headers", hasKey("cookie"))
                .body("cookies", hasKey("apimlAuthenticationToken"));
        }
    }

    @Nested
    class WhenTestingZosmfScheme {

        @Test
        void givenTokenInHeader_thenReturnInfo() {
            RestAssured.useRelaxedHTTPSValidation();

            given()
                .contentType(ContentType.JSON)
                .header(new Header(ApimlConstants.OIDC_HEADER_NAME, validToken))
                .when()
                .get(ZOSMF_REQUEST_ENDPOINT)
                .then().statusCode(200)
                .body("headers", not(hasKey("x-zowe-auth-failure")))
                .body("headers", hasKey("cookie"))
                .body("cookies", hasKey("jwtToken"));
        }
    }

    @Nested
    class WhenTestingSafIdtScheme {

        @Test
        void givenTokenInHeader_thenReturnInfo() {
            RestAssured.useRelaxedHTTPSValidation();

            given()
                .contentType(ContentType.JSON)
                .header(new Header(ApimlConstants.OIDC_HEADER_NAME, validToken))
                .when()
                .get(SAF_IDT_REQUEST_ENDPOINT)
                .then().statusCode(200)
                .body("headers", not(hasKey("x-zowe-auth-failure")))
                .body("headers", hasKey("x-saf-token"));
        }
    }

    @Nested
    class WhenTestingPassticketScheme {

        @Test
        void givenTokenInHeader_thenReturnInfo() {
            RestAssured.useRelaxedHTTPSValidation();

            given()
                .contentType(ContentType.JSON)
                .header(new Header(ApimlConstants.OIDC_HEADER_NAME, validToken))
                .when()
                .get(PASS_TICKET_REQUEST_ENDPOINT)
                .then().statusCode(200)
                .body("headers", not(hasKey("x-zowe-auth-failure")))
                .body("headers", hasKey("authorization"))
                .body("headers.authorization", startsWith("Basic"));
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
                .then().statusCode(200)
                .body("headers", hasKey("x-zowe-auth-failure"))
                .body("headers.x-zowe-auth-failure", is("ZWEAG102E Token is not valid"))
                .header(ApimlConstants.AUTH_FAIL_HEADER, startsWith("ZWEAG102E Token is not valid"));
        }
    }

}
