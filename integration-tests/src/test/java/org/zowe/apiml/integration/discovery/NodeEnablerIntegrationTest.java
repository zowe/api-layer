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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.zowe.apiml.util.categories.*;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.is;

/**
 * Test that Node.js enabler is properly integrated with the API ML (Discovery, Gateway)
 */
@TestsNotMeantForZowe
@NotForMainframeTest
@GatewayTest
@NotAttlsTest
@NodeEnablerTest
class NodeEnablerIntegrationTest {

    private static final String APP_INFO_HEALTH = "/hwexpress/api/v1/status/";

    @BeforeAll
    public static void beforeClass() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    void givenEnablerIsOnboarded_whenRequestingPublicEndpoint_returnStatus() {
        URI uri = HttpRequestUtils.getUriFromGateway(APP_INFO_HEALTH);

        given()
            .log().all()
        .when()
            .get(uri)
        .then()
            .log().all()
            .statusCode(is(SC_OK))
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body("status", is("UP"));
    }

}
