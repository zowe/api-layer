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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.GatewayTest;
import org.zowe.apiml.util.categories.NotAttlsTest;
import org.zowe.apiml.util.categories.NotForMainframeTest;
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;
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
@Disabled
class NodeEnablerIntegrationTest implements TestWithStartedInstances {

    private static final String APP_INFO_HEALTH = "/hwexpress/api/v1/status/";
    private static final String JSON_CONTENT_TYPE = "application/json;charset=utf-8";

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
            .contentType(is(JSON_CONTENT_TYPE))
            .body("status", is("UP"));
    }

}
