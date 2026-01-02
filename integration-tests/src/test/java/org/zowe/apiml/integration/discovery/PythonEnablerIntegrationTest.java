/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.integration.discovery;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.categories.*;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.is;

/**
 * Test that Python enabler is properly integrated with the API ML (Discovery, Gateway)
 */
@TestsNotMeantForZowe
@NotForMainframeTest
@GatewayTest
@NotAttlsTest
@PythonEnablerTest
class PythonEnablerIntegrationTest {

    private static final String APP_INFO_HEALTH = "/pythonservice/api/v1/application/health";

    @BeforeAll
    public static void beforeClass() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    void givenEnablerIsOnboarded_whenRequestingPublicEndpoint_returnStatus() {
        URI uri = HttpRequestUtils.getUriFromGateway(APP_INFO_HEALTH);

        given()
        .when()
            .get(uri)
        .then()
            .statusCode(is(SC_OK))
            .contentType(ContentType.JSON)
            .body("status", is("UP"));
    }

}
