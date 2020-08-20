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

import lombok.RequiredArgsConstructor;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.hamcrest.core.Is.is;

@RequiredArgsConstructor
public class AuthenticationProviders {
    private final String authenticationEndpointPath;

    protected void switchProvider(String provider) {
        given()
            .contentType(JSON)
            .body("{\"provider\": \"" + provider + "\"}")
        .when()
            .post(authenticationEndpointPath)
        .then()
            .statusCode(is(SC_NO_CONTENT));
    }
}
