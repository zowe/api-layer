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
import org.zowe.apiml.gatewayservice.SecurityUtils;

import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.hamcrest.core.Is.is;

@RequiredArgsConstructor
public class AuthenticationProviders {

    public AuthenticationProviders(String path, String... ports) {
        for (String port : ports) {
            authenticationEndpointPathList.add(SecurityUtils.getGatewayUrl(path, Integer.parseInt(port)));
        }
    }

    private final List<String> authenticationEndpointPathList = new ArrayList<>();

    protected void switchProvider(String provider) {
        authenticationEndpointPathList.forEach(authenticationEndpointPath -> {
            given()
                .contentType(JSON)
                .body("{\"provider\": \"" + provider + "\"}")
                .when()
                .post(authenticationEndpointPath)
                .then()
                .statusCode(is(SC_NO_CONTENT));
        });
    }
}
