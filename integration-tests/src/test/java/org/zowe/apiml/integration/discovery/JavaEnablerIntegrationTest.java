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
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.GatewayTest;
import org.zowe.apiml.util.categories.NotForMainframeTest;
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.is;

/**
 * Test that the Java enabler is properly integrated with the API ML (Discovery, Gateway)
 */
@TestsNotMeantForZowe
@NotForMainframeTest
@GatewayTest
class JavaEnablerIntegrationTest implements TestWithStartedInstances {
    private static final String UI_V1_PATH = "/ui/v1/enablerJavaSampleApp/";
    private static final String APP_INFO_HEALTH = "/api/v1/enablerJavaSampleApp/application/health/";

    private static final String JSON_CONTENT_TYPE = "application/json";

    @BeforeAll
    static void beforeClass() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    void shouldGetUI() {
        URI uri = HttpRequestUtils.getUriFromGateway(UI_V1_PATH);

        given()
        .when()
            .get(uri)
        .then()
            .statusCode(is(SC_OK))
            .contentType(is("text/html;charset=UTF-8"));
    }


    @Test
    void shouldGetHealth() {
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
