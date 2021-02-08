/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.enablenodejssampleapp;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.is;

@TestsNotMeantForZowe
public class EnablerNodejsSampleAppEndpointTest {

    private static final String APP_INFO_PATH = "/hwexpress/api/v1/info/";
    private static final String APP_INFO_HEALTH = "/hwexpress/api/v1/status/";
    private static final String JSON_CONTENT_TYPE = "application/json;charset=utf-8";
    private static final String GREETING_PATH = "/hwexpress/api/v1/hello";

    @BeforeAll
    public static void beforeClass() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    void shouldGetApplicationInfo() {
        URI uri = HttpRequestUtils.getUriFromGateway(APP_INFO_PATH);

        given()
            .when()
            .get(uri)
            .then()
            .statusCode(is(SC_OK))
            .contentType(is(JSON_CONTENT_TYPE));
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

    @Test
    void shouldGetGreeting() {
        URI uri = HttpRequestUtils.getUriFromGateway(GREETING_PATH);

        given()
            .when()
            .get(uri)
            .then()
            .statusCode(is(SC_OK))
            .body("greeting", is("Hello World!"));
    }

}
