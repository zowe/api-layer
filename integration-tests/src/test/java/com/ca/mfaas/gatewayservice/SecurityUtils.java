/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gatewayservice;



import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

public class SecurityUtils {
    private final static String TOKEN = "apimlAuthenticationToken";
    private final static String LOGIN_ENDPOINT = "/auth/login";


    public static String gatewayToken(String username, String password) {
        GatewayLoginRequest loginRequest = new GatewayLoginRequest(username, password);

        String token = given()
            .contentType(JSON)
            .body(loginRequest)
            .when()
            .post(LOGIN_ENDPOINT)
            .then()
            .statusCode(is(SC_OK))
            .cookie(TOKEN, not(isEmptyString()))
            .body(
                TOKEN, not(isEmptyString())
            )
            .extract().
                path(TOKEN);

        return token;
    }
}
