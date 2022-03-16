/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.functional.gateway;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.SecurityUtils;
import org.zowe.apiml.util.categories.GeneralAuthenticationTest;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.http.HttpRequestUtils;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.core.Is.is;

@GeneralAuthenticationTest
public class GatewayAuthenticationTest {

    private final static String PASSWORD = ConfigReader.environmentConfiguration().getCredentials().getPassword();
    private final static String USERNAME = ConfigReader.environmentConfiguration().getCredentials().getUser();
    private static final String ACTUATOR_ENDPOINT = "/application";

    @BeforeEach
    void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Nested
    class GivenBearerAuthentication {
        @Nested
        class WhenAccessingProtectedEndpoint {
            @Test
            void thenAuthenticate() {
                String token = SecurityUtils.gatewayToken(USERNAME, PASSWORD);
                // Gateway request to url
                given()
                    .header("Authorization", "Bearer " + token)
                    .when()
                    .get(HttpRequestUtils.getUriFromGateway(ACTUATOR_ENDPOINT))
                    .then()
                    .statusCode(is(SC_OK));
            }
        }
    }

    @Nested
    class GivenInvalidBearerAuthentication {
        @Nested
        class WhenAccessingProtectedEndpoint {
            @Test
            void thenReturnUnauthorized() {
                // Gateway request to url
                given()
                    .header("Authorization", "Bearer invalidToken")
                    .when()
                    .get(HttpRequestUtils.getUriFromGateway(ACTUATOR_ENDPOINT))
                    .then()
                    .statusCode(is(SC_UNAUTHORIZED));
            }
        }
    }
}
