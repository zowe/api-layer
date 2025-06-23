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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static io.restassured.RestAssured.given;

@AcceptanceTest
class ReactiveAuthenticationControllerTests extends AcceptanceTestWithMockServices {

    @BeforeEach
    void setUp() throws JsonProcessingException {
        mockZosmfSuccess();
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
            .log().all()
        .when()
            .post(URI.create(basePath + "/gateway/api/v1/auth/login"))
        .then()
            .statusCode(204)
            .cookie("apimlAuthenticationToken");
    }

}
