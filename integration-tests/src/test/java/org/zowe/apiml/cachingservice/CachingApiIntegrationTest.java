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
import org.zowe.apiml.util.config.*;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

@TestsNotMeantForZowe
class CachingApiIntegrationTest {

    private static final URI CACHING_PATH = HttpRequestUtils.getUriFromGateway("/cachingservice/api/v1/cache");
    private final static String COOKIE_NAME = "apimlAuthenticationToken";
    private static String jwtToken = SecurityUtils.gatewayToken();
    private final EnvironmentConfiguration environmentConfiguration = ConfigReader.environmentConfiguration();;

    @BeforeAll
    static void setup() {
        RestAssured.useRelaxedHTTPSValidation();
    }


    @Test
    @Disabled
    void givenMultipleConcurrentCalls_correctResponseInTheEnd() throws InterruptedException {
        ExecutorService service = Executors.newFixedThreadPool(8);
        AtomicInteger ai = new AtomicInteger(20);
        for (int i = 0; i < 3; i++) {
            service.execute(() -> {
                given()
                    .contentType(JSON)
                    .cookie(COOKIE_NAME, jwtToken)
                    .body(new KeyValue(String.valueOf(ai.getAndIncrement()), "someValue"))
                    .when()
                    .post(CACHING_PATH).then().statusCode(201);

            });
        }
        given()
            .contentType(JSON)
            .cookie(COOKIE_NAME, jwtToken)
            .when()
            .get(CACHING_PATH).then()
            .statusCode(200);
        AtomicInteger ai2 = new AtomicInteger(20);
        for (int i = 0; i < 3; i++) {
            service.execute(() -> {
                given()
                    .contentType(JSON)
                    .cookie(COOKIE_NAME, jwtToken)
                    .when()
                    .delete(CACHING_PATH + "/" + ai2.getAndIncrement());

            });
        }

        service.shutdown();
        service.awaitTermination(30L, TimeUnit.SECONDS);

        given()
            .contentType(JSON)
            .cookie(COOKIE_NAME, jwtToken)
            .when()
            .get(CACHING_PATH).then().body("20",isEmptyOrNullString())
            .body("21",isEmptyOrNullString())
            .body("22",isEmptyOrNullString())
            .statusCode(200);
    }


    @Test
    void givenValidKeyValue_whenCallingCreateEndpoint_thenStoreIt() {
        try {
            KeyValue keyValue = new KeyValue("testKey", "testValue");

            given()
                .contentType(JSON)
                .body(keyValue)
                .cookie(COOKIE_NAME, jwtToken)
                .when()
                .post(CACHING_PATH)
                .then()
                .statusCode(is(SC_CREATED));
        } finally {
            deteleValueUnderServiceIdWithoutValidation("testKey", jwtToken);
        }

    }

    @Test
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
    void givenValidKeyParameter_whenCallingGetEndpoint_thenReturnKeyValueEntry() {

        try {
            loadValueUnderServiceId(new KeyValue("testKey", "testValue"), jwtToken);

            given()
                .contentType(JSON)
                .cookie(COOKIE_NAME, jwtToken)
                .when()
                .get(CACHING_PATH + "/testKey")
                .then()
                .body(not(isEmptyString()))
                .statusCode(is(SC_OK));
        } finally {
            deteleValueUnderServiceIdWithoutValidation("testKey", jwtToken);
        }
    }

