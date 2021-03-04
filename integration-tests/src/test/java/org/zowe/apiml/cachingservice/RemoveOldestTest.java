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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.CachingRequests;
import org.zowe.apiml.util.categories.NotForMainframeTest;
import org.zowe.apiml.util.config.SslContext;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.hamcrest.core.Is.is;

@NotForMainframeTest
class RemoveOldestTest {
    private final CachingRequests requests = new CachingRequests();
    private static final CachingService service = new CachingService();

    @BeforeAll
    static void setup() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        SslContext.prepareSslAuthentication();
        service.start();
    }

    @AfterAll
    static void tearDown() {
        service.stop();
    }

    @Test
    void givenStorageIsFull_whenAnotherKeyIsInserted_thenTheOldestIsRemoved() {
        int amountOfAllowedRecords = 100 + 1;
        URI removeOldestCachingServiceUri = service.getBaseUrl();
        try {
            KeyValue keyValue;

            // The default configuration is to allow 100 records.
            for (int i = 0; i < amountOfAllowedRecords; i++) {
                keyValue = new KeyValue("key" + i, "testValue");
                requests.create(removeOldestCachingServiceUri, keyValue, SslContext.clientCertValid);
            }

            keyValue = new KeyValue("keyThatWillReplaceTheKey0", "testValue");
            given().config(SslContext.clientCertValid)
                .contentType(JSON)
                .body(keyValue)
                .when()
                .get(service.getBaseUrl() + "/key0")
                .then()
                .statusCode(is(SC_NOT_FOUND));
        } finally {
            for (int i = 0; i < amountOfAllowedRecords; i++) {
                requests.deleteValueUnderServiceIdWithoutValidation(removeOldestCachingServiceUri, "key" + i, SslContext.clientCertValid);
            }
        }
    }
}
