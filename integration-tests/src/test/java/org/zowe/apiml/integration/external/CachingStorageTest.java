/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.integration.external;

import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.zowe.apiml.util.KeyValue;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.CachingServiceTest;
import org.zowe.apiml.util.categories.InfinispanStorageTest;
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;
import org.zowe.apiml.util.config.ItSslConfigFactory;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.zowe.apiml.util.requests.Endpoints.CACHING_CACHE;
import static org.zowe.apiml.util.requests.Endpoints.CACHING_CACHE_LIST;

/**
 * This test is verifying integration with the different Storage mechanisms for the Caching service. If these tests pass,
 * we are ok with the stability and quality of the integration.
 * <p>
 * As such we need to run this suite against the different implemented storage mechanisms.
 */
@TestsNotMeantForZowe
@CachingServiceTest
class CachingStorageTest implements TestWithStartedInstances {

    private static final URI CACHING_PATH = HttpRequestUtils.getUriFromGateway(CACHING_CACHE);
    private static final URI CACHING_INVALIDATE_TOKEN_PATH = HttpRequestUtils.getUriFromGateway(CACHING_CACHE_LIST);
    private static final String SPECIFIC_SERVICE_HEADER = "X-CS-Service-ID";

    @BeforeAll
    static void setup() throws Exception {
        SslContext.prepareSslAuthentication(ItSslConfigFactory.integrationTests());
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    void givenMultipleConcurrentCalls_correctResponseInTheEnd() throws InterruptedException {
        ExecutorService service = Executors.newFixedThreadPool(3);

        AtomicInteger ai = new AtomicInteger(20);
        for (int i = 0; i < 3; i++) {
            service.execute(() -> given().config(SslContext.clientCertValid)
                .contentType(JSON)
                .body(new KeyValue(String.valueOf(ai.getAndIncrement()), "someValue"))
                .when()
                .post(CACHING_PATH).then().statusCode(201));
        }

        service.shutdown();
        service.awaitTermination(30L, TimeUnit.SECONDS);

        given().config(SslContext.clientCertValid)
            .contentType(JSON)
            .when()
            .get(CACHING_PATH).then().body("20", is(not(is(emptyString()))))
            .body("21", is(not(is(emptyString()))))
            .body("22", is(not(is(emptyString()))))
            .statusCode(200);

        ExecutorService deleteService = Executors.newFixedThreadPool(8);

        AtomicInteger ai2 = new AtomicInteger(20);
        for (int i = 0; i < 3; i++) {
            deleteService.execute(() -> given().config(SslContext.clientCertValid)
                .contentType(JSON)
                .when()
                .delete(CACHING_PATH + "/" + ai2.getAndIncrement()));
        }

        deleteService.shutdown();
        deleteService.awaitTermination(30L, TimeUnit.SECONDS);

        given().config(SslContext.clientCertValid)
            .contentType(JSON)
            .when()
            .get(CACHING_PATH).then().body("20", is(emptyOrNullString()))
            .body("21", is(emptyOrNullString()))
            .body("22", is(emptyOrNullString()))
            .statusCode(200);
    }

    @Test
    @InfinispanStorageTest
    void givenMultipleUpdates_correctResultReturned() throws InterruptedException {
        ExecutorService service = Executors.newFixedThreadPool(3);

        AtomicInteger ai = new AtomicInteger(20);
        for (int i = 0; i < 3; i++) {
            int index = i;
            service.execute(() -> given().config(SslContext.clientCertApiml)
                .contentType(JSON)
                .body(new KeyValue("testTokens" + index, String.valueOf(ai.getAndIncrement())))
                .when()
                .post(CACHING_INVALIDATE_TOKEN_PATH + "/invalidTokens").then().statusCode(201));
        }
        service.shutdown();
        service.awaitTermination(30L, TimeUnit.SECONDS);
        given().config(SslContext.clientCertApiml)
            .contentType(JSON)
            .when()
            .get(CACHING_INVALIDATE_TOKEN_PATH + "/invalidTokens")
            .then()
            .statusCode(200)
            .body("testTokens0", is(not(is(emptyOrNullString()))))
            .body("testTokens1", is(not(is(emptyOrNullString()))))
            .body("testTokens2", is(not(is(emptyOrNullString()))));
    }

    @Test
    @InfinispanStorageTest
    void givenDuplicateKey_entryIsUpdated() {
        ExecutorService service = Executors.newFixedThreadPool(2);
        for (int i = 0; i < 2; i++) {
            int index = i;
            service.execute(() ->  given().config(SslContext.clientCertApiml)
                .contentType(JSON)
                .body(new KeyValue("testTokens4", "duplicateToken" + index))
                .when()
                .post(CACHING_INVALIDATE_TOKEN_PATH + "/invalidTokens").then().statusCode(HttpStatus.CREATED.value()));
        }
        service.shutdown();
    }

    @Test
    @InfinispanStorageTest
    void givenUntrustedClientCert_returnForbidden() {
        given().config(SslContext.selfSignedUntrusted)
            .contentType(JSON)
            .when()
            .get(CACHING_INVALIDATE_TOKEN_PATH + "/invalidTokens").then().statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @InfinispanStorageTest
    void givenTokensAndRules_correctResultReturned() throws InterruptedException {
        ExecutorService service = Executors.newFixedThreadPool(3);

        AtomicInteger ai = new AtomicInteger(20);
        for (int i = 0; i < 3; i++) {
            int index = i;
            service.execute(() -> given().config(SslContext.clientCertApiml)
                .contentType(JSON)
                .body(new KeyValue("hashed_token" + index, String.valueOf(ai.getAndIncrement())))
                .when()
                .post(CACHING_INVALIDATE_TOKEN_PATH + "/invalidTokens").then().statusCode(201));
        }
        for (int i = 0; i < 3; i++) {
            int index = i;
            service.execute(() -> given().config(SslContext.clientCertApiml)
                .contentType(JSON)
                .body(new KeyValue("hashed_rule" + index, String.valueOf(ai.getAndIncrement())))
                .when()
                .post(CACHING_INVALIDATE_TOKEN_PATH + "/invalidTokenRules").then().statusCode(201));
        }
        service.shutdown();
        service.awaitTermination(30L, TimeUnit.SECONDS);
        // get all tokens and rules at once
        given().config(SslContext.clientCertApiml)
            .contentType(JSON)
            .when()
            .get(CACHING_INVALIDATE_TOKEN_PATH)
            .then()
            .statusCode(200)
            .body("invalidTokens", is(not(is(emptyOrNullString()))))
            .body("invalidTokenRules", is(not(is(emptyOrNullString()))))
            .body("invalidTokens.hashed_token0", is(not(is(emptyOrNullString()))))
            .body("invalidTokens.hashed_token1", is(not(is(emptyOrNullString()))))
            .body("invalidTokens.hashed_token2", is(not(is(emptyOrNullString()))))
            .body("invalidTokenRules.hashed_rule0", is(not(is(emptyOrNullString()))))
            .body("invalidTokenRules.hashed_rule1", is(not(is(emptyOrNullString()))))
            .body("invalidTokenRules.hashed_rule2", is(not(is(emptyOrNullString()))));
    }

    @Nested
    class WhenCreatingKey {
        @Nested
        class ReturnCreated {
            @Test
            void givenValidKeyValue() {
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
        }

        @Nested
        class ReturnBadRequest {
            @Test
            void givenEmptyBody() {
                given().config(SslContext.clientCertValid)
                    .contentType(JSON)
                    .when()
                    .post(CACHING_PATH)
                    .then()
                    .statusCode(is(SC_BAD_REQUEST));
            }
        }
    }

    @Nested
    class WhenGettingValue {
        @Nested
        class ReturnKeyValueEntry {
            @Test
            void givenValidKeyParameter() {

                try {
                    loadValueUnderServiceId(new KeyValue("testKey", "testValue"), SslContext.clientCertValid);

                    given().config(SslContext.clientCertValid)
                        .contentType(JSON)
                        .when()
                        .get(CACHING_PATH + "/testKey")
                        .then()
                        .body(not(is(emptyString())))
                        .statusCode(is(SC_OK));
                } finally {
                    deleteValueUnderServiceIdWithoutValidation("testKey", SslContext.clientCertValid);
                }
            }
        }

        @Nested
        class ReturnKeyNotFound {
            @Test
            void givenNonExistingKeyParameter() {
                given().config(SslContext.clientCertValid)
                    .contentType(JSON)
                    .when()
                    .get(CACHING_PATH + "/invalidKey")
                    .then()
                    .body(not(is(emptyString())))
                    .statusCode(is(SC_NOT_FOUND));
            }
        }
    }

    @Nested
    class WhenGettingAllForService {
        @Nested
        class ReturnAllStoredEntriesForSpecificService {
            /**
             * This test is testing that records from one user do not leak to select all for other user.
             * The VSAM implementation is dependent on hashcodes of composite key elements.
             * It is important to test both ways, so that both combinations are validated.
             */
            @Test
            void givenValidKeyAndCertificate() {

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
                        .body("testKey1", is(not(is(emptyString()))),
                            "testKey2", is(not(is(emptyString()))),
                            "testKey3", is(emptyOrNullString()),
                            "testKey4", is(emptyOrNullString()))
                        .statusCode(is(SC_OK));

                    given().config(user2)
                        .log().uri()
                        .contentType(JSON)
                        .when()
                        .get(CACHING_PATH)
                        .then().log().all()
                        .body("testKey3", is(not(is(emptyString()))),
                            "testKey4", is(not(is(emptyString()))),
                            "testKey1", is(emptyOrNullString()),
                            "testKey2", is(emptyOrNullString()))
                        .statusCode(is(SC_OK));
                } finally {
                    deleteValueUnderServiceIdWithoutValidation("testKey1", user1);
                    deleteValueUnderServiceIdWithoutValidation("testKey2", user1);
                    deleteValueUnderServiceIdWithoutValidation("testKey3", user2);
                    deleteValueUnderServiceIdWithoutValidation("testKey4", user2);
                }
            }

            @Test
            void givenValidKeyCertificateAndServiceHeader() {
                String serviceSpecificId1 = "service1";
                String serviceSpecificId2 = "service2";

                RestAssuredConfig user1 = SslContext.clientCertValid;
                RestAssuredConfig user2 = SslContext.clientCertUser;

                KeyValue keyValue1 = new KeyValue("testKey1", "testValue1");
                KeyValue keyValue2 = new KeyValue("testKey2", "testValue2");
                KeyValue keyValue3 = new KeyValue("testKey3", "testValue3");
                KeyValue keyValue4 = new KeyValue("testKey4", "testValue4");

                try {
                    loadValueUnderServiceId(keyValue1, user1, serviceSpecificId1);
                    loadValueUnderServiceId(keyValue2, user1, serviceSpecificId2);

                    loadValueUnderServiceId(keyValue3, user2, serviceSpecificId1);
                    loadValueUnderServiceId(keyValue4, user2, serviceSpecificId2);

                    given().config(user1)
                        .header(SPECIFIC_SERVICE_HEADER, serviceSpecificId1)
                        .log().uri()
                        .contentType(JSON)
                        .when()
                        .get(CACHING_PATH)
                        .then().log().all()
                        .body("testKey1", is(not(is(emptyString()))),
                            "testKey2", is(emptyOrNullString()),
                            "testKey3", is(emptyOrNullString()),
                            "testKey4", is(emptyOrNullString()))
                        .statusCode(is(SC_OK));

                    given().config(user1)
                        .header(SPECIFIC_SERVICE_HEADER, serviceSpecificId2)
                        .log().uri()
                        .contentType(JSON)
                        .when()
                        .get(CACHING_PATH)
                        .then().log().all()
                        .body("testKey2", is(not(is(emptyString()))),
                            "testKey1", is(emptyOrNullString()),
                            "testKey3", is(emptyOrNullString()),
                            "testKey4", is(emptyOrNullString()))
                        .statusCode(is(SC_OK));

                    given().config(user2)
                        .header(SPECIFIC_SERVICE_HEADER, serviceSpecificId1)
                        .log().uri()
                        .contentType(JSON)
                        .when()
                        .get(CACHING_PATH)
                        .then().log().all()
                        .body("testKey3", is(not(is(emptyString()))),
                            "testKey4", is(not(is(emptyString()))),
                            "testKey1", is(emptyOrNullString()),
                            "testKey2", is(emptyOrNullString()))
                        .statusCode(is(SC_OK));

                    given().config(user2)
                        .header(SPECIFIC_SERVICE_HEADER, serviceSpecificId2)
                        .log().uri()
                        .contentType(JSON)
                        .when()
                        .get(CACHING_PATH)
                        .then().log().all()
                        .body("testKey4", is(not(is(emptyString()))),
                            "testKey3", is(emptyOrNullString()),
                            "testKey1", is(emptyOrNullString()),
                            "testKey2", is(emptyOrNullString()))
                        .statusCode(is(SC_OK));
                } finally {
                    deleteValueUnderServiceIdWithoutValidation("testKey1", user1, serviceSpecificId1);
                    deleteValueUnderServiceIdWithoutValidation("testKey2", user1, serviceSpecificId2);
                    deleteValueUnderServiceIdWithoutValidation("testKey3", user2, serviceSpecificId1);
                    deleteValueUnderServiceIdWithoutValidation("testKey4", user2, serviceSpecificId2);
                }
            }
        }
    }

    @Nested
    class WhenDeletingAllForService {
        @Nested
        class VerifyAllEntriesAreDeleted {
            @Test
            void givenValidServiceParameter() {
                RestAssuredConfig clientCert = SslContext.clientCertValid;

                KeyValue keyValue1 = new KeyValue("testKey1", "testValue1");
                KeyValue keyValue2 = new KeyValue("testKey2", "testValue2");

                loadValueUnderServiceId(keyValue1, clientCert);
                loadValueUnderServiceId(keyValue2, clientCert);

                given().config(clientCert)
                    .log()
                    .uri()
                    .contentType(JSON)
                    .when()
                    .delete(CACHING_PATH)
                    .then()
                    .log().all()
                    .statusCode(is(SC_OK));

                given().config(clientCert)
                    .contentType(JSON)
                    .when()
                    .get(CACHING_PATH + "/testKey1")
                    .then()
                    .body(not(is(emptyString())))
                    .statusCode(is(SC_NOT_FOUND));
            }
        }
    }

    @Nested
    class WhenUpdatingValue {
        @Nested
        class ReturnUpdatedValue {
            @Test
            void givenValidKeyAndValues() {

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
        }
    }

    @Nested
    class WhenDeletingKey {
        @Nested
        class VerifyKeyIsDeleted {
            @Test
            void givenValidKey() {

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
        }

        @Nested
        class ReturnNotFound {
            @Test
            void givenInvalidKey() {
                given().config(SslContext.clientCertValid)
                    .contentType(JSON)
                    .when()
                    .delete(CACHING_PATH + "/invalidKey")
                    .then()
                    .statusCode(is(SC_NOT_FOUND));
            }
        }
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

    private static void loadValueUnderServiceId(KeyValue value, RestAssuredConfig config, String specificServiceId) {
        given().config(config)
            .header(SPECIFIC_SERVICE_HEADER, specificServiceId)
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

    private static void deleteValueUnderServiceIdWithoutValidation(String value, RestAssuredConfig config, String specificServiceId) {
        given().config(config)
            .header(SPECIFIC_SERVICE_HEADER, specificServiceId)
            .contentType(JSON)
            .when()
            .delete(CACHING_PATH + "/" + value);
    }
}