    @Test
    void givenValidKeyParameter_whenCallingGetAllEndpoint_thenAllTheStoredEntries() {

        List<Credentials> testUsers = environmentConfiguration.getAuxiliaryUserList().getCredentials();
        assertThat(testUsers, hasSize(greaterThanOrEqualTo(2)));

        testUsers.sort((o1, o2) -> o1.getUser().hashCode() - o2.getUser().hashCode());
        Credentials testUserLowerHashcode = testUsers.get(0);
        Credentials testUserHigherHashcode = testUsers.get(1);

        System.out.println("TestUserLowerHashcode: " + testUserLowerHashcode.getUser());
        System.out.println("TestUserHigherHashcode: " + testUserHigherHashcode.getUser());

        String jwtToken1 = SecurityUtils.gatewayToken(testUserLowerHashcode.getUser(), testUserLowerHashcode.getPassword());
        String jwtToken2 = SecurityUtils.gatewayToken(testUserHigherHashcode.getUser(), testUserHigherHashcode.getPassword());

        assertThat(jwtToken1, is(not(isEmptyString())));
        assertThat(jwtToken2, is(not(isEmptyString())));

        KeyValue keyValue1 = new KeyValue("testKey1", "testValue1");
        KeyValue keyValue2 = new KeyValue("testKey2", "testValue2");
        KeyValue keyValue3 = new KeyValue("testKey3", "testValue3");
        KeyValue keyValue4 = new KeyValue("testKey4", "testValue4");

        try {
            loadValueUnderServiceId(keyValue1, jwtToken1);
            loadValueUnderServiceId(keyValue2, jwtToken1);

            loadValueUnderServiceId(keyValue3, jwtToken2);
            loadValueUnderServiceId(keyValue4, jwtToken2);

            given().log().uri()
                .contentType(JSON)
                .cookie(COOKIE_NAME, jwtToken1)
                .when()
                .get(CACHING_PATH)
                .then().log().all()
                .body("testKey1", is(not(isEmptyString())),
                    "testKey2", is(not(isEmptyString())),
                    "testKey3", isEmptyOrNullString(),
                    "testKey4", isEmptyOrNullString())
                .statusCode(is(SC_OK));

        } finally {
            deteleValueUnderServiceIdWithoutValidation("testKey1", jwtToken1);
            deteleValueUnderServiceIdWithoutValidation("testKey2", jwtToken1);
            deteleValueUnderServiceIdWithoutValidation("testKey3", jwtToken1);
            deteleValueUnderServiceIdWithoutValidation("testKey4", jwtToken1);

            deteleValueUnderServiceIdWithoutValidation("testKey1", jwtToken2);
            deteleValueUnderServiceIdWithoutValidation("testKey2", jwtToken2);
            deteleValueUnderServiceIdWithoutValidation("testKey3", jwtToken2);
            deteleValueUnderServiceIdWithoutValidation("testKey4", jwtToken2);
        }
    }

    @Test
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
    void givenValidKeyParameter_whenCallingUpdateEndpoint_thenReturnUpdateValue() {

        try {
            loadValueUnderServiceId(new KeyValue("testKey", "testValue"), jwtToken);

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
        } finally {
            deteleValueUnderServiceIdWithoutValidation("testKey", jwtToken);
        }
    }

    @Test
    void givenValidKeyParameter_whenCallingDeleteEndpoint_thenDeleteKeyValueFromStore() {

        try {
            loadValueUnderServiceId(new KeyValue("testKey", "testValue"), jwtToken);

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
        } finally {
            deteleValueUnderServiceIdWithoutValidation("testkey", jwtToken);
        }

    }

    @Test
    void givenInvalidParameter_whenCallingDeleteEndpoint_thenNotFound() {
        given()
            .contentType(JSON)
            .cookie(COOKIE_NAME, jwtToken)
            .when()
            .delete(CACHING_PATH + "/invalidKey")
            .then()
            .statusCode(is(SC_NOT_FOUND));
    }

    private static String generateToken() {
        return SecurityUtils.gatewayToken();
    }

    private static void loadValueUnderServiceId(KeyValue value, String jwtToken) {
        given()
            .contentType(JSON)
            .body(value)
            .cookie(COOKIE_NAME, jwtToken)
            .when()
            .post(CACHING_PATH)
            .then()
            .statusCode(is(SC_CREATED));
    }

    private static void deteleValueUnderServiceIdWithoutValidation(String value, String jwtToken) {
        given()
            .contentType(JSON)
            .cookie(COOKIE_NAME, jwtToken)
            .when()
            .delete(CACHING_PATH + "/" + value);
    }
}
