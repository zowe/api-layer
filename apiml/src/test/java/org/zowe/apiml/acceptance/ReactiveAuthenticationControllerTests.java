/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.acceptance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.gateway.MockService;

import java.net.URI;

import static io.restassured.RestAssured.given;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.net.httpserver.Headers;

@AcceptanceTest
class ReactiveAuthenticationControllerTests extends AcceptanceTestWithMockServices {

    @BeforeEach
    void setUp() throws JsonProcessingException {
        mockAuthSuccess();
    }

    @Nested
    class GivenAuthenticationController {

        @Nested
        class LoginTest {

            @Nested
            class SuccessTest {

                @BeforeEach
                void setUp() throws JsonProcessingException {
                    mockAuthSuccess();
                }

                @Test
                void whenLoginWithBody_thenSuccess() {
                    given()
                        .body("""
                            {
                                "username": "USER",
                                "password": "validPassword"
                            }
                        """)
                    .when()
                        .post(URI.create(basePath + "/gateway/api/v1/auth/login"))
                    .then()
                        .cookie("apimlAuthenticationToken")
                        .statusCode(204);
                }

            }

        }

    }

    private void mockAuthSuccess() throws JsonProcessingException {
        var headers = new Headers();
        headers.add("Set-Cookie", "jwtToken=jwt");
        headers.add("Set-Cookie", "LtpaToken2=ltpatoken");

        mockService("zosmf").scope(MockService.Scope.TEST)
            .addEndpoint("/zosmf/info")
                .responseCode(200)
                .contentType("application/json")
                .headers(headers)
                .bodyJson("{\"zosmf_version\":\"29\",\"zosmf_saf_realm\":\"SAFRealm\",\"zosmf_full_version\":\"29.0\"}")
        .and()
            .start();
    }

}
