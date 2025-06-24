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

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@AcceptanceTest
public class ReactivePATTests extends AcceptanceTestWithMockServices {

    private static final String GENERATE_PAT_ENDPOINT = "/gateway/api/v1/auth/access-token/generate";

    @BeforeEach
    void setUp() throws Exception {
        mockZosmfSuccess();
    }

    @Test
    void generatePAT_Success() {
        var token = given()
            .auth().preemptive().basic("USER", "validPassword")
            .body("""
            {
                "scopes": ["gateway"],
                "validity": 90
            }
            """)
            .contentType(ContentType.JSON)
        .when()
            .post(URI.create(basePath + GENERATE_PAT_ENDPOINT))
        .then()
            .statusCode(200)
        .extract()
            .body()
            .asString();

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

}
