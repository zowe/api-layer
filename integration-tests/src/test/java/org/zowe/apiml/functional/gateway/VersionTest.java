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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.zowe.apiml.util.http.HttpRequestUtils;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;

class VersionTest {

    @SuppressWarnings("unused")
    private static String[] versionUrls() {
        return new String[]{
            "/application/version", "/gateway/version", "/gateway/api/v1/version"
        };
    }

    @BeforeEach
    void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Nested
    class GivenNoAuthentication {

        @Nested
        class WhenRequestingVersion {

            @ParameterizedTest(name = "ReturnValidVersion {index} {0} ")
            @MethodSource("org.zowe.apiml.functional.gateway.VersionTest#versionUrls")
            void returnValidVersion(String endpoint) {
                // Gateway request to url
                given()
                .when()
                    .get(HttpRequestUtils.getUriFromGateway(endpoint))
                .then()
                    .statusCode(SC_OK)
                    .body("apiml.version", is(not(nullValue())))
                    .body("apiml.buildNumber", is(not(nullValue())))
                    .body("apiml.commitHash", is(not(nullValue())));
            }
        }
    }
}

