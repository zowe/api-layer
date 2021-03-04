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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.CachingRequests;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_INSUFFICIENT_STORAGE;
import static org.hamcrest.core.Is.is;

class RejectEvictionTest {
    private static final URI CACHING_PATH = HttpRequestUtils.getUriFromGateway("/cachingservice/api/v1/cache");
    private final CachingRequests requests = new CachingRequests();

    @BeforeAll
    static void setup() throws Exception {
        SslContext.prepareSslAuthentication();
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    void givenStorageIsFull_whenAnotherKeyIsInserted_thenItIsRejected() {
        int amountOfAllowedRecords = 100;
        try {
            KeyValue keyValue;

            // The default configuration is to allow 100 records.
            for (int i = 0; i < amountOfAllowedRecords; i++) {
                keyValue = new KeyValue("key" + i, "testValue");
                requests.create(keyValue, SslContext.clientCertValid);
            }

            keyValue = new KeyValue("keyThatWontPass", "testValue");
            given().config(SslContext.clientCertValid)
                .contentType(JSON)
                .body(keyValue)
                .when()
                .post(CACHING_PATH)
                .then()
                .statusCode(is(SC_INSUFFICIENT_STORAGE));
        } finally {
            for (int i = 0; i < amountOfAllowedRecords; i++) {
                requests.deleteValueUnderServiceIdWithoutValidation("key" + i, SslContext.clientCertValid);
            }
        }
    }
}
