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
import org.junit.jupiter.api.Test;

import java.net.URI;

import static io.restassured.RestAssured.given;

@AcceptanceTest
public class ReactivePATTests extends AcceptanceTestWithMockServices {

    private static final String REFRESH_ENDPOINT = "/gateway/api/v1/auth/refresh";

    @BeforeEach
    void setUp() throws Exception {
        mockZosmfSuccess();
    }

    @Test
    void whenRefreshPATWithoutCert_then403() {
        given()
        .when()
            .post(URI.create(basePath + REFRESH_ENDPOINT))
        .then()
            .statusCode(403);
    }

    @Test
    void whenWrongMethod_thenFail() {
        given()
        .when()
            .get(URI.create(basePath + REFRESH_ENDPOINT))
        .then()
            .statusCode(405);
    }

}
