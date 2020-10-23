/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cachingservice;
import io.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.*;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.gatewayservice.SecurityUtils;
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

@TestsNotMeantForZowe
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CachingApiIntegrationTest {

    private static final URI CACHING_PATH = HttpRequestUtils.getUriFromGateway("/cachingservice/api/v1/cache");
    private final static String COOKIE_NAME = "apimlAuthenticationToken";
    private static String jwtToken;

    @BeforeAll
    static void setup() {
        RestAssured.useRelaxedHTTPSValidation();
        jwtToken = generateToken();
    }

    @Test
    @Order(1)
    void givenValidKeyValue_whenCallingCreateEndpoint_thenStoreIt() {
        KeyValue keyValue = new KeyValue("testKey", "testValue");

        given()
            .contentType(JSON)
            .body(keyValue)
            .cookie(COOKIE_NAME, jwtToken)
            .when()
            .post(CACHING_PATH)
            .then()
            .statusCode(is(SC_CREATED));
    }

    @Test
    @Order(2)
    void givenEmptyBody_whenCallingCreateEndpoint_thenReturn400() {
        given()
            .contentType(JSON)
            .cookie(COOKIE_NAME, jwtToken)
            .when()
            .post(CACHING_PATH)
            .then()
            .statusCode(is(SC_BAD_REQUEST));
    }

    @Test
    @Order(3)
    void givenValidKeyParameter_whenCallingGetEndpoint_thenReturnKeyValueEntry() {
        given()
            .contentType(JSON)
            .cookie(COOKIE_NAME, jwtToken)
            .when()
            .get(CACHING_PATH + "/testKey")
            .then()
            .body(not(isEmptyString()))
            .statusCode(is(SC_OK));
    }

    @Test
    @Order(4)
    void givenValidKeyParameter_whenCallingGetAllEndpoint_thenAllTheStoredEntries() {
        KeyValue keyValue = new KeyValue("testKey2", "testValue2");

        given()
            .contentType(JSON)
            .body(keyValue)
            .cookie(COOKIE_NAME, jwtToken)
            .when()
            .post(CACHING_PATH)
            .then()
            .statusCode(is(SC_CREATED));

        given()
            .contentType(JSON)
            .cookie(COOKIE_NAME, jwtToken)
            .when()
            .get(CACHING_PATH)
            .then()
            .body("testKey", Matchers.is(not(isEmptyString())),
                "testKey2", Matchers.is(not(isEmptyString())))
            .statusCode(is(SC_OK));
    }

    @Test
    @Order(5)
    void givenNonExistingKeyParameter_whenCallingGetEndpoint_thenReturnKeyNotFound() {
        given()
            .contentType(JSON)
            .cookie(COOKIE_NAME, jwtToken)
            .when()
            .get(CACHING_PATH + "/invalidKey")
            .then()
            .body(not(isEmptyString()))
            .statusCode(is(SC_NOT_FOUND));
    }

    @Test
    @Order(6)
    void givenValidKeyParameter_whenCallingUpdateEndpoint_thenReturnUpdateValue() {
        KeyValue newValue = new KeyValue("testKey", "newValue");

        given()
            .contentType(JSON)
            .body(newValue)
            .cookie(COOKIE_NAME, jwtToken)
            .when()
            .put(CACHING_PATH)
            .then()
            .statusCode(is(SC_NO_CONTENT));

        given()
            .contentType(JSON)
            .cookie(COOKIE_NAME, jwtToken)
            .when()
            .get(CACHING_PATH + "/testKey")
            .then()
            .body("value", Matchers.is("newValue"))
            .statusCode(is(SC_OK));
    }

    @Test
    @Order(7)
    void givenValidKeyParameter_whenCallingDeleteEndpoint_thenDeleteKeyValueFromStore() {
        given()
            .contentType(JSON)
            .cookie(COOKIE_NAME, jwtToken)
            .when()
            .delete(CACHING_PATH + "/testKey")
            .then()
            .statusCode(is(SC_NO_CONTENT));

        given()
            .contentType(JSON)
            .cookie(COOKIE_NAME, jwtToken)
            .when()
            .get(CACHING_PATH + "/testKey")
            .then()
            .statusCode(is(SC_NOT_FOUND));
    }

    private static String generateToken() {
        return SecurityUtils.gatewayToken();
    }
}
