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
import io.restassured.config.RestAssuredConfig;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

@TestsNotMeantForZowe
class CachingApiIntegrationTest {

    private static final URI CACHING_PATH = HttpRequestUtils.getUriFromGateway("/cachingservice/api/v1/cache");

    @BeforeAll
    static void setup() throws Exception {
        SslContext.prepareSslAuthentication();
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    void givenMultipleConcurrentCalls_correctResponseInTheEnd() throws InterruptedException {
        ExecutorService service = Executors.newFixedThreadPool(8);

        AtomicInteger ai = new AtomicInteger(20);
        for (int i = 0; i < 3; i++) {
            service.execute(() -> {
                given().config(SslContext.clientCertValid)
                    .contentType(JSON)
                    .body(new KeyValue(String.valueOf(ai.getAndIncrement()), "someValue"))
                    .when()
                    .post(CACHING_PATH).then().statusCode(201);

            });
        }

        service.shutdown();
        service.awaitTermination(30L, TimeUnit.SECONDS);

        given().config(SslContext.clientCertValid)
            .contentType(JSON)
            .when()
            .get(CACHING_PATH).then().body("20", is(not(isEmptyString())))
            .body("21", is(not(isEmptyString())))
            .body("22", is(not(isEmptyString())))
            .statusCode(200);

        ExecutorService deleteService = Executors.newFixedThreadPool(8);

        AtomicInteger ai2 = new AtomicInteger(20);
        for (int i = 0; i < 3; i++) {
            deleteService.execute(() -> {
                given().config(SslContext.clientCertValid)
                    .contentType(JSON)
                    .when()
                    .delete(CACHING_PATH + "/" + ai2.getAndIncrement());

            });
        }

        deleteService.shutdown();
        deleteService.awaitTermination(30L, TimeUnit.SECONDS);

        given().config(SslContext.clientCertValid)
            .contentType(JSON)
            .when()
            .get(CACHING_PATH).then().body("20", isEmptyOrNullString())
            .body("21", isEmptyOrNullString())
            .body("22", isEmptyOrNullString())
            .statusCode(200);
    }


    @Test
    void givenValidKeyValue_whenCallingCreateEndpoint_thenStoreIt() {
        try {
            KeyValue keyValue = new KeyValue("testKey", "testValue");

            given().config(SslContext.clientCertValid)
                .contentType(JSON)
                .body(keyValue)
                .when()
                .post(CACHING_PATH)
                .then()
                .statusCode(is(SC_CREATED));
        } finally {
            deleteValueUnderServiceIdWithoutValidation("testKey", SslContext.clientCertValid);
        }

    }

    @Test
    void givenEmptyBody_whenCallingCreateEndpoint_thenReturn400() {
        given().config(SslContext.clientCertValid)
            .contentType(JSON)
            .when()
            .post(CACHING_PATH)
            .then()
            .statusCode(is(SC_BAD_REQUEST));
    }

    @Test
    void givenValidKeyParameter_whenCallingGetEndpoint_thenReturnKeyValueEntry() {

        try {
            loadValueUnderServiceId(new KeyValue("testKey", "testValue"), SslContext.clientCertValid);

            given().config(SslContext.clientCertValid)
                .contentType(JSON)
                .when()
                .get(CACHING_PATH + "/testKey")
                .then()
                .body(not(isEmptyString()))
                .statusCode(is(SC_OK));
        } finally {
            deleteValueUnderServiceIdWithoutValidation("testKey", SslContext.clientCertValid);
        }
    }

    /**
     * This test is testing that records from one user do not leak to select all for other user.
     * The VSAM implementation is dependent on hashcodes of composite key elements.
     * It is important to test both ways, so that both combinations are validated.
     */
    @Test
    void givenValidKeyParameter_whenCallingGetAllEndpoint_thenAllTheStoredEntries() {

        RestAssuredConfig user1 = SslContext.clientCertValid;
        RestAssuredConfig user2 = SslContext.clientCertUser;

        KeyValue keyValue1 = new KeyValue("testKey1", "testValue1");
        KeyValue keyValue2 = new KeyValue("testKey2", "testValue2");
        KeyValue keyValue3 = new KeyValue("testKey3", "testValue3");
        KeyValue keyValue4 = new KeyValue("testKey4", "testValue4");

        try {
            loadValueUnderServiceId(keyValue1, user1);
            loadValueUnderServiceId(keyValue2, user1);

            loadValueUnderServiceId(keyValue3, user2);
            loadValueUnderServiceId(keyValue4, user2);

            given().config(user1)
                .log().uri()
                .contentType(JSON)
                .when()
                .get(CACHING_PATH)
                .then().log().all()
                .body("testKey1", is(not(isEmptyString())),
                    "testKey2", is(not(isEmptyString())),
                    "testKey3", isEmptyOrNullString(),
                    "testKey4", isEmptyOrNullString())
                .statusCode(is(SC_OK));

            given().config(user2)
                .log().uri()
                .contentType(JSON)
                .when()
                .get(CACHING_PATH)
                .then().log().all()
                .body("testKey3", is(not(isEmptyString())),
                    "testKey4", is(not(isEmptyString())),
                    "testKey1", isEmptyOrNullString(),
                    "testKey2", isEmptyOrNullString())
                .statusCode(is(SC_OK));
        } finally {
            deleteValueUnderServiceIdWithoutValidation("testKey1", user1);
            deleteValueUnderServiceIdWithoutValidation("testKey2", user1);
            deleteValueUnderServiceIdWithoutValidation("testKey3", user2);
            deleteValueUnderServiceIdWithoutValidation("testKey4", user2);

            deleteValueUnderServiceIdWithoutValidation("testKey1", user1);
            deleteValueUnderServiceIdWithoutValidation("testKey2", user1);
            deleteValueUnderServiceIdWithoutValidation("testKey3", user2);
            deleteValueUnderServiceIdWithoutValidation("testKey4", user2);
        }
    }

    @Test
    void givenNonExistingKeyParameter_whenCallingGetEndpoint_thenReturnKeyNotFound() {
        given().config(SslContext.clientCertValid)
            .contentType(JSON)
            .when()
            .get(CACHING_PATH + "/invalidKey")
            .then()
            .body(not(isEmptyString()))
            .statusCode(is(SC_NOT_FOUND));
    }

    @Test
    void givenValidKeyParameter_whenCallingUpdateEndpoint_thenReturnUpdateValue() {

        try {
            loadValueUnderServiceId(new KeyValue("testKey", "testValue"), SslContext.clientCertValid);

            KeyValue newValue = new KeyValue("testKey", "newValue");

            given().config(SslContext.clientCertValid)
                .contentType(JSON)
                .body(newValue)
                .when()
                .put(CACHING_PATH)
                .then()
                .statusCode(is(SC_NO_CONTENT));

            given().config(SslContext.clientCertValid)
                .contentType(JSON)
                .when()
                .get(CACHING_PATH + "/testKey")
                .then()
                .body("value", Matchers.is("newValue"))
                .statusCode(is(SC_OK));
        } finally {
            deleteValueUnderServiceIdWithoutValidation("testKey", SslContext.clientCertValid);
        }
    }

    @Test
    void givenValidKeyParameter_whenCallingDeleteEndpoint_thenDeleteKeyValueFromStore() {

        try {
            loadValueUnderServiceId(new KeyValue("testKey", "testValue"), SslContext.clientCertValid);

            given().config(SslContext.clientCertValid)
                .contentType(JSON)
                .when()
                .delete(CACHING_PATH + "/testKey")
                .then()
                .statusCode(is(SC_NO_CONTENT));

            given().config(SslContext.clientCertValid)
                .contentType(JSON)
                .when()
                .get(CACHING_PATH + "/testKey")
                .then()
                .statusCode(is(SC_NOT_FOUND));
        } finally {
            deleteValueUnderServiceIdWithoutValidation("testkey", SslContext.clientCertValid);
        }

    }

    @Test
    void givenInvalidParameter_whenCallingDeleteEndpoint_thenNotFound() {
        given().config(SslContext.clientCertValid)
            .contentType(JSON)
            .when()
            .delete(CACHING_PATH + "/invalidKey")
            .then()
            .statusCode(is(SC_NOT_FOUND));
    }

    private static void loadValueUnderServiceId(KeyValue value, RestAssuredConfig config) {
        given().config(config)
            .contentType(JSON)
            .body(value)
            .when()
            .post(CACHING_PATH)
            .then()
            .statusCode(is(SC_CREATED));
    }

    private static void deleteValueUnderServiceIdWithoutValidation(String value, RestAssuredConfig config) {
        given().config(config)
            .contentType(JSON)
            .when()
            .delete(CACHING_PATH + "/" + value);
    }
}
