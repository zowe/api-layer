/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.acceptance.requests;

import io.restassured.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.zowe.apiml.security.common.login.LoginRequest;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.hamcrest.core.Is.is;
import static org.zowe.apiml.security.SecurityUtils.COOKIE_AUTH_NAME;

@RequiredArgsConstructor
public class SecurityRequests {
    private final String basePath;

    public Cookie validJwtToken() {
        LoginRequest loginRequest = new LoginRequest("user", "user".toCharArray());

        return given()
            .contentType(JSON)
            .body(loginRequest)
        .when()
            .post(basePath + "/zaas/api/v1/auth/login")
        .then()
            .statusCode(is(SC_NO_CONTENT))
            .extract()
            .detailedCookie(COOKIE_AUTH_NAME);
    }
}
