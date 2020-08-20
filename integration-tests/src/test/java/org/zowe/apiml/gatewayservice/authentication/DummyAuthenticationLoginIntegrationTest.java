/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gatewayservice.authentication;

import io.jsonwebtoken.Claims;
import io.restassured.RestAssured;
import io.restassured.http.Cookie;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.security.common.login.LoginRequest;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;

public class DummyAuthenticationLoginIntegrationTest extends Login {
    @BeforeAll
    static void switchToTestedProvider() {
        RestAssured.port = PORT;
        RestAssured.useRelaxedHTTPSValidation();

        providers.switchProvider("dummy");
    }

    protected String getUsername() {
        return "user";
    }

    protected String getPassword() {
        return "user";
    }

    @Test
    void givenValidCredentialsInBody_whenUserAuthenticatesTwice_thenTwoDifferentValidTokenIsProduced() {
        LoginRequest loginRequest = new LoginRequest(getUsername(), getPassword());

        String jwtToken1 = authenticateAndVerify(loginRequest);
        String jwtToken2 = authenticateAndVerify(loginRequest);

        assertThat(jwtToken1, is(not(jwtToken2)));
    }

    private String authenticateAndVerify(LoginRequest loginRequest) {
        Cookie cookie = given()
            .contentType(JSON)
            .body(loginRequest)
        .when()
            .post(String.format("%s://%s:%d%s%s", SCHEME, HOST, PORT, BASE_PATH, LOGIN_ENDPOINT))
        .then()
            .statusCode(is(SC_NO_CONTENT))
            .cookie(COOKIE_NAME, not(isEmptyString()))
            .extract().detailedCookie(COOKIE_NAME);

        assertThat(cookie.isHttpOnly(), is(true));
        assertThat(cookie.getValue(), is(notNullValue()));
        assertThat(cookie.getMaxAge(), is(-1));

        int i = cookie.getValue().lastIndexOf('.');
        String untrustedJwtString = cookie.getValue().substring(0, i + 1);
        Claims claims = parseJwtString(untrustedJwtString);
        assertThatTokenIsValid(claims);

        return cookie.getValue();
    }
}
