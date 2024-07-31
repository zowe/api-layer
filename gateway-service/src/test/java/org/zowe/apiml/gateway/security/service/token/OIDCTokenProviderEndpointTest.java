/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.service.token;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;
import static org.zowe.apiml.constants.ApimlConstants.BEARER_AUTHENTICATION_PREFIX;

@SpringBootTest(
    properties = {
        "apiml.security.oidc.validationType=endpoint",
        "apiml.security.oidc.enabled=true",
        "apiml.security.oidc.userInfo.uri=https://localhost:/user/info"
    }
)
@Import(OIDCTokenProviderEndpointTest.OidcProviderTestController.class)
class OIDCTokenProviderEndpointTest {

    private final String VALID_TOKEN = "xyz";
    private final String INVALID_TOKEN = "xyz";

    @Test
    void givenValidToken_thenAccessEndpoint() {
        given()
            .header(HttpHeaders.AUTHORIZATION, BEARER_AUTHENTICATION_PREFIX + " " + VALID_TOKEN)
        .when()
            .get("/gateway/api/v1/RESTRICTED")
        .then()
            .statusCode(200);
    }

    @Test
    void givenInvalidToken_thenRejectAccessToEndpoint() {
        given()
            .header(HttpHeaders.AUTHORIZATION, BEARER_AUTHENTICATION_PREFIX + " " + INVALID_TOKEN)
        .when()
            .get("/gateway/api/v1/RESTRICTED")
        .then()
            .statusCode(401);
    }

    @Controller
    class OidcProviderTestController {

        @GetMapping("/user/info")
        ResponseEntity<String> verify(@RequestHeader(HttpHeaders.AUTHORIZATION) String authenticationHeader) {
            if (StringUtils.equals(BEARER_AUTHENTICATION_PREFIX + " " + VALID_TOKEN, authenticationHeader)) {
                return ResponseEntity.ok("{\"detail\":\"information\")");
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"error\":\"message\")");
        }

    }

}